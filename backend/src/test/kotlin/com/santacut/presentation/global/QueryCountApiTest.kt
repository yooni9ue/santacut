package com.santacut.presentation.global

import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.share.command.CreateShareCommand
import com.santacut.application.share.facade.ShareFacade
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.LowConfidenceDimensions
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.ranking.model.RankingCursor
import com.santacut.domain.ranking.service.RankingService
import com.santacut.infrastructure.analysis.repository.AnalysisJobRepository
import jakarta.persistence.EntityManagerFactory
import jakarta.servlet.http.Cookie
import org.hibernate.SessionFactory
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.util.UUID
import kotlin.test.assertEquals

/**
 * 자주 부르는 조회와 순위 계산 뒤 진행 상황 알림이 SQL을 몇 번 준비하는지 센다(Hibernate 통계의 prepareStatementCount).
 * 참가자나 작업 수에 따라 조회 수가 늘면(N+1) 이 테스트가 깨진다. 다른 테스트의 행이 섞이지 않게
 * `build/query-count-test.db`를 따로 쓰고 테스트마다 참가자를 모두 지운다(외래 키 삭제 규칙이 나머지 행을 지운다).
 */
@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:sqlite:./build/query-count-test.db",
        "app.season-year=2099",
        "santacut.ai.poll-delay-ms=3600000",
        "santacut.upload.poll-delay-ms=3600000",
        "santacut.scheduling.enabled=false",
    ],
)
@AutoConfigureMockMvc
class QueryCountApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val shareFacade: ShareFacade,
    private val assessmentService: AssessmentService,
    private val analysisJobService: AnalysisJobService,
    private val analysisJobRepository: AnalysisJobRepository,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
    entityManagerFactory: EntityManagerFactory,
) {
    private val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics

    @BeforeEach
    fun clearParticipants() {
        jdbcTemplate.update("DELETE FROM participants")
    }

    @Test
    fun `자주 부르는 조회는 정해진 수의 SQL만 준비한다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val session = Cookie(SESSION_COOKIE, participant.sessionToken)
        val jobId = succeededJob(participant.id)
        val computedAt = clock.instant()
        transactionTemplate.executeWithoutResult { rankingService.rebuild(computedAt) }
        val token = shareFacade.createShare(CreateShareCommand(participant.id)).token
        val cursor = RankingCursor.after(1, computedAt).value()

        val counts = mapOf(
            "status" to countQueries { mockMvc.perform(get("/v1/analyses/$jobId").cookie(session)).andExpect(status().isOk) },
            "rankings" to countQueries { mockMvc.perform(get("/v1/rankings").param("cursor", cursor)).andExpect(status().isOk) },
            "result" to countQueries { mockMvc.perform(get("/v1/me/result").cookie(session)).andExpect(status().isOk) },
            "share page" to countQueries { mockMvc.perform(get("/s/$token")).andExpect(status().isOk) },
        )

        assertEquals(EXPECTED_READ_COUNTS, counts)
    }

    @Test
    fun `순위 계산 뒤 진행 상황 알림의 SQL 수`() {
        assertEquals(EXPECTED_ANNOUNCE_ONE, announcementQueries(1))
        assertEquals(EXPECTED_ANNOUNCE_MANY, announcementQueries(MANY_JOBS))
    }

    /** 성공한 작업 [jobs]개를 만들고 순위를 계산한 뒤, 그 작업들을 다시 알리는 데 준비한 SQL 수. 알림은 커밋 뒤에 돈다. */
    private fun announcementQueries(jobs: Int): Long {
        clearParticipants()
        repeat(jobs) { succeededJob(participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE)).id) }
        transactionTemplate.executeWithoutResult { rankingService.rebuild(clock.instant()) }
        return countQueries { transactionTemplate.executeWithoutResult { analysisJobService.announceRankedSince(null) } }
    }

    /** 평가까지 저장하고 성공으로 끝낸 작업. */
    private fun succeededJob(participantId: UUID): String {
        val now = clock.instant()
        val job = AnalysisJob.enqueue(
            participantId,
            IdempotencyKey.from("key-" + UUID.randomUUID()),
            AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
            AnalysisPayload("{}"),
            now,
        )
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        transactionTemplate.executeWithoutResult {
            job.startAttempt(LEASE, now, LEASE_DURATION)
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            job.succeed(assessment.id, now)
            analysisJobRepository.save(job)
        }
        return job.id
    }

    private fun countQueries(action: () -> Unit): Long {
        statistics.clear()
        action()
        return statistics.prepareStatementCount
    }

    companion object {
        private const val SESSION_COOKIE = "sc_session"
        private const val LEASE = "lease"
        private val LEASE_DURATION: Duration = Duration.ofMinutes(5)
        private val SCORES = listOf(60, 60, 60, 60)
        private const val MANY_JOBS = 20
        private val EXPECTED_READ_COUNTS = mapOf("status" to 4L, "rankings" to 3L, "result" to 5L, "share page" to 1L)

        /** 지금은 작업마다 작업, 최신 스냅숏, 평가를 따로 읽는다(작업 하나에 3번 + 목록 1번). */
        private const val EXPECTED_ANNOUNCE_ONE = 4L
        private const val EXPECTED_ANNOUNCE_MANY = 61L
    }
}
