package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.AnalysisUpload
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.InstagramThread
import com.santacut.domain.analysis.model.RawChatFile
import com.santacut.domain.analysis.model.UploadConcurrencyLimit
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.global.config.UploadIntakeProperties
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.analysis.repository.AnalysisUploadRepository
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * 검증을 기다리는 원본 파일을 저장하고, 참여자 이름을 화자 표시로 바꾼 분석 제출 내용으로 정규화한다.
 * 원본과 본인 이름은 검증이 끝나면 지운다.
 */
@Service
class AnalysisUploadService(
    private val analysisUploadRepository: AnalysisUploadRepository,
    private val objectMapper: ObjectMapper,
    uploadIntakeProperties: UploadIntakeProperties,
) {
    private val concurrencyLimit = UploadConcurrencyLimit(
        uploadIntakeProperties.maxConcurrentUploads, uploadIntakeProperties.retryAfter,
    )

    /** 원본을 메모리로 읽어 저장하는 [action]을 동시 처리 상한 안에서 한다. 자리가 없으면 UPLOAD_BUSY(503)다. */
    fun <T> withinConcurrencyLimit(now: Instant, action: () -> T): T = concurrencyLimit.runWithinLimit(now, action)

    fun store(job: AnalysisJob, selfSpeaker: String, file: RawChatFile, now: Instant) {
        analysisUploadRepository.save(AnalysisUpload.of(job, selfSpeaker, file, now))
    }

    /** 가장 먼저 받은 원본. */
    fun findNext(): AnalysisUpload? = analysisUploadRepository.findFirstByOrderByCreatedAtAsc()

    fun delete(jobId: String) {
        analysisUploadRepository.deleteByJobId(jobId)
    }

    /** 검증 대기 수명([AnalysisJob.QUEUE_EXPIRY])을 넘긴 원본을 지운다. */
    fun deleteOverdue(now: Instant) {
        analysisUploadRepository.deleteByCreatedAtBefore(AnalysisJob.queueExpiryCutoff(now))
    }

    /**
     * 원본을 화자 표시만 남긴 제출 내용으로 바꾼다. 출처별 파싱과 정규화 순서는 [AnalysisUpload.normalize]가 정한다.
     * 분석 기간 이벤트 전체로 조서 집계를 세는 일은 [tallyDossier]에 맡긴다.
     */
    fun normalize(
        upload: AnalysisUpload,
        now: Instant,
        tallyDossier: (DataSource, AnalysisEvents) -> DossierTally?,
    ): AnalysisSubmission = upload.normalize(now, ::readInstagramThread, tallyDossier)

    /** 올바른 JSON이 아니거나, 본문이 `null`이거나, 대화방 객체 모양이 아니면 [ErrorCode.UNSUPPORTED_FILE_FORMAT]이다. */
    private fun readInstagramThread(text: String): InstagramThread {
        val thread: InstagramThread? = try {
            objectMapper.readValue(text, InstagramThread::class.java)
        } catch (exception: JacksonException) {
            null
        }
        return thread ?: throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
    }
}
