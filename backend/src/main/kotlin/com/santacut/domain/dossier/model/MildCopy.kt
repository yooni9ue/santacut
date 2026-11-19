package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 담백 톤과 놀릴 수 없는 지표에 쓰는 중립 문구(copy.json의 `mild`). 숫자 슬롯과 놀림 없이 쓴다.
 *
 * @property hookMemo 담백 톤에서 올해의 한마디 장 메모
 * @property summons 담백 톤에서 소환장 줄. 죄목·놀림 없이 조서를 받아 보라는 말만 한다.
 */
data class MildCopy(
    @JsonProperty("extremes_memo") val extremesMemo: Templates,
    val summary: Templates,
    @JsonProperty("hook_memo") val hookMemo: Templates,
    val summons: Templates,
)
