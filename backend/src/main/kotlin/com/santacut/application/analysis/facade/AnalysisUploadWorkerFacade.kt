package com.santacut.application.analysis.facade

import com.santacut.application.global.Facade
import com.santacut.domain.analysis.model.AnalysisFailureCode
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.AnalysisUpload
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AnalysisUploadService
import com.santacut.domain.global.exception.SantaCutException
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock

/**
 * 원본 파일로 받은 작업을 하나씩 꺼내 검증·정규화하고 점수 산정 대기열에 넣는 백그라운드 작업자.
 *
 * 최대 20MB 파일을 읽고 파싱하는 동안 트랜잭션을 열어 두면 하나뿐인 DB 커넥션(Hikari 풀 1개)과
 * SQLite 쓰기 잠금을 쥐게 된다. 그래서 [TransactionTemplate]으로 원본 꺼내기와 결과 기록 트랜잭션을 나누고,
 * 파싱은 트랜잭션 밖에서 한다. 로그에는 작업 ID와 오류 코드만 남긴다.
 */
@Facade
class AnalysisUploadWorkerFacade(
    private val analysisJobService: AnalysisJobService,
    private val analysisUploadService: AnalysisUploadService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    private val logger = LoggerFactory.getLogger(AnalysisUploadWorkerFacade::class.java)

    @Scheduled(fixedDelayString = "\${santacut.upload.poll-delay-ms:${DEFAULT_POLL_DELAY_MS}}")
    fun validateNextUpload() {
        transactionTemplate.executeWithoutResult { cleanUpOverdue() }
        val upload = transactionTemplate.execute { claimNextUpload() } ?: return
        try {
            val submission = analysisUploadService.normalize(upload, clock.instant())
            transactionTemplate.executeWithoutResult { queue(upload.jobId, submission) }
        } catch (exception: Exception) {
            val code = AnalysisFailureCode.fromValidationFailure(exception)
            logger.warn("원본 파일 검증 {} 실패: {}", upload.jobId, failureReason(exception))
            transactionTemplate.executeWithoutResult { reject(upload.jobId, code) }
        }
    }

    /** 24시간 넘게 검증을 기다린 원본을 지우고 그 작업을 만료한다. */
    private fun cleanUpOverdue() {
        val now = clock.instant()
        analysisUploadService.deleteOverdue(now)
        analysisJobService.expireOverdueJobs(now)
    }

    /** 가장 먼저 받은 원본의 검증을 시작한다. 작업이 이미 끝났거나 시도를 다 썼으면 원본만 지운다. */
    private fun claimNextUpload(): AnalysisUpload? {
        val upload = analysisUploadService.findNext() ?: return null
        if (analysisJobService.startValidation(upload.jobId, clock.instant()) != null) return upload
        analysisUploadService.delete(upload.jobId)
        return null
    }

    private fun queue(jobId: String, submission: AnalysisSubmission) {
        analysisJobService.queueValidated(jobId, submission)
        analysisUploadService.delete(jobId)
    }

    private fun reject(jobId: String, code: AnalysisFailureCode) {
        analysisJobService.rejectUpload(jobId, code, clock.instant())
        analysisUploadService.delete(jobId)
    }

    /** 비즈니스 오류는 오류 코드만, 그 밖의 예외는 클래스 이름만 남긴다. 파일 내용이나 참여자 이름은 남기지 않는다. */
    private fun failureReason(exception: Exception): String =
        if (exception is SantaCutException) exception.errorCode.name else exception.javaClass.simpleName

    companion object {
        /** `santacut.upload.poll-delay-ms`가 없을 때 다음 원본을 찾기 전 쉬는 시간(밀리초). */
        private const val DEFAULT_POLL_DELAY_MS = 1000L
    }
}
