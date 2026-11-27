package com.santacut.presentation.roast.response

/**
 * 리캡 카드 한 장(9:16)의 공통 필드. 카드 종류([kind])마다 구현이 나머지 필드를 더한다.
 * 모든 카드에 상단 바 값(rank, top_percent)과 총점 표시(score_text)가 있다.
 * JSON 이름은 구현의 필드 애노테이션이 정한다(index, kind, width, height, score_text, rank, top_percent).
 */
sealed interface RecapCardResponse {
    val index: Int
    val kind: String
    val width: Int
    val height: Int
    val scoreText: String
    val rank: Int?
    val topPercent: Int?

    companion object {
        /** 카드 가로 픽셀(9:16). */
        const val WIDTH = 1080

        /** 카드 세로 픽셀(9:16). */
        const val HEIGHT = 1920
    }
}
