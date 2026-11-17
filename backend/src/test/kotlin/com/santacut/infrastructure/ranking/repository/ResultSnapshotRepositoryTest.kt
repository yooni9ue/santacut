package com.santacut.infrastructure.ranking.repository

import com.santacut.IntegrationTest
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.share.model.Share
import com.santacut.infrastructure.share.repository.ShareRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 순위 스냅숏은 새 행으로 바로 저장되고, 정리는 공유 링크가 붙잡은 스냅숏만 남기는지 확인한다.
 */
@IntegrationTest
class ResultSnapshotRepositoryTest @Autowired constructor(
    private val resultSnapshotRepository: ResultSnapshotRepository,
    private val shareRepository: ShareRepository,
    private val transactionTemplate: TransactionTemplate,
    private val participantFacade: ParticipantFacade,
    private val jdbcTemplate: JdbcTemplate,
) {
    private val participantId: String by lazy {
        participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id.toString()
    }
    private val assessmentId: String by lazy { insertAssessment(participantId) }

    @Test
    fun `새로 만든 스냅숏은 새 행이고 저장하거나 읽으면 새 행이 아니다`() {
        val snapshot = snapshot()
        assertTrue(snapshot.isNew)

        transactionTemplate.executeWithoutResult { resultSnapshotRepository.saveAll(listOf(snapshot)) }

        assertFalse(snapshot.isNew)
        assertFalse(resultSnapshotRepository.findById(snapshot.id).orElseThrow().isNew)
    }

    @Test
    fun `정리는 공유 링크가 붙잡은 스냅숏만 남긴다`() {
        val shared = snapshot()
        val stale = snapshot()
        transactionTemplate.executeWithoutResult {
            resultSnapshotRepository.saveAll(listOf(shared, stale))
            shareRepository.save(share(shared.id))
        }

        transactionTemplate.executeWithoutResult { resultSnapshotRepository.deleteUnshared() }

        assertTrue(resultSnapshotRepository.existsById(shared.id))
        assertFalse(resultSnapshotRepository.existsById(stale.id))
        assertEquals(0, transactionTemplate.execute { resultSnapshotRepository.deleteUnshared() })
    }

    private fun snapshot(): ResultSnapshot =
        ResultSnapshot(UUID.randomUUID().toString(), participantId, assessmentId, 1, 1, ETA, COMPUTED_AT)

    private fun share(snapshotId: String): Share =
        Share(UUID.randomUUID().toString(), participantId, snapshotId, "산타", 8000, 1, ETA, "통과", "한마디", COMPUTED_AT)

    /** 스냅숏의 외래 키를 채울 평가 한 행. 점수 흐름은 이 테스트의 관심이 아니라 행만 넣는다. */
    private fun insertAssessment(userId: String): String {
        val id = UUID.randomUUID().toString()
        jdbcTemplate.update(INSERT_ASSESSMENT, id, userId)
        return id
    }

    companion object {
        private const val INSERT_ASSESSMENT = "INSERT INTO assessments (id, user_id, source, score_hundredths, verdict, comment, components) " +
            "VALUES (?, ?, 'KAKAO', 8000, '통과', '한마디', '[80,80,80,80]')"
        private val ETA: Instant = Instant.parse("2026-12-25T00:03:00Z")
        private val COMPUTED_AT: Instant = Instant.parse("2026-12-01T00:00:00Z")
    }
}
