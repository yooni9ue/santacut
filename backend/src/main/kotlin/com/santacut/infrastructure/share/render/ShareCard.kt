package com.santacut.infrastructure.share.render

/**
 * 공유 페이지와 공유 카드 PNG에 그릴 값. 표시 형식은 이미 맞춘 문자열이고, HTML 이스케이프는 하기 전이다.
 *
 * @property score 소수점 둘째 자리까지 표시한 점수(예: `82.45`)
 * @property eta 서울 시각으로 표시한 도착 예정일
 * @property computedAt 데이터베이스 형식 그대로의 계산 시각(UTC)
 */
data class ShareCard(
    val token: String,
    val nickname: String,
    val score: String,
    val rank: Int,
    val verdict: String,
    val eta: String,
    val comment: String,
    val computedAt: String,
)
