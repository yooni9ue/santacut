package com.santacut.application.participant.command

/**
 * 한 상위 지역의 바로 아래 지역 목록을 조회한다.
 *
 * @property parentCode 상위 지역 코드. null이면 최상위 지역(시·도)을 조회한다.
 */
data class ListRegionsCommand(
    val parentCode: String?,
)
