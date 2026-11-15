package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.AnalysisUpload
import com.santacut.domain.analysis.model.ChatMessages
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.InstagramThread
import com.santacut.domain.analysis.model.KakaoTalkLines
import com.santacut.domain.analysis.model.RawChatFile
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.analysis.repository.AnalysisUploadRepository
import org.springframework.stereotype.Service
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import java.time.Instant

/**
 * 검증을 기다리는 원본 파일을 저장하고, 본인 메시지만 분석 제출 내용으로 정규화한다.
 * 원본과 본인 이름은 검증이 끝나면 지운다.
 */
@Service
class AnalysisUploadService(
    private val analysisUploadRepository: AnalysisUploadRepository,
    private val objectMapper: ObjectMapper,
) {
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
     * 원본을 읽어 본인 메시지만 이벤트로 바꾸고 제출 한도에 맞춘다.
     * 이벤트가 20개보다 적으면 [ErrorCode.INSUFFICIENT_EVENTS]다.
     */
    fun normalize(upload: AnalysisUpload, now: Instant): AnalysisSubmission {
        val events = readMessages(upload.toRawFile()).eventsWrittenBy(upload.selfSpeaker).recentAt(now)
        return AnalysisSubmission(upload.source, events).also { it.verifyAcceptableAt(now) }
    }

    private fun readMessages(file: RawChatFile): ChatMessages = when (file.source) {
        DataSource.KAKAO -> KakaoTalkLines.from(file.decodeText()).toMessages()
        DataSource.INSTAGRAM -> readInstagramThread(file.decodeText()).toMessages()
    }

    private fun readInstagramThread(text: String): InstagramThread =
        try {
            objectMapper.readValue(text, InstagramThread::class.java)
        } catch (exception: JacksonException) {
            throw SantaCutException(ErrorCode.UNSUPPORTED_FILE_FORMAT)
        }
}
