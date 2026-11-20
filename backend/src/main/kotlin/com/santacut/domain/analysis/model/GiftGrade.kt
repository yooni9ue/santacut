package com.santacut.domain.analysis.model

/**
 * 「팩폭 산타」 선물 판정 등급. 총점(100분의 1점) 구간으로 나눈다. 기존 [ScoreVerdict]·순위·배지 경계와는 따로 간다.
 *
 * @property label 카드에 찍는 등급 이름
 */
enum class GiftGrade(val label: String) {
    PREMIUM("특급"),
    GIFT_DEPARTED("선물 배송 출발"),
    SCRAPED_THROUGH("턱걸이 통과"),
    ONE_STEP_SHORT("한 끗"),
    OFF_LIST("명단 밖"),
    COAL("석탄"),
    ;

    companion object {
        private const val PREMIUM_FROM = 9000
        private const val GIFT_DEPARTED_FROM = 7500
        private const val SCRAPED_THROUGH_FROM = GiftVerdict.CUT_HUNDREDTHS
        private const val ONE_STEP_SHORT_FROM = 5500
        private const val OFF_LIST_FROM = 5000

        /** [hundredths](총점, 100분의 1점)가 든 등급. 9000 이상 특급부터 4999 이하 석탄까지다. */
        fun of(hundredths: Int): GiftGrade = when {
            hundredths >= PREMIUM_FROM -> PREMIUM
            hundredths >= GIFT_DEPARTED_FROM -> GIFT_DEPARTED
            hundredths >= SCRAPED_THROUGH_FROM -> SCRAPED_THROUGH
            hundredths >= ONE_STEP_SHORT_FROM -> ONE_STEP_SHORT
            hundredths >= OFF_LIST_FROM -> OFF_LIST
            else -> COAL
        }
    }
}
