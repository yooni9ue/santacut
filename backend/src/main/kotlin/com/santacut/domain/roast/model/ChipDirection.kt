package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 칩 사실의 방향(roast/facts.json의 directions, 입버릇은 praise·negative 목록). 산타가 보기에 좋은 버릇인지, 나쁜 버릇인지, 어느 쪽도 아닌지다.
 * 선물 받는 이유와 살아남은 말은 좋은 쪽([GOOD]), 점수 깎인 말은 나쁜 쪽([BAD]) 칩만 근거로 쓴다. 중립([NEUTRAL]) 칩은 근거 카드에 쓰지 않는다.
 * 어느 쪽으로도 읽히는 입버릇('출발', '벌써')은 [EITHER]라 모든 근거 종류에 쓴다. 칭찬 입버릇('와', '대박')을 깎인 말로,
 * 근무시간에 조용한 것을 잘못으로, '하루 폭주 배수'를 폭주 없음으로 뒤집어 쓰지 않게 한다.
 */
enum class ChipDirection {
    @JsonProperty("+")
    GOOD,

    @JsonProperty("-")
    BAD,

    @JsonProperty("0")
    NEUTRAL,

    @JsonProperty("±")
    EITHER,
    ;

    /** [kind] 근거 카드의 칩으로 쓸 수 있는지. 이유·살아남은 말은 좋은 쪽, 깎인 말은 나쁜 쪽, 중립은 어느 근거에도 못 쓴다. */
    fun allows(kind: EvidenceKind): Boolean = when (this) {
        GOOD -> kind != EvidenceKind.CUT
        BAD -> kind == EvidenceKind.CUT
        NEUTRAL -> false
        EITHER -> true
    }

    /** 이 칩을 쓸 수 있는 근거 종류 key. 작가 입력의 칩 후보 kinds에 싣는다. 모든 종류면 null이다. */
    fun kindKeys(): List<String>? =
        if (this == EITHER) null else EvidenceKind.entries.filter(::allows).map(EvidenceKind::key)

    /** 근거 카드에 쓸 수 있는 방향인지. */
    fun isEvidence(): Boolean = this != NEUTRAL
}
