package com.santacut.infrastructure.share.render

/**
 * 공유 페이지와 공유 카드 PNG에 그릴 값. 표시 형식은 이미 맞춘 문자열이고, HTML 이스케이프는 하기 전이다.
 *
 * @property score 소수점 둘째 자리까지 표시한 점수(예: `82.45`)
 * @property rank 표시용 순위(예: `3위`, `공동 3위`)
 * @property eta 서울 시각으로 표시한 도착 예정일
 * @property computedAt 서울 시각으로 표시한 순위 계산 시각
 * @property stage 공유 당시 순위 상태(예: `미리보기`, `시즌 확정`)
 * @property sharedAt 서울 시각으로 표시한 공유 시각
 * @property archetype 산타 유형 이름. 예전 링크면 빈 문자열이다.
 * @property bars 항목 막대 요약(예: `공감 72 · 응원 65 · 존중 70 · 협력 58`). 예전 링크면 빈 문자열이다.
 * @property highlights 배지, 상위 %, 가장 약한 항목을 이은 줄
 * @property quote 본인이 싣기로 고른 한마디. 없으면 null이다. 상대 말은 들어오지 않는다.
 */
data class ShareCard(
    val token: String,
    val nickname: String,
    val score: String,
    val rank: String,
    val verdict: String,
    val eta: String,
    val comment: String,
    val computedAt: String,
    val stage: String,
    val sharedAt: String,
    val archetype: String = "",
    val bars: String = "",
    val highlights: String = "",
    val quote: String? = null,
)
