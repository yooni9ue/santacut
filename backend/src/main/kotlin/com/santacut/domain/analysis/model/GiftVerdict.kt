package com.santacut.domain.analysis.model

import kotlin.math.abs

/**
 * 「팩폭 산타」 선물 판정. 총점 [hundredths](100분의 1점)가 컷 65점([CUT_HUNDREDTHS]) 이상이면 선물을 받고, 아래면 석탄이다.
 * 재심은 없다. 기존 결과 API의 [ScoreVerdict]와 순위·배지 경계는 그대로 두고 리캡 카드와 조서 판결 장만 이 판정을 쓴다.
 */
data class GiftVerdict(val hundredths: Int) {
    /** 선물을 받는지(컷 이상). */
    val passed: Boolean = hundredths >= CUT_HUNDREDTHS

    fun grade(): GiftGrade = GiftGrade.of(hundredths)

    /** 총점 - 컷(100분의 1점). 통과면 0 이상이다. */
    fun cutDistanceHundredths(): Int = hundredths - CUT_HUNDREDTHS

    /** 컷 거리 표시. 부호를 늘 붙이고 소수 둘째 자리까지 쓴다("+8.73", "-3.20", "+0.00"). */
    fun cutDistanceText(): String {
        val distance = cutDistanceHundredths()
        return (if (distance < 0) MINUS else PLUS) + pointsText(abs(distance))
    }

    /** 총점 표시. 소수 둘째 자리까지 쓴다("73.73"). */
    fun scoreText(): String = (if (hundredths < 0) MINUS else "") + pointsText(abs(hundredths))

    /** 도장 문구. 선물이면 "선물 배송 확정", 아니면 "석탄"이다. */
    fun stamp(): String = if (passed) GIFT_STAMP else COAL_STAMP

    /** 농담을 끈 사람의 도장 문구. 석탄 대신 "선물은 내년에"다. */
    fun stampMild(): String = if (passed) GIFT_STAMP else MILD_COAL_STAMP

    /** 판결 제목. 도장과 등급을 잇고, 등급 이름이 도장과 같으면(석탄) 도장만 쓴다. 예: "선물 배송 확정 · 턱걸이 통과". */
    fun headline(): String = joined(stamp(), grade().label)

    /** 농담을 끈 판결 제목. 석탄 등급 이름은 쓰지 않는다. */
    fun mildHeadline(): String = if (grade() == GiftGrade.COAL) stampMild() else joined(stampMild(), grade().label)

    fun margin(): CutMargin = CutMargin.of(cutDistanceHundredths())

    /** 작가 입력과 저장에 쓰는 판정 key. 선물이면 gift, 아니면 coal이다. */
    fun key(): String = if (passed) GIFT_KEY else COAL_KEY

    /** 리캡 카드 스킨. 선물이면 GIFT, 아니면 COAL이다. */
    fun skin(): String = key().uppercase()

    /** 서버가 고정한 공유 문구. */
    fun shareText(): String = if (passed) GIFT_SHARE_TEXT else COAL_SHARE_TEXT

    private fun joined(stamp: String, gradeLabel: String): String = if (stamp == gradeLabel) stamp else stamp + SEPARATOR + gradeLabel

    private fun pointsText(absoluteHundredths: Int): String =
        (absoluteHundredths / HUNDRED).toString() + DECIMAL_POINT + (absoluteHundredths % HUNDRED).toString().padStart(2, ZERO)

    companion object {
        /** 선물 컷(100분의 1점). 65점이다. */
        const val CUT_HUNDREDTHS = 6500

        /** 선물 컷(점). */
        const val CUT_POINTS = CUT_HUNDREDTHS / 100
        const val GIFT_KEY = "gift"
        const val COAL_KEY = "coal"
        private const val HUNDRED = 100
        private const val ZERO = '0'
        private const val PLUS = "+"
        private const val MINUS = "-"
        private const val DECIMAL_POINT = "."
        private const val SEPARATOR = " · "
        private const val GIFT_STAMP = "선물 배송 확정"
        private const val COAL_STAMP = "석탄"
        private const val MILD_COAL_STAMP = "선물은 내년에"
        private const val GIFT_SHARE_TEXT = "산타 공식 인증. 올해 선물 받음. 너는?"
        private const val COAL_SHARE_TEXT = "나 올해 석탄이래. 사유 봐라."
    }
}
