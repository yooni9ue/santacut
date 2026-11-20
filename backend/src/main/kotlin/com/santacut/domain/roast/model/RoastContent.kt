package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 저장하는 팩폭 결과(roasts.content_json). 검증을 통과한 작가 문장과 서버 대체 문장, 필드별 출처를 담는다.
 * 말풍선 원문 인용은 담지 않는다(인용은 사전 표기 원형 입버릇뿐이다).
 *
 * @property verdict 쓸 때의 판정 key(gift, coal)
 * @property verdictLine 판결 한 줄(농담)
 * @property verdictMild 놀림 없는 판결(농담을 끈 사람용)
 * @property pairs 놀림·뒤집기 짝. 화면에는 앞의 세 짝을 쓴다.
 * @property story 세 막 줄거리. 12/9 화면부터 쓴다.
 */
data class RoastContent(
    val verdict: String,
    val title: RoastText,
    @JsonProperty("verdict_line") val verdictLine: RoastText,
    @JsonProperty("verdict_mild") val verdictMild: RoastText,
    val pairs: RoastPairs,
    val letter: RoastText,
    val story: RoastStoryLines,
) {
    /** 대체 문장으로 채운 문장 수. 로그에는 이 수와 필드 이름만 남긴다. */
    fun fallbackCount(): Int =
        title.fallbackCount() + verdictLine.fallbackCount() + verdictMild.fallbackCount() + letter.fallbackCount() +
            pairs.fallbackCount() + story.fallbackCount()
}
