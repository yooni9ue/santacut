package com.santacut.domain.dossier.model

/**
 * 이모지 집계. 이모지는 자소 묶음(그래핌) 하나를 한 개로 센다.
 *
 * @property top 많이 쓴 이모지 상위 3개
 * @property density 본인 메시지 100개당 이모지 수(소수 첫째 자리)
 */
data class EmojiStats(
    val top: EmojiTallies,
    val density: Double,
)
