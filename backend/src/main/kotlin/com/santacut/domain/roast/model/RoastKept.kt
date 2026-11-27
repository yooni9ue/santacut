package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 다시 쓰기 입력(rewrite.kept)에 싣는, 앞선 응답에서 이미 받아 둔 문장. 서버는 이 문장을 갈아 끼우지 않으므로
 * 작가는 새로 쓰는 묶음이 이것과 겹치지 않게 써야 한다(판정 한 줄과 마지막 한 줄의 펀치, 카드끼리의 칩·줄·낱말·라벨).
 *
 * @property verdictLine 받아 둔 판정 한 줄. 아직 없으면 싣지 않는다.
 * @property evidence 받아 둔 근거 카드
 * @property finalLine 받아 둔 결론 마지막 한 줄. 아직 없으면 싣지 않는다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RoastKept(
    @JsonProperty("verdict_line") val verdictLine: String?,
    val evidence: List<KeptCard>,
    @JsonProperty("final_line") val finalLine: String?,
)
