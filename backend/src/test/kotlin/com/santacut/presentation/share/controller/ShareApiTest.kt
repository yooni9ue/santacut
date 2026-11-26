package com.santacut.presentation.share.controller

import com.jayway.jsonpath.JsonPath
import com.santacut.IntegrationTest
import com.santacut.TestCsrf.withCsrf
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
import com.santacut.domain.share.model.RecapDeck
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import tools.jackson.databind.ObjectMapper
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** POST /v1/shares가 고정하는 리캡 카드 덱과 GET /s/{token} 공개 페이지. 팩폭 문장은 대체 문장으로 저장한 결과다. */
@IntegrationTest
class ShareApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val assessmentService: AssessmentService,
    private val roastService: RoastService,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
    private val objectMapper: ObjectMapper,
    private val clock: Clock,
) {
    @Test
    fun `새 공유 링크는 내 리캡과 같은 수와 순서의 카드 덱을 서버에서 만들어 저장한다`() {
        val session = participantWithResult(withRoast = true)
        val recap = mockMvc.perform(get(RECAP_PATH).cookie(session)).andExpect(status().isOk).andReturn().response.contentAsString

        val deck = deckOf(createShare(session))

        assertEquals(RecapDeck.VERSION, deck.version)
        assertEquals(JsonPath.read<List<String>>(recap, "$.cards[*].kind"), deck.cards.convertEach { it.kind.name })
        assertEquals(JsonPath.read<List<Int>>(recap, "$.cards[*].index"), deck.cards.convertEach { it.index })
        assertEquals(JsonPath.read<String>(recap, "$.skin"), deck.skin)
    }

    @Test
    fun `카드 덱이 없는 기존 링크를 다시 공유하면 같은 토큰에 카드 덱을 한 번 채운다`() {
        val session = participantWithResult(withRoast = true)
        val token = createShare(session)
        jdbcTemplate.update("UPDATE shares SET recap_json = NULL WHERE token = ?", token)

        assertEquals(token, createShare(session))

        assertNotNull(recapJsonOf(token))
    }

    @Test
    fun `팩폭 문장이 없는 결과도 공유 링크를 만들고 카드 덱 없이 기존 요약 페이지를 보여 준다`() {
        val session = participantWithResult(withRoast = false)

        val token = createShare(session)

        assertNull(recapJsonOf(token))
        val html = pageOf(token)
        assertTrue(html.contains("자세한 결과 보기") && html.contains("<meta property=\"og:image\""))
        assertFalse(html.contains("data-deck") || html.contains("<script"))
    }

    @Test
    fun `공개 페이지는 저장한 카드를 모두 순서대로 그리고 보안 헤더를 붙인다`() {
        val token = createShare(participantWithResult(withRoast = true))
        val count = deckOf(token).cards.convertEach { it }.size

        val html = pageOf(token)

        assertTrue(html.contains("공유 당시 리캡 카드 <strong>${count}장</strong>"))
        val positions = (1..count).map { html.indexOf("<span class=\"card-index\">$it / $count</span>") }
        assertTrue(positions.all { it >= 0 } && positions == positions.sorted(), "카드 순서: $positions")
        assertTrue(html.contains("<meta property=\"og:url\" content=\"http://localhost:8080/s/$token\">"))
    }

    @Test
    fun `조서 문장과 상대 말은 저장한 카드 덱에도 공개 페이지에도 나오지 않는다`() {
        val token = createShare(participantWithResult(withRoast = true))
        val recapJson = assertNotNull(recapJsonOf(token))
        val html = pageOf(token)

        listOf(SELF_SECRET, PARTNER_SECRET).forEach { secret ->
            assertFalse(recapJson.contains(secret), "카드 덱에 비공개 문자열이 있습니다: $secret")
            assertFalse(html.contains(secret), "공개 페이지에 비공개 문자열이 있습니다: $secret")
        }
    }

    @Test
    fun `취소했거나 없는 링크는 보안 헤더가 붙은 볼 수 없는 결과 페이지다`() {
        val response = mockMvc.perform(get("/s/not-a-token").accept(MediaType.TEXT_HTML))
            .andExpect(status().isNotFound)
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_STORE))
            .andExpect(header().string(REFERRER_POLICY, NO_REFERRER))
            .andReturn().response
        assertSinglePagePolicy(response)
        assertTrue(response.getContentAsString(Charsets.UTF_8).contains("더 이상 볼 수 없는 결과"))
    }

    private fun createShare(session: Cookie): String {
        val body = mockMvc.perform(withCsrf(mockMvc, post(SHARES_PATH).cookie(session)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.imageUrl").isString)
            .andReturn().response.contentAsString
        return JsonPath.read(body, "$.token")
    }

    /** 공개 페이지 HTML. 카드 덱·요약 페이지 모두 보안 헤더와 공유 페이지 정책 하나만 붙어야 한다. */
    private fun pageOf(token: String): String {
        val response = mockMvc.perform(get("/s/$token").accept(MediaType.TEXT_HTML))
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.CACHE_CONTROL, NO_STORE))
            .andExpect(header().string(REFERRER_POLICY, NO_REFERRER))
            .andReturn().response
        assertSinglePagePolicy(response)
        return response.getContentAsString(Charsets.UTF_8)
    }

    /** 정책이 둘이면 브라우저가 교집합을 적용해 인라인 스크립트가 막힌다. 공유 페이지 정책(스크립트 해시) 하나만 있어야 한다. */
    private fun assertSinglePagePolicy(response: MockHttpServletResponse) {
        val policies = response.getHeaders(CSP)
        assertEquals(1, policies.size, "Content-Security-Policy 헤더: $policies")
        assertTrue(policies.single().contains("script-src 'sha256-") && policies.single().contains("frame-ancestors 'none'"), policies.single())
    }

    private fun deckOf(token: String): RecapDeck = objectMapper.readValue(assertNotNull(recapJsonOf(token)), RecapDeck::class.java)

    private fun recapJsonOf(token: String): String? =
        jdbcTemplate.queryForObject("SELECT recap_json FROM shares WHERE token = ?", String::class.java, token)

    /** 평가와 순위를 저장한 참가자의 세션. [withRoast]면 그 평가의 팩폭 결과(대체 문장)도 저장한다. */
    private fun participantWithResult(withRoast: Boolean): Cookie {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val now = clock.instant()
        val job = AnalysisJob.enqueue(participant.id, IdempotencyKey.from("key-" + UUID.randomUUID()), EMPTY_SUBMISSION, AnalysisPayload("{}"), now)
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        val dossier = TestDossiers.draft(TestDossiers.selves(DOSSIER_TEXTS) + PARTNER_MESSAGES)
        transactionTemplate.executeWithoutResult {
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            if (withRoast) roastService.record(assessment, roastService.fallbackDraft(score.hundredths, dossier, 0), now)
            rankingService.rebuild(now)
        }
        return Cookie(SESSION_COOKIE, participant.sessionToken)
    }

    companion object {
        private const val SHARES_PATH = "/v1/shares"
        private const val RECAP_PATH = "/v1/me/recap"
        private const val SESSION_COOKIE = "sc_session"
        private const val REFERRER_POLICY = "Referrer-Policy"
        private const val NO_REFERRER = "no-referrer"
        private const val NO_STORE = "no-store"
        private const val CSP = "Content-Security-Policy"
        private val SCORES = listOf(90, 90, 90, 90)
        private val EMPTY_SUBMISSION = AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList()))
        /** 조서 집계에 넣는 본인 문장과 상대 말. 공유 카드 덱과 공개 페이지에 나오면 안 되는 고유 문자열을 담는다. */
        private const val SELF_SECRET = "조서비공개문장7f3a"
        private const val PARTNER_SECRET = "상대고유발언9c2e"
        private const val PARTNER_MESSAGE_COUNT = 20
        private const val PARTNER_OFFSET_MINUTES = 30L
        private const val MINUTES_APART = 60L
        private val DOSSIER_TEXTS = List(40) { "진짜 ㅋㅋㅋ 고마워 $SELF_SECRET" }
        private val PARTNER_MESSAGES = List(PARTNER_MESSAGE_COUNT) { index ->
            TestDossiers.partner(TestDossiers.START.plusMinutes(PARTNER_OFFSET_MINUTES + index * MINUTES_APART), "$PARTNER_SECRET 고마워")
        }
    }
}
