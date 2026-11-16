package com.santacut.domain.analysis.service

import com.santacut.IntegrationTest
import com.santacut.domain.analysis.model.AnalysisEvent
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.SampleCandidates
import com.santacut.infrastructure.analysis.client.RecordingChatModel
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * 큰 내보내기(본인 메시지 2,000개)를 채점해도 AI에 보내는 사용자 메시지가 글자 예산 안에 드는지,
 * 원문 전체 대신 항목별 신호와 표본만 보내는지 AI 호출 경계(ChatModel)에서 확인한다.
 */
@IntegrationTest
@Import(RecordingChatModelConfiguration::class)
class AnalysisScoringServiceTest @Autowired constructor(
    private val analysisScoringService: AnalysisScoringService,
    private val recordingChatModel: RecordingChatModel,
    private val objectMapper: ObjectMapper,
) {
    @Test
    fun `메시지 2,000개를 채점해도 AI 사용자 메시지는 글자 예산 안이고 표본 줄 번호로 점수를 매긴다`() {
        val payload = objectMapper.writeValueAsString(largeSubmission())

        val digest = analysisScoringService.digest(jobWith(payload))
        val answer = analysisScoringService.ask(digest, SYSTEM_PROMPT)

        val userText = checkNotNull(recordingChatModel.lastPrompt).userMessage.text.orEmpty()
        val sampleLines = userText.substringAfter(SAMPLE_HEADER).lines()
            .filterNot { it.startsWith(WEEK_HEADER_PREFIX) || it.startsWith(GAP_LINE_PREFIX) }
        assertEquals(EXPECTED_HUNDREDTHS, answer.toScore(digest.sampleSize()).hundredths)
        assertTrue(userText.length <= MAX_USER_MESSAGE_CHARS, "length=${userText.length}")
        assertTrue(payload.length > userText.length * MIN_REDUCTION_FACTOR, "payload=${payload.length},user=${userText.length}")
        assertEquals((1..SampleCandidates.MAX_SAMPLE_MESSAGES).toList(), sampleLines.map { it.substringBefore("|").toInt() })
    }

    @Test
    fun `같은 제출이면 채점 입력 해시가 같고 프롬프트 판이 다르면 다르다`() {
        val digest = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission())))
        val again = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission())))

        assertEquals(analysisScoringService.fingerprint(digest, 2), analysisScoringService.fingerprint(again, 2))
        assertNotEquals(analysisScoringService.fingerprint(digest, 2), analysisScoringService.fingerprint(digest, 3))
    }

    /** 90일에 고르게 퍼진 본인 메시지 2,000개. 짧은 웃음, 항목 낱말, 아주 긴 메시지가 섞여 있다. */
    private fun largeSubmission(): AnalysisSubmission = AnalysisSubmission(
        DataSource.KAKAO,
        AnalysisEvents(List(EVENT_COUNT) { index -> AnalysisEvent(occurredAt(index), syntheticText(index)) }),
    )

    private fun occurredAt(index: Int): Instant = LATEST.minus(SPAN.multipliedBy((EVENT_COUNT - index).toLong()))

    private fun syntheticText(index: Int): String {
        val phrase = PHRASES[index % PHRASES.size]
        return if (index % LONG_MESSAGE_EVERY == 0) phrase.repeat(LONG_MESSAGE_REPEAT) else "$phrase $index"
    }

    private fun jobWith(payload: String): AnalysisJob = AnalysisJob(
        UUID.randomUUID().toString(), UUID.randomUUID().toString(), "key", "hash", DataSource.KAKAO, payload, LATEST,
    )

    companion object {
        private const val EVENT_COUNT = 2000
        private const val SYSTEM_PROMPT = "테스트 채점 프롬프트"
        private const val SAMPLE_HEADER = "[sample]\n"
        private const val WEEK_HEADER_PREFIX = "# "
        private const val GAP_LINE_PREFIX = "("
        private const val EXPECTED_HUNDREDTHS = 8000

        /** 신호 블록(약 200자) + 표본 본문 6,000자 + 줄 번호 120개와 주 머리줄 13개를 넉넉히 더한 상한. */
        private const val MAX_USER_MESSAGE_CHARS = 7000
        private const val MIN_REDUCTION_FACTOR = 10
        private const val LONG_MESSAGE_EVERY = 50
        private const val LONG_MESSAGE_REPEAT = 20
        private val LATEST: Instant = Instant.parse("2026-11-15T12:00:00Z")

        /** 2,000개가 89일 안에 들도록 나눈 간격. */
        private val SPAN: Duration = Duration.ofDays(89).dividedBy(EVENT_COUNT.toLong())
        private val PHRASES = listOf(
            "ㅋㅋㅋㅋㅋ",
            "오늘 너무 고마웠어 진짜 덕분에 살았다",
            "내일 몇 시에 만나? 같이 저녁 먹자",
            "괜찮아? 많이 힘들었겠다 푹 쉬어",
            "응",
            "미안 늦었어 지금 가는 중이야",
            "크리스마스에 트리 보러 갈래",
            "오늘 점심 뭐 먹었어",
            "그거 내가 도와줄게 걱정 마",
            "😂😂",
            "회의 끝나고 연락할게",
            "주말에 영화 볼까 아니면 산책할까",
        )
    }
}
