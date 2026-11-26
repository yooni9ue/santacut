package com.santacut.application.roast.result

/**
 * 리캡 덱 안 카드 한 장의 자리. 카드 값은 [RecapResult]에 있고, 여기에는 순서([index])와 종류([kind])만 둔다.
 * * 덱 순서는 [RecapCards]가 정한다.
 *
 * @property pair 팩폭 카드([RecapCardKind.ROAST])가 싣는 짝. 다른 종류는 null이다.
 */
data class RecapCard(val index: Int, val kind: RecapCardKind, val pair: RecapPairResult? = null)
