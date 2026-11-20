package com.santacut.domain.analysis.model

/**
 * 총점이 컷(65점)에서 떨어진 정도. 팩폭 작가 입력의 margin으로 보낸다. 숫자 대신 말만 보낸다.
 * 등급([GiftGrade]) 경계를 그대로 따른다. 컷 위아래 10점 안(굴뚝에 걸쳐서 통과, 굴뚝 앞 유턴)이 아슬아슬이다.
 *
 * @property word 작가에게 보내는 말
 */
enum class CutMargin(val word: String) {
    AMPLE("넉넉"),
    CLOSE("아슬아슬"),
    FAR("한참"),
    ;

    companion object {
        /** [grade]의 말. 굴뚝 프리패스·썰매 탑승 완료는 넉넉, 굴뚝에 걸쳐서 통과·굴뚝 앞 유턴은 아슬아슬, 그 아래는 한참이다. */
        fun of(grade: GiftGrade): CutMargin = when (grade) {
            GiftGrade.PREMIUM, GiftGrade.GIFT_DEPARTED -> AMPLE
            GiftGrade.SCRAPED_THROUGH, GiftGrade.ONE_STEP_SHORT -> CLOSE
            GiftGrade.OFF_LIST, GiftGrade.COAL -> FAR
        }
    }
}
