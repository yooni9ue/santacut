package com.santacut.domain.roast.model

import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper

/**
 * 작가 응답 하나를 읽고 검증한 결과. JSON으로 읽을 수 없으면 [output]이 null이고 [checks]에 json 실패 하나만 있다.
 */
class RoastReview(val output: RoastOutput?, val checks: RoastChecks) {
    companion object {
        /** 이보다 긴 응답은 읽지 않는다(폭주 방어). */
        private const val MAX_RESPONSE_LENGTH = 100_000

        /**
         * 작가 응답 본문 [rawJson]을 입력 [brief] 기준으로 필드마다 검증한다. 테스트·실측 도구에서도 이 함수 하나로 검증을 돌린다.
         * 비었거나 너무 길거나 JSON 객체가 아니면 읽을 수 없는 응답이다.
         */
        fun of(rawJson: String?, brief: RoastBrief, rules: RoastRules, objectMapper: ObjectMapper): RoastReview {
            if (rawJson.isNullOrBlank() || rawJson.length > MAX_RESPONSE_LENGTH) return RoastReview(null, RoastChecks.unreadable())
            val node = try {
                objectMapper.readTree(rawJson)
            } catch (exception: JacksonException) {
                null
            }
            if (node == null || !node.isObject) return RoastReview(null, RoastChecks.unreadable())
            val output = RoastOutput.from(node)
            return RoastReview(output, RoastChecks.of(output, brief, rules))
        }
    }
}
