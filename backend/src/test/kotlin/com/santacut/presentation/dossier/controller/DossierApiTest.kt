package com.santacut.presentation.dossier.controller

import com.santacut.IntegrationTest
import com.santacut.TestDossiers
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
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
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.ranking.service.RankingService
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.hasItems
import org.hamcrest.Matchers.nullValue
import org.hamcrest.Matchers.startsWith
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Clock
import java.time.Duration
import java.util.UUID

/** GET /v1/me/dossier와 결과 응답의 dossier_available. 문구는 테스트 픽스처 리소스의 것이라 구조만 본다. */
@IntegrationTest
class DossierApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val assessmentService: AssessmentService,
    private val dossierService: DossierService,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    @Test
    fun `조서가 있는 결과는 칭호, 톤, 순서대로 놓은 장면을 돌려주고 결과 응답에 dossier_available이 true다`() {
        val session = participantWithResult(withDossier = true)

        mockMvc.perform(get(DOSSIER_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").isString)
            .andExpect(jsonPath("$.tone").value("normal"))
            .andExpect(jsonPath("$.scenes[0].id").value("teaser"))
            .andExpect(jsonPath("$.scenes[0].shareable").value(false))
            .andExpect(jsonPath("$.scenes[*].kind", hasItems("hook", "extreme", "verdict", "summons")))
            .andExpect(jsonPath("$.scenes[1].lines").isArray)
            .andExpect(jsonPath("$.scenes[?(@.id == 'verdict')].big_value", everyItem(startsWith("전국 "))))
        mockMvc.perform(get(RESULT_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.dossier_available").value(true))
    }

    @Test
    fun `이 기능 전 결과라 조서가 없으면 404 DOSSIER_NOT_FOUND이고 결과 응답의 dossier_available은 false다`() {
        val session = participantWithResult(withDossier = false)

        mockMvc.perform(get(DOSSIER_PATH).cookie(session))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("DOSSIER_NOT_FOUND"))
        mockMvc.perform(get(RESULT_PATH).cookie(session))
            .andExpect(jsonPath("$.dossier_available").value(false))
    }

    @Test
    fun `예전 집계 규칙 판으로 센 조서는 조회하지 않고 404 DOSSIER_NOT_FOUND이며 dossier_available은 false다`() {
        val session = participantWithResult(withDossier = true, statsVersion = DossierStats.VERSION - 1)

        mockMvc.perform(get(DOSSIER_PATH).cookie(session))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("DOSSIER_NOT_FOUND"))
        mockMvc.perform(get(RESULT_PATH).cookie(session))
            .andExpect(jsonPath("$.dossier_available").value(false))
    }

    @Test
    fun `재업로드 뒤 순위 다시 계산 전이어도 최신 평가의 조서를 순위 없이 돌려준다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        recordResult(participant.id, withDossier = false, statsVersion = DossierStats.VERSION, rebuild = true)
        recordResult(participant.id, withDossier = true, statsVersion = DossierStats.VERSION, rebuild = false, later = LATER)
        val session = Cookie(SESSION_COOKIE, participant.sessionToken)

        mockMvc.perform(get(RESULT_PATH).cookie(session))
            .andExpect(jsonPath("$.ranking_pending").value(true))
            .andExpect(jsonPath("$.dossier_available").value(true))
        mockMvc.perform(get(DOSSIER_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.scenes[?(@.id == 'verdict')].big_value", everyItem(nullValue())))
    }

    @Test
    fun `점수가 나온 결과가 아직 없으면 결과 API처럼 404 RESULT_NOT_FOUND다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))

        mockMvc.perform(get(DOSSIER_PATH).cookie(Cookie(SESSION_COOKIE, participant.sessionToken)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESULT_NOT_FOUND"))
    }

    @Test
    fun `세션 없이 부르면 401이다`() {
        mockMvc.perform(get(DOSSIER_PATH)).andExpect(status().isUnauthorized)
    }

    /** 평가와 순위가 있는 참가자의 세션. [withDossier]면 그 평가의 조서도 [statsVersion] 판으로 저장한다. */
    private fun participantWithResult(withDossier: Boolean, statsVersion: Int = DossierStats.VERSION): Cookie {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        recordResult(participant.id, withDossier, statsVersion, rebuild = true)
        return Cookie(SESSION_COOKIE, participant.sessionToken)
    }

    /** 참가자 [participantId]의 새 평가를 지금보다 [later] 뒤 시각으로 저장한다. [rebuild]면 순위도 다시 계산한다. */
    private fun recordResult(participantId: UUID, withDossier: Boolean, statsVersion: Int, rebuild: Boolean, later: Duration = Duration.ZERO) {
        val now = clock.instant().plus(later)
        val job = AnalysisJob.enqueue(participantId, IdempotencyKey.from("key-" + UUID.randomUUID()), EMPTY_SUBMISSION, AnalysisPayload("{}"), now)
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        val draft = TestDossiers.draft(TestDossiers.selves(DOSSIER_TEXTS)).copy(statsVersion = statsVersion)
        transactionTemplate.executeWithoutResult {
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            if (withDossier) dossierService.record(assessment, draft, now)
            if (rebuild) rankingService.rebuild(now)
        }
    }

    companion object {
        private val LATER: Duration = Duration.ofSeconds(1)
        private const val DOSSIER_PATH = "/v1/me/dossier"
        private const val RESULT_PATH = "/v1/me/result"
        private const val SESSION_COOKIE = "sc_session"
        private val SCORES = listOf(99, 99, 99, 99)
        private val EMPTY_SUBMISSION = AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList()))
        private val DOSSIER_TEXTS = List(40) { "진짜 ㅋㅋㅋ 고마워" }
    }
}
