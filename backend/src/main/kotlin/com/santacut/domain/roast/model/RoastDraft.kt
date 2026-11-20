package com.santacut.domain.roast.model

/**
 * 저장하기 전의 팩폭 결과. 작가 호출과 JSON 직렬화는 쓰기 트랜잭션 밖에서 미리 한다.
 *
 * @property contentJson 검증을 통과한 문장과 대체 문장, 출처([RoastContent]) JSON. 말풍선 원문은 담지 않는다.
 * @property promptVersion 작가 프롬프트 판. 작가를 부르지 않았으면 null이다.
 * @property model 작가 모델 ID. 작가를 부르지 않았으면 빈 문자열이다.
 */
data class RoastDraft(val contentJson: String, val promptVersion: Int?, val model: String)
