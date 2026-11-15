package com.santacut.domain.ranking.model

/**
 * 순위의 확정 상태. 시즌 마감 전에는 참가자가 늘면 바뀔 수 있는 미리보기이고, 마감 후에는 확정이다.
 *
 * @property label 공유 카드·페이지에 쓰는 한국어 이름
 */
enum class RankingStage(val label: String) {
    PREVIEW("미리보기"),
    FINAL("시즌 확정"),
}
