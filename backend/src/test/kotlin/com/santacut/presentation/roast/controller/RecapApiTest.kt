package com.santacut.presentation.roast.controller

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
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.roast.service.RoastService
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.`is`
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
import java.util.UUID

/** GET /v1/me/recap. 문장은 대체 문장으로 저장한 결과라 구조와 서버 숫자만 본다. */
@IntegrationTest
class RecapApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val assessmentService: AssessmentService,
    private val roastService: RoastService,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val clock: Clock,
) {
    @Test
    fun `선물 결과는 순서가 고정된 9대16 카드 덱과 판정 숫자, 공유 문구를 돌려준다`() {
        val session = participantWithResult(GIFT_SCORES, withRoast = true)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skin").value("GIFT"))
            .andExpect(jsonPath("$.share_text").value("산타 공식 인증. 올해 선물 받음. 너는?"))
            .andExpect(jsonPath("$.cards[*].kind", contains("VERDICT", "ROAST", "ROAST", "ROAST", "LETTER", "STORY", "ARCHETYPE", "DIMENSIONS")))
            .andExpect(jsonPath("$.cards[*].index", contains(0, 1, 2, 3, 4, 5, 6, 7)))
            .andExpect(jsonPath("$.cards[*].width", everyItem(`is`(1080))))
            .andExpect(jsonPath("$.cards[*].height", everyItem(`is`(1920))))
            .andExpect(jsonPath("$.cards[0].cut").value(65))
            .andExpect(jsonPath("$.cards[0].passed").value(true))
            .andExpect(jsonPath("$.cards[0].stamp").value("선물 배송 확정"))
            .andExpect(jsonPath("$.cards[0].grade_code").value("PREMIUM"))
            .andExpect(jsonPath("$.cards[0].cut_distance").value(startsWith("+")))
            .andExpect(jsonPath("$.cards[0].title").isString)
            .andExpect(jsonPath("$.cards[0].verdict_mild").isString)
            .andExpect(jsonPath("$.cards[1].roast").isString)
            .andExpect(jsonPath("$.cards[1].flip").isString)
            .andExpect(jsonPath("$.cards[5].acts[*].months", contains("1~4월", "5~8월", "9~12월")))
            .andExpect(jsonPath("$.cards[7].bars.length()").value(4))
    }

    @Test
    fun `모든 카드에 응답과 같은 점수 표시가 있다`() {
        val session = participantWithResult(GIFT_SCORES, withRoast = true)

        val body = mockMvc.perform(get(RECAP_PATH).cookie(session)).andReturn().response.contentAsString
        val scoreText = Regex("\"score_text\":\"([^\"]+)\"").find(body)?.groupValues?.get(1)
        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(jsonPath("$.cards[*].score_text", everyItem(`is`(scoreText))))
    }

    @Test
    fun `석탄 결과는 COAL 스킨이고 도착 시각이 없으며 농담을 끈 도장은 선물은 내년에다`() {
        val session = participantWithResult(COAL_SCORES, withRoast = true)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skin").value("COAL"))
            .andExpect(jsonPath("$.share_text").value("나 올해 석탄이래. 사유 봐라."))
            .andExpect(jsonPath("$.cards[0].passed").value(false))
            .andExpect(jsonPath("$.cards[0].stamp").value("석탄"))
            .andExpect(jsonPath("$.cards[0].stamp_mild").value("선물은 내년에"))
            .andExpect(jsonPath("$.cards[0].delivery_eta").value(nullValue()))
    }

    @Test
    fun `이 기능 전 결과라 팩폭 문장이 없으면 404 RECAP_NOT_FOUND다`() {
        val session = participantWithResult(GIFT_SCORES, withRoast = false)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RECAP_NOT_FOUND"))
    }

    @Test
    fun `결과 응답의 recap_available은 최신 결과에 팩폭 문장이 있을 때만 true다`() {
        val withRoast = participantWithResult(GIFT_SCORES, withRoast = true)
        val withoutRoast = participantWithResult(GIFT_SCORES, withRoast = false)

        mockMvc.perform(get(RESULT_PATH).cookie(withRoast)).andExpect(status().isOk).andExpect(jsonPath("$.recap_available").value(true))
        mockMvc.perform(get(RESULT_PATH).cookie(withoutRoast)).andExpect(status().isOk).andExpect(jsonPath("$.recap_available").value(false))
    }

    @Test
    fun `점수가 나온 결과가 아직 없으면 404 RESULT_NOT_FOUND다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))

        mockMvc.perform(get(RECAP_PATH).cookie(Cookie(SESSION_COOKIE, participant.sessionToken)))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("RESULT_NOT_FOUND"))
    }

    @Test
    fun `세션 없이 부르면 401이다`() {
        mockMvc.perform(get(RECAP_PATH)).andExpect(status().isUnauthorized)
    }

    /** 항목 점수 [scores]로 평가와 순위를 저장한 참가자의 세션. [withRoast]면 그 평가의 팩폭 결과(대체 문장)도 저장한다. */
    private fun participantWithResult(scores: List<Int>, withRoast: Boolean): Cookie {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val now = clock.instant()
        val job = AnalysisJob.enqueue(participant.id, IdempotencyKey.from("key-" + UUID.randomUUID()), EMPTY_SUBMISSION, AnalysisPayload("{}"), now)
        val score = Score.shrunk(scores.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        val dossier = TestDossiers.draft(TestDossiers.selves(DOSSIER_TEXTS))
        transactionTemplate.executeWithoutResult {
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            if (withRoast) roastService.record(assessment, roastService.fallbackDraft(score.hundredths, dossier, 0), now)
            rankingService.rebuild(now)
        }
        return Cookie(SESSION_COOKIE, participant.sessionToken)
    }

    companion object {
        private const val RECAP_PATH = "/v1/me/recap"
        private const val RESULT_PATH = "/v1/me/result"
        private const val SESSION_COOKIE = "sc_session"
        private val GIFT_SCORES = listOf(99, 99, 99, 99)
        private val COAL_SCORES = listOf(5, 5, 5, 5)
        private val EMPTY_SUBMISSION = AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList()))
        private val DOSSIER_TEXTS = List(40) { "진짜 ㅋㅋㅋ 고마워" }
    }
}
