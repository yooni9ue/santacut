package com.santacut.application.ranking.command

/**
 * 순위표 한 페이지를 조회한다.
 *
 * @property cursor 이전 응답의 next_cursor. 첫 페이지는 null이다.
 */
data class RankingPageCommand(
    val cursor: String?,
)
