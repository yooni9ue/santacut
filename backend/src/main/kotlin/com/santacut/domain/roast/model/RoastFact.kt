package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 작가에게 보내는 사실 하나. 숫자 값은 보내지 않고 남들과 비교한 수준([level])만 말로 보낸다.
 * 칩과 고르기 점수는 서버만 쓰고 이 JSON에는 싣지 않는다(칩은 칩 후보 목록으로 따로 보낸다).
 *
 * @property group 사실의 계열. 한 계열이 재료를 독차지하지 않게 고를 때 겹치지 않게 한다.
 * @property score 고르기 점수(튀는 정도 × 편집 무게, 0~). 클수록 앞자리에 뽑힐 가능성이 크다. 서버만 쓴다.
 * @property quoteRule 이 칩 근거 카드에 붙여도 되는 말풍선 조건. 입버릇 칩은 [QuoteRule.PHRASE], 그 밖에 정하지 않으면 말풍선 없이 쓰는 [QuoteRule.NONE]이다. 서버만 쓴다.
 * @property direction 칩의 방향. 어느 근거 종류에 쓸 수 있는지 정한다([ChipDirection.allows]). 작가에게는 칩 후보의 kinds로 알린다.
 * @property dimension 방향이 가리키는 항목 점수 key. 근거 카드 영역 태그에 쓴다. 서버만 쓴다.
 * @property topics 칩이 말하는 버릇의 낱말. 근거 한 줄이 이 낱말과 겹쳐도 근거를 말한 것으로 친다. 작가에게도 보낸다.
 */
data class RoastFact(
    val key: String,
    val meaning: String,
    val level: String,
    val group: String,
    @get:JsonIgnore val chip: RoastChip,
    @get:JsonIgnore val score: Double = 0.0,
    @get:JsonIgnore val quoteRule: QuoteRule = if (key.startsWith(PhraseFactSpec.KEY_PREFIX)) QuoteRule.PHRASE else QuoteRule.NONE,
    @get:JsonIgnore val direction: ChipDirection = ChipDirection.EITHER,
    @get:JsonIgnore val dimension: String? = null,
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY) val topics: List<String> = emptyList(),
)
