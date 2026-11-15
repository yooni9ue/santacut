package com.santacut.domain.analysis.model

/**
 * 점수 구간별 재미용 판정과 한마디.
 */
enum class ScoreVerdict(
    private val minimumHundredths: Int,
    val title: String,
    val comment: String,
) {
    SANTA_FAMILY(9000, "산타 친자 의혹", "이 정도면 선물 말고 상속을 기다려라."),
    GIFT_DEPARTED(7500, "선물 배송 출발", "착한 척인지는 모르겠는데 일단 속아드림."),
    DELIVERY_ON_HOLD(5000, "굴뚝 앞 배송 보류", "산타가 네 선물 찾느라 안경을 다시 씀."),
    COAL_ON_DELIVERY(0, "석탄 착불 배송", "크리스마스 선물인데 새해 복 많이 받아라."),
    ;

    /** 점수가 이 판정의 하한 이상인지. */
    fun covers(hundredths: Int): Boolean = hundredths >= minimumHundredths

    companion object {
        fun of(hundredths: Int): ScoreVerdict = ScoreVerdicts(entries).find(hundredths) ?: COAL_ON_DELIVERY
    }
}
