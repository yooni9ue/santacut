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

    /** 도장 문구. 등급마다 다르다([GiftGrade.stamp]). 예: 굴뚝에 걸쳐서 통과면 "겨우 실었다". */
    fun stamp(): String = grade().stamp

    /** 농담을 끈 사람의 도장 문구. 통과 등급은 도장과 같고, 탈락 세 등급은 "내년 명단 예약"이다. */
    fun stampMild(): String = if (passed) stamp() else MILD_COAL_STAMP

    /** 판결 제목. 도장과 등급을 잇는다. 예: "겨우 실었다 · 굴뚝에 걸쳐서 통과". */
    fun headline(): String = stamp() + SEPARATOR + grade().label

    /** 농담을 끈 판결 제목. 통과 등급은 도장과 등급을 잇고, 탈락 세 등급은 등급 이름 없이 "내년 명단 예약"만 쓴다. */
    fun mildHeadline(): String = if (passed) headline() else stampMild()

    fun margin(): CutMargin = CutMargin.of(grade())

    /** 작가 입력과 저장에 쓰는 판정 key. 선물이면 gift, 아니면 coal이다. */
    fun key(): String = if (passed) GIFT_KEY else COAL_KEY

    /** 리캡 카드 스킨. 선물이면 GIFT, 아니면 COAL이다. */
    fun skin(): String = key().uppercase()

    /** 서버가 고정한 공유 문구. 등급마다 다르고 총점 표시를 넣는다([GiftGrade.shareTextWith]). */
    fun shareText(): String = grade().shareTextWith(scoreText())

    /** 농담을 끈 사람의 공유 문구. 점수와 놀림 없이 통과면 선물 받은 문구, 탈락이면 내년으로 미룬 문구 하나다. */
    fun shareTextMild(): String = if (passed) MILD_GIFT_SHARE_TEXT else MILD_COAL_SHARE_TEXT

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
        private const val MILD_COAL_STAMP = "내년 명단 예약"
        private const val MILD_GIFT_SHARE_TEXT = "산타가 내 단톡방 다 읽음. 올해 선물 받음. 너도 까봐."
        private const val MILD_COAL_SHARE_TEXT = "올해 선물은 내년으로 미뤄짐. 너는 받았는지 까봐."
    }
}
