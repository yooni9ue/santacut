package com.santacut.domain.analysis.model

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.model.SeoulTime
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import kotlin.test.assertEquals

/**
 * 원본 파일을 본인 메시지만 담은 제출 내용으로 바꾸는 규칙을 Spring 없이 확인한다. 원본은 카카오톡 iOS 형식이다.
 */
class AnalysisUploadTest {
    private val now = Instant.parse("2026-11-15T00:00:00Z")
    private val yesterday = now.minus(Duration.ofDays(1))
    private val noInstagram: (String) -> InstagramThread = { error("카카오톡 원본은 JSON으로 읽지 않는다") }
    private val noDossier: (DataSource, AnalysisEvents) -> DossierTally? = { _, _ -> null }

    @Test
    fun `다른 참여자 메시지는 이름 대신 상대로 표시해 본인 메시지와 함께 이벤트로 바꾼다`() {
        val lines = listOf(line(yesterday, OTHER_SPEAKER, "남의 비밀")) + ownLines(yesterday, MIN_EVENTS)
        val submission = upload(lines).normalize(now, noInstagram, noDossier)
        assertEquals(DataSource.KAKAO, submission.source)
        assertEquals(listOf("남의 비밀") + List(MIN_EVENTS) { "고마워 $it" }, texts(submission))
        assertEquals(listOf("상대") + List(MIN_EVENTS) { null }, submission.events.toList().map { it.partner })
    }

    @Test
    fun `참여자 표시 이름은 본인을 포함해 처음 나온 순서로 제출 내용에 담는다`() {
        val lines = listOf(line(yesterday, OTHER_SPEAKER, "안녕")) + ownLines(yesterday, MIN_EVENTS)

        val submission = upload(lines).normalize(now, noInstagram, noDossier)

        assertEquals(listOf(OTHER_SPEAKER, SELF_SPEAKER), submission.speakerNames)
    }

    @Test
    fun `올해 1월 1일(한국 시각) 전이거나 5분 넘게 미래인 메시지는 버린다`() {
        val stale = line(AnalysisEvent.periodStart(now).minusSeconds(60), SELF_SPEAKER, "작년 메시지")
        val future = line(now.plus(Duration.ofMinutes(10)), SELF_SPEAKER, "미래 메시지")
        val submission = upload(listOf(stale) + ownLines(yesterday, MIN_EVENTS) + future).normalize(now, noInstagram, noDossier)
        assertEquals(List(MIN_EVENTS) { "고마워 $it" }, texts(submission))
    }

    @Test
    fun `본인 메시지가 2,000개를 넘으면 한 달 안에서는 최신 2,000개만 남긴다`() {
        val lines = ownLines(now.minus(Duration.ofDays(2)), AnalysisEvents.MAX_EVENTS + 1)
        val texts = texts(upload(lines).normalize(now, noInstagram, noDossier))
        assertEquals(AnalysisEvents.MAX_EVENTS, texts.size)
        assertEquals("고마워 1", texts.first())
    }

    @Test
    fun `본인 메시지가 20개보다 적으면 INSUFFICIENT_EVENTS다`() {
        val lines = ownLines(yesterday, MIN_EVENTS - 1) + line(now, OTHER_SPEAKER, "남의 메시지")
        assertErrorCode(ErrorCode.INSUFFICIENT_EVENTS) { upload(lines).normalize(now, noInstagram, noDossier) }
    }

    @Test
    fun `고른 본인 이름이 대화에 없으면 SPEAKER_NOT_IDENTIFIED다`() {
        val lines = ownLines(yesterday, MIN_EVENTS)
        assertErrorCode(ErrorCode.SPEAKER_NOT_IDENTIFIED) {
            upload(lines, selfSpeaker = "모르는 사람").normalize(now, noInstagram, noDossier)
        }
    }

    private fun upload(lines: List<String>, selfSpeaker: String = SELF_SPEAKER): AnalysisUpload =
        AnalysisUpload(JOB_ID, DataSource.KAKAO, selfSpeaker, lines.joinToString("\n").toByteArray(), now)

    private fun ownLines(start: Instant, count: Int): List<String> =
        List(count) { index -> line(start.plusSeconds(index * MINUTE_SECONDS), SELF_SPEAKER, "고마워 $index") }

    private fun line(at: Instant, speaker: String, text: String): String = "${IOS_TIME.format(at)}, $speaker : $text"

    private fun texts(submission: AnalysisSubmission): List<String> = submission.events.toList().map { it.text }

    companion object {
        private const val JOB_ID = "job-1"
        private const val SELF_SPEAKER = "산타"
        private const val OTHER_SPEAKER = "루돌프"
        private const val MIN_EVENTS = AnalysisEvents.MIN_EVENTS
        private const val MINUTE_SECONDS = 60L
        private val IOS_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy. M. d. H:mm").withZone(SeoulTime.ZONE)
    }
}
