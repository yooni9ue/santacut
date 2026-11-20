package com.santacut.infrastructure.roast.client

import com.santacut.domain.roast.model.RoastTextKind

/**
 * 팩폭 작가 응답의 JSON 스키마. Gemini 구조화 출력으로 보내 응답 형태를 모델 쪽에서 강제한다.
 * 모든 키를 필수로 둔다. 문장 필드마다 서버 검증과 같은 글자 수 상한([RoastTextKind.maxLength])을 `maxLength`로,
 * 허용 범위를 `description`으로 싣는다. 글자 수 초과가 첫 응답이 떨어지는 흔한 까닭이라 모델이 쓰는 동안 범위를 보게 하려는 것이다.
 * Gemini 구조화 출력은 지원하지 않는 제약을 조용히 빼거나 지키지 않을 수 있어 `maxLength`를 강제 장치로 믿지 않는다.
 * 실제로 모델에 닿는 힌트는 `description`의 범위 문구이고, 지키는지는 도메인 모델([com.santacut.domain.roast.model.RoastReview])이 검증한다.
 * 하한은 싣지 않는다(채우려고 늘여 쓰지 않게).
 */
object RoastResponseSchema {
    private const val STRING = """{"type":"string"}"""

    /** 스키마 JSON 문자열. 짝은 5개, 줄거리는 3줄이다. */
    val JSON: String = schema()

    private fun schema(): String {
        val pair = """{"type":"object","properties":{"fact_key":$STRING,"roast":${text(RoastTextKind.ROAST)},""" +
            """"flip":${text(RoastTextKind.FLIP)}},"required":["fact_key","roast","flip"]}"""
        val storyLine = """{"type":"object","properties":{"act":{"type":"integer","minimum":1,"maximum":3},""" +
            """"line":${text(RoastTextKind.STORY)}},"required":["act","line"]}"""
        return """{"type":"object","properties":{"title":${text(RoastTextKind.TITLE)},"verdict":${text(RoastTextKind.VERDICT)},""" +
            """"verdict_mild":${text(RoastTextKind.VERDICT_MILD)},"pairs":{"type":"array","items":$pair,"minItems":5,"maxItems":5},""" +
            """"letter":${text(RoastTextKind.LETTER)},"story":{"type":"array","items":$storyLine,"minItems":3,"maxItems":3}},""" +
            """"required":["title","verdict","verdict_mild","pairs","letter","story"]}"""
    }

    private fun text(kind: RoastTextKind): String =
        """{"type":"string","maxLength":${kind.maxLength},"description":"공백과 문장부호를 포함해 ${kind.minLength}~${kind.maxLength}자"}"""
}
