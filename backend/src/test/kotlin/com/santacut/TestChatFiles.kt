package com.santacut

import com.santacut.domain.global.model.SeoulTime
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 통합 테스트가 올릴 카카오톡 iOS 형식과 인스타그램 JSON 형식 대화 파일. 이벤트 시각 범위(올해 1월 1일부터)를 지키도록 기준 시각에서 거꾸로 만든다.
 */
object TestChatFiles {
    const val SELF_SPEAKER = "산타"
    const val OTHER_SPEAKER = "루돌프"
    const val OTHER_TEXT = "다른 사람의 비밀 이야기"

    private const val MINUTE_SECONDS = 60L
    private const val DAY_SECONDS = 86_400L
    private val IOS_TIME = DateTimeFormatter.ofPattern("yyyy. M. d. H:mm").withZone(SeoulTime.ZONE)

    /** 다른 참여자 메시지 하나와 본인 메시지 [ownCount]개가 있는 대화. 메시지는 [now] 하루 전부터 1분 간격이다. */
    fun kakaoIos(now: Instant, ownCount: Int): ByteArray {
        val start = now.minusSeconds(DAY_SECONDS)
        val other = line(start.minusSeconds(MINUTE_SECONDS), OTHER_SPEAKER, OTHER_TEXT)
        val own = List(ownCount) { index -> line(start.plusSeconds(index * MINUTE_SECONDS), SELF_SPEAKER, "고마워 $index") }
        return (listOf(other) + own).joinToString("\n").toByteArray()
    }

    /** [kakaoIos]와 같은 메시지를 담은 인스타그램 `message_1.json`. 실제 파일처럼 최신 메시지부터 적는다. */
    fun instagram(now: Instant, ownCount: Int): ByteArray {
        val start = now.minusSeconds(DAY_SECONDS)
        val other = message(start.minusSeconds(MINUTE_SECONDS), OTHER_SPEAKER, OTHER_TEXT)
        val own = List(ownCount) { index -> message(start.plusSeconds(index * MINUTE_SECONDS), SELF_SPEAKER, "고마워 $index") }
        return ("{\"messages\":[" + (listOf(other) + own).reversed().joinToString(",") + "]}").toByteArray()
    }

    private fun message(at: Instant, speaker: String, text: String): String =
        "{\"sender_name\":\"$speaker\",\"timestamp_ms\":${at.toEpochMilli()},\"content\":\"$text\"}"

    private fun line(at: Instant, speaker: String, text: String): String = "${IOS_TIME.format(at)}, $speaker : $text"
}
