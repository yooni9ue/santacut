package com.santacut.presentation.global

import com.santacut.TestDossiers
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
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.roast.service.RoastService
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
    private val dossierService: DossierService,
    private val roastService: RoastService,
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

        assertEquals(EXPECTED_READ_COUNTS, readQueries(jobId, session, cursor, token))
    }

    private fun readQueries(jobId: String, session: Cookie, cursor: String, token: String): Map<String, Long> = mapOf(
        "status" to countQueries { mockMvc.perform(get("/v1/analyses/$jobId").cookie(session)).andExpect(status().isOk) },
        "rankings" to countQueries { mockMvc.perform(get("/v1/rankings").param("cursor", cursor)).andExpect(status().isOk) },
        "result" to countQueries { mockMvc.perform(get("/v1/me/result").cookie(session)).andExpect(status().isOk) },
        "share page" to countQueries { mockMvc.perform(get("/s/$token")).andExpect(status().isOk) },
        "dossier" to countQueries { mockMvc.perform(get("/v1/me/dossier").cookie(session)).andExpect(status().isOk) },
        "recap" to countQueries { mockMvc.perform(get("/v1/me/recap").cookie(session)).andExpect(status().isOk) },
    )

    @Test
    fun `순위 계산 뒤 진행 상황 알림의 SQL 수는 작업 수와 상관없다`() {
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
        val job = AnalysisJob.enqueue(participantId, IdempotencyKey.from("key-" + UUID.randomUUID()), EMPTY_SUBMISSION, AnalysisPayload("{}"), now)
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        transactionTemplate.executeWithoutResult {
            job.startAttempt(LEASE, now, LEASE_DURATION)
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            dossierService.record(assessment, TestDossiers.draft(TestDossiers.selves(listOf("고마워", "진짜"))), now)
            roastService.record(assessment, roastService.fallbackDraft(score.hundredths, score.components, null, 0), now)
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
        private val EMPTY_SUBMISSION = AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList()))
        /**
         * 결과 조회는 조서가 있는지(dossier_available)와 리캡 카드가 있는지(recap_available)를 한 번씩 더 읽는다.
         * 조서 조회는 세션, 최신 평가, 최신 순위 스냅숏, 조서, 지역 이름을 한 번씩 읽는다.
         * 리캡 조회는 세션, 최신 평가, 최신 순위 스냅숏, 팩폭 결과, 순위 계산 참가자 수, 저장한 유형 분포를 한 번씩 읽는다.
         */
        private val EXPECTED_READ_COUNTS = mapOf(
            "status" to 3L, "rankings" to 3L, "result" to 7L, "share page" to 1L, "dossier" to 5L, "recap" to 6L,
        )

        /** 알릴 작업 id 목록, 작업들, 참가자별 순위 반영 시각을 한 번씩 읽는다. 작업 수와 상관없다(예전에는 작업 하나에 3번 더). */
        private const val EXPECTED_ANNOUNCE_ONE = 3L
        private const val EXPECTED_ANNOUNCE_MANY = 3L
    }
}
