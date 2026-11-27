package com.santacut.application.roast.result

/**
 * 모든 리캡 카드 상단 바 값(`전국 {rank}등 · 상위 {topPercent}%`, 상위 비율이 없으면 `전국 {rank}등`). 순위는 순위 계산이 끝나면 참가자 수와 상관없이 늘 싣는다.
 *
 * @property rank 전국 순위. 최신 평가가 아직 순위에 오르지 않았으면(재업로드 직후) null이다.
 * @property topPercent 상위 비율(올림, 1~100). 순위와 그 순위 계산의 참가자 수로 내고, 순위가 없거나 참가자가 100명 아래면 null이다.
 * @property total 그 순위 계산의 참가자 수. 순위가 없으면 null이다.
 * @property rankingPending 최신 평가가 아직 순위에 반영되지 않았는지. 참이면 순위를 다시 조회한다.
 */
data class RecapTopResult(val rank: Int?, val topPercent: Int?, val total: Long?, val rankingPending: Boolean)
