package com.santacut.domain.analysis.service

import com.santacut.IntegrationTest
import com.santacut.domain.analysis.model.AnalysisEvent
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoringChunks
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
 * 큰 내보내기(본인과 상대 메시지 2,000개)를 채점하면 대화 전체를 화자 표시와 줄 번호를 붙인 대화문으로 보내는지,
 * 채점 입력 해시가 상대 메시지까지 덮는지 AI 호출 경계(ChatModel)에서 확인한다.
 */
@IntegrationTest
@Import(RecordingChatModelConfiguration::class)
class AnalysisScoringServiceTest @Autowired constructor(
    private val analysisScoringService: AnalysisScoringService,
    private val recordingChatModel: RecordingChatModel,
    private val objectMapper: ObjectMapper,
) {
    @Test
    fun `메시지 2,000개를 채점하면 대화문을 구간으로 나눠 보내고 구간 점수를 합쳐 수축한다`() {
        recordingChatModel.prompts.clear()
        val payload = objectMapper.writeValueAsString(largeSubmission())

        val digest = analysisScoringService.digest(jobWith(payload))
        val recorded = mutableListOf<ModelUsage>()
        val answers = analysisScoringService.ask(digest, SYSTEM_PROMPT) { usage -> recorded += usage }

        val texts = recordingChatModel.prompts.map { it.userMessage.text.orEmpty() }
        val lines = texts.flatMap { text -> text.substringAfter(TRANSCRIPT_HEADER).lines().filterNot { it.startsWith(GAP_LINE_PREFIX) } }
        assertEquals(EXPECTED_HUNDREDTHS, analysisScoringService.score(answers).hundredths)
        assertTrue(texts.size in 2..ScoringChunks.MAX_CHUNKS, "chunks=${texts.size}")
        assertEquals(texts.size, recorded.size)
        assertEquals((1..digest.lineCount()).toList(), lines.map { it.substringBefore("|").toInt() }.distinct().sorted())
        assertEquals(setOf(SELF, PARTNER), lines.map { it.split("|")[1] }.toSet())
        assertTrue(texts.all { it.length < payload.length / 2 }, "payload=${payload.length}")
    }

    @Test
    fun `사용량 기록이 실패해도 이미 과금된 답을 버리지 않고 점수를 낸다`() {
        val digest = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission())))

        val answers = analysisScoringService.ask(digest, SYSTEM_PROMPT) { throw IllegalStateException("기록 실패") }

        assertEquals(EXPECTED_HUNDREDTHS, analysisScoringService.score(answers).hundredths)
    }

    @Test
    fun `상대 메시지만 달라도 채점 입력 해시가 다르다`() {
        val digest = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission())))
        val changed = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission(partnerSuffix = "!"))))

        assertNotEquals(analysisScoringService.fingerprint(digest, 4), analysisScoringService.fingerprint(changed, 4))
    }

    @Test
    fun `같은 제출이면 채점 입력 해시가 같고 프롬프트 판이 다르면 다르다`() {
        val digest = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission())))
        val again = analysisScoringService.digest(jobWith(objectMapper.writeValueAsString(largeSubmission())))

        assertEquals(analysisScoringService.fingerprint(digest, 2), analysisScoringService.fingerprint(again, 2))
        assertNotEquals(analysisScoringService.fingerprint(digest, 2), analysisScoringService.fingerprint(digest, 3))
    }

    /** 일정 간격으로 퍼진 메시지 2,000개. 본인과 상대가 번갈아 보내고, 짧은 웃음과 아주 긴 메시지가 섞여 있다. */
    private fun largeSubmission(partnerSuffix: String = ""): AnalysisSubmission = AnalysisSubmission(
        DataSource.KAKAO,
        AnalysisEvents(
            List(EVENT_COUNT) { index ->
                if (index % 2 == 0) {
                    AnalysisEvent(occurredAt(index), syntheticText(index))
                } else {
                    AnalysisEvent(occurredAt(index), syntheticText(index) + partnerSuffix, PARTNER)
                }
            },
        ),
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
        private const val TRANSCRIPT_HEADER = "[대화]\n"
        private const val SELF = "나"
        private const val PARTNER = "상대"
        private const val GAP_LINE_PREFIX = "("
        /** 모든 구간 80점, 기회 수 모름(n=2), 본인 턴 충분(w=2), C=0.25면 (13.75 + 160) / 2.25 = 77.22점이다. */
        private const val EXPECTED_HUNDREDTHS = 7722

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
