package com.santacut.infrastructure.analysis.repository

import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisUpload
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.RawChatFile
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 원본 파일을 SQLite BLOB 컬럼에 그대로 쓰고 읽는지, 원본을 읽지 않는 삭제 쿼리가 도는지 확인한다.
 */
@IntegrationTest
class AnalysisUploadRepositoryTest @Autowired constructor(
    private val participantFacade: ParticipantFacade,
    private val analysisJobRepository: AnalysisJobRepository,
    private val analysisUploadRepository: AnalysisUploadRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    @Test
    fun `원본 바이트를 그대로 저장하고 읽는다`() {
        val content = ByteArray(CONTENT_SIZE) { index -> (index % BYTE_RANGE).toByte() }
        val upload = saveUpload(content, CREATED_AT)

        val found = analysisUploadRepository.findById(upload.jobId).orElseThrow()

        assertContentEquals(content, found.content)
        assertEquals(SELF_SPEAKER, found.selfSpeaker)
        assertEquals(DataSource.KAKAO, found.source)
    }

    @Test
    fun `작업 ID나 받은 시각 기준으로 원본을 지우고 작업은 남긴다`() {
        val byJob = saveUpload("대화".toByteArray(), CREATED_AT)
        val old = saveUpload("대화".toByteArray(), CREATED_AT.minus(Duration.ofDays(2)))

        transactionTemplate.executeWithoutResult { analysisUploadRepository.deleteByJobId(byJob.jobId) }
        transactionTemplate.executeWithoutResult {
            analysisUploadRepository.deleteByCreatedAtBefore(CREATED_AT.minus(Duration.ofDays(1)))
        }

        assertFalse(analysisUploadRepository.existsById(byJob.jobId))
        assertFalse(analysisUploadRepository.existsById(old.jobId))
        assertTrue(analysisJobRepository.existsById(old.jobId))
    }

    private fun saveUpload(content: ByteArray, createdAt: Instant): AnalysisUpload {
        val participantId = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id
        val file = RawChatFile(DataSource.KAKAO, content)
        val key = IdempotencyKey.from("key-" + UUID.randomUUID())
        val job = analysisJobRepository.save(
            AnalysisJob.receive(participantId, key, file.source, file.requestHash(SELF_SPEAKER), createdAt),
        )
        return analysisUploadRepository.save(AnalysisUpload.of(job, SELF_SPEAKER, file, createdAt))
    }

    companion object {
        private const val SELF_SPEAKER = "산타"
        private const val CONTENT_SIZE = 70_000
        private const val BYTE_RANGE = 256
        private val CREATED_AT: Instant = Instant.parse("2099-11-01T00:00:00Z")
    }
}
