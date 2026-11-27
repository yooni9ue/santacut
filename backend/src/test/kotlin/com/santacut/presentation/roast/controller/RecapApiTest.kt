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
import com.santacut.domain.roast.model.RoastDraft
import com.santacut.domain.roast.service.RoastService
import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.contains
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.everyItem
import org.hamcrest.Matchers.greaterThanOrEqualTo
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.lessThanOrEqualTo
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.notNullValue
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
    fun `선물 결과는 판정, 컷 표, 이유, 깎인 말, 결론 순서의 9대16 카드 덱과 서버 숫자를 돌려준다`() {
        val session = participantWithResult(GIFT_SCORES, withRoast = true)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skin").value("GIFT"))
            .andExpect(jsonPath("$.share_text").value(allOf(startsWith("산타가 내 단톡방 다 읽고도 "), endsWith("점 줌. 이게 인성이다. 너도 까봐."))))
            .andExpect(jsonPath("$.share_text_mild").doesNotExist())
            .andExpect(jsonPath("$.cards[0].kind").value("VERDICT"))
            .andExpect(jsonPath("$.cards[1].kind").value("CUTLINE"))
            .andExpect(jsonPath("$.cards[2].kind").value("REASON"))
            .andExpect(jsonPath("$.cards[-1:].kind", contains("CONCLUSION")))
            .andExpect(jsonPath("$.cards.length()").value(allOf(greaterThanOrEqualTo(MIN_CARDS), lessThanOrEqualTo(MAX_CARDS))))
            .andExpect(jsonPath("$.cards[*].kind", everyItem(not(`is`("KEEP")))))
            .andExpect(jsonPath("$.cards[*].width", everyItem(`is`(1080))))
            .andExpect(jsonPath("$.cards[*].height", everyItem(`is`(1920))))
            .andExpect(jsonPath("$.cards[0].verdict").value("착한 아이"))
            .andExpect(jsonPath("$.cards[0].cut").value(65))
            .andExpect(jsonPath("$.cards[0].grade").value("굴뚝 프리패스"))
            .andExpect(jsonPath("$.cards[0].cut_distance").value(startsWith("+")))
            .andExpect(jsonPath("$.cards[0].verdict_line").isString)
            .andExpect(jsonPath("$.cards[0].verdict_mild").doesNotExist())
            .andExpect(jsonPath("$.cards[0].stamp_mild").doesNotExist())
            .andExpect(jsonPath("$.cards[1].band_detail").value("산타가 문 열어둠"))
            .andExpect(jsonPath("$.cards[1].bands.length()").value(6))
            .andExpect(jsonPath("$.cards[1].band_position").value(0))
            .andExpect(jsonPath("$.cards[2].chip_text").isString)
            .andExpect(jsonPath("$.cards[2].label").value(endsWith("상")))
            .andExpect(jsonPath("$.cards[-1:].friend_call", contains("우리 방에서 나보다 착한 애 나와")))
            .andExpect(jsonPath("$.cards[-1:].band_tag", contains("굴뚝프리패스")))
    }

    @Test
    fun `순위 계산이 끝나면 참가자가 적어도 모든 카드에 순위와 상위 비율이 있다`() {
        val session = participantWithResult(GIFT_SCORES, withRoast = true)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.ranking_pending").value(false))
            .andExpect(jsonPath("$.rank").isNumber)
            .andExpect(jsonPath("$.top_percent").isNumber)
            .andExpect(jsonPath("$.cards[*].rank", everyItem(notNullValue())))
            .andExpect(jsonPath("$.cards[*].top_percent", everyItem(notNullValue())))
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
    fun `석탄 결과는 판정, 컷 표, 깎인 말, 살아남은 말 하나, 결론 순서이고 도착 시각이 없다`() {
        val session = participantWithResult(COAL_SCORES, withRoast = true)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.skin").value("COAL"))
            .andExpect(jsonPath("$.share_text").value("산타가 내 단톡방 읽고 석탄 보냄. 반품도 안 된대. 혼자 받긴 억울하다. 너도 까봐."))
            .andExpect(jsonPath("$.cards[0].verdict").value("나쁜 아이"))
            .andExpect(jsonPath("$.cards[0].passed").value(false))
            .andExpect(jsonPath("$.cards[0].grade").value("석탄 당첨"))
            .andExpect(jsonPath("$.cards[0].delivery_eta").value(nullValue()))
            .andExpect(jsonPath("$.cards[1].kind").value("CUTLINE"))
            .andExpect(jsonPath("$.cards[2].kind").value("CUT"))
            .andExpect(jsonPath("$.cards[-2:].kind", contains("KEEP", "CONCLUSION")))
            .andExpect(jsonPath("$.cards[*].kind", everyItem(not(`is`("REASON")))))
            .andExpect(jsonPath("$.cards[-1:].friend_call", contains("우리 방 석탄 동기 구함")))
    }

    @Test
    fun `작가 프롬프트 4판으로 저장한 옛 결과도 500 없이 통계 카드와 대체 결론으로 돌려준다`() {
        val session = participantWithResult(GIFT_SCORES, withRoast = false, legacyJson = LEGACY_V4_JSON)

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cards[0].verdict_line").value("옛 판결 한 줄이다. 선물 명단에 올린다."))
            .andExpect(jsonPath("$.cards[2].kind").value("REASON"))
            .andExpect(jsonPath("$.cards[2].chip_text").value("ㅋ 한 번에 최대 12개"))
            .andExpect(jsonPath("$.cards[2].quote").value(nullValue()))
            .andExpect(jsonPath("$.cards[-1:].kind", contains("CONCLUSION")))
            .andExpect(jsonPath("$.cards.length()").value(allOf(greaterThanOrEqualTo(MIN_CARDS), lessThanOrEqualTo(MAX_CARDS))))
    }

    @Test
    fun `저장한 팩폭 결과를 읽을 수 없어도 500 없이 구간별 대체 내용으로 돌려준다`() {
        val session = participantWithResult(COAL_SCORES, withRoast = false, legacyJson = "not json")

        mockMvc.perform(get(RECAP_PATH).cookie(session))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.cards.length()").value(MIN_CARDS))
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

    /**
     * 항목 점수 [scores]로 평가와 순위를 저장한 참가자의 세션. [withRoast]면 그 평가의 팩폭 결과(대체 문장)도 저장한다.
     * [legacyJson]이 있으면 그 JSON을 옛 작가 프롬프트(4판) 결과로 저장한다.
     */
    private fun participantWithResult(scores: List<Int>, withRoast: Boolean, legacyJson: String? = null): Cookie {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val now = clock.instant()
        val job = AnalysisJob.enqueue(participant.id, IdempotencyKey.from("key-" + UUID.randomUUID()), EMPTY_SUBMISSION, AnalysisPayload("{}"), now)
        val score = Score.shrunk(scores.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        val dossier = TestDossiers.draft(TestDossiers.selves(DOSSIER_TEXTS))
        transactionTemplate.executeWithoutResult {
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            if (withRoast) roastService.record(assessment, roastService.fallbackDraft(score.hundredths, score.components, dossier, 0), now)
            if (legacyJson != null) roastService.record(assessment, RoastDraft(legacyJson, LEGACY_PROMPT_VERSION, LEGACY_MODEL), now)
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
        private const val MIN_CARDS = 5
        private const val MAX_CARDS = 8
        private const val LEGACY_PROMPT_VERSION = 4
        private const val LEGACY_MODEL = "legacy-model"

        /** 작가 프롬프트 4판 결과 꼴(합성 예시). 칭호·담백 판결·짝·쪽지·줄거리가 있고 근거·결론은 없다. */
        private val LEGACY_V4_JSON = """
            {"verdict":"gift","title":{"text":"옛 칭호","source":"AI"},
            "verdict_line":{"text":"옛 판결 한 줄이다. 선물 명단에 올린다.","source":"AI"},
            "verdict_mild":{"text":"옛 담백 판결이다.","source":"AI"},
            "pairs":[{"fact_key":"laugh_max_run","roast":"옛 놀림","flip":"근데 옛 뒤집기","source":"AI","chip":{"label":"ㅋ 한 번에 최대","value":"12개"}},
            {"fact_key":"generic","roast":"일반 놀림","flip":"근데 일반 뒤집기","source":"FALLBACK"}],
            "letter":{"text":"옛 쪽지","source":"AI"},
            "story":[{"act":1,"line":"봄. 옛 줄거리.","source":"AI"}]}
        """.trimIndent()
    }
}
