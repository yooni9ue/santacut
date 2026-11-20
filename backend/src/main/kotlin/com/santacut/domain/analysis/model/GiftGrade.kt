package com.santacut.domain.analysis.model

/**
 * 「팩폭 산타」 선물 판정 등급. 총점(100분의 1점) 구간으로 나눈다. 기존 [ScoreVerdict]·순위·배지 경계와는 따로 간다.
 *
 * @property label 카드에 찍는 등급 이름
 * @property stamp 도장 문구. 등급의 온도를 말한다.
 * @property shareText 서버가 고정한 공유 문구 틀. [SCORE_SLOT]에 총점 표시가 들어간다(없는 등급도 있다).
 * @property temperature 팩폭 작가에게 보내는 등급 온도(넉넉한 통과, 아슬아슬 통과, 아깝게 탈락, 석탄)
 */
enum class GiftGrade(val label: String, val stamp: String, val shareText: String, val temperature: String) {
    PREMIUM("굴뚝 프리패스", "산타 최애", "산타가 내 단톡방 다 읽고도 {점수}점 줌. 이게 인성이다. 너도 까봐.", "넉넉한 통과"),
    GIFT_DEPARTED("썰매 탑승 완료", "선물 실었다", "산타가 내 단톡방 다 읽음. {점수}점, 선물 실림. 넌 읽히면 석탄 각.", "넉넉한 통과"),
    SCRAPED_THROUGH("굴뚝에 걸쳐서 통과", "겨우 실었다", "산타가 내 단톡방 다 읽음. {점수}점, 굴뚝에 걸쳐서 겨우 통과. 넌 넘냐?", "아슬아슬 통과"),
    ONE_STEP_SHORT("굴뚝 앞 유턴", "아깝다 진짜", "{점수}점. 굴뚝 앞에서 썰매가 유턴함. 억울해서 올린다. 너는 넘나 보자.", "아깝게 탈락"),
    OFF_LIST("명단에 없음", "산타 읽씹", "산타한테 읽씹당함. {점수}점. 나만 당할 수 없다. 너도 까봐.", "석탄"),
    COAL("석탄 당첨", "반품 불가", "산타가 내 단톡방 읽고 석탄 보냄. 반품도 안 된대. 혼자 받긴 억울하다. 너도 까봐.", "석탄"),
    ;

    /** 총점 표시 [scoreText]를 넣은 공유 문구. */
    fun shareTextWith(scoreText: String): String = shareText.replace(SCORE_SLOT, scoreText)

    companion object {
        /** 공유 문구 틀에서 총점 표시가 들어갈 자리. */
        const val SCORE_SLOT = "{점수}"
        private const val PREMIUM_FROM = 9000
        private const val GIFT_DEPARTED_FROM = 7500
        private const val SCRAPED_THROUGH_FROM = GiftVerdict.CUT_HUNDREDTHS
        private const val ONE_STEP_SHORT_FROM = 5500
        private const val OFF_LIST_FROM = 5000

        /** [hundredths](총점, 100분의 1점)가 든 등급. 9000 이상 굴뚝 프리패스부터 4999 이하 석탄 당첨까지다. */
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
