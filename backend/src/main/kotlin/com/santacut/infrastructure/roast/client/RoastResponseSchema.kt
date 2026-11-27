package com.santacut.infrastructure.roast.client

import com.santacut.domain.roast.model.RoastTextKind

/**
 * 팩폭 작가 응답의 JSON 스키마(작가 프롬프트 5판, 리캡 덱). Gemini 구조화 출력으로 보내 응답 형태를 모델 쪽에서 강제한다.
 * 판정 한 줄(verdict_line), 근거 카드 목록(evidence), 결론(conclusion)을 모두 필수로 둔다. line_id와 hero_line_id는 정수이고,
 * 점수 깎인 말은 line_id를 null로 둘 수 있다. 문장 필드마다 서버 검증과 같은 글자 수 상한([RoastTextKind.maxLength])을 `maxLength`로,
 * 허용 범위를 `description`으로 싣는다. Gemini 구조화 출력은 지원하지 않는 제약을 조용히 빼거나 지키지 않을 수 있어
 * `maxLength`를 강제 장치로 믿지 않고, 지키는지는 도메인 모델([com.santacut.domain.roast.model.RoastReview])이 검증한다.
 * 근거 카드 수는 서버 슬롯 수가 입력마다 달라 스키마에 박지 않는다.
 */
object RoastResponseSchema {
    private const val STRING = """{"type":"string"}"""
    private const val NULLABLE_INTEGER = """{"type":"integer","nullable":true}"""
    private const val KIND = """{"type":"string","enum":["reason","cut","keep"]}"""

    /** 스키마 JSON 문자열. */
    val JSON: String = schema()

    private fun schema(): String {
        val evidence = """{"type":"object","properties":{"kind":$KIND,"line_id":$NULLABLE_INTEGER,"chip_key":$STRING,""" +
            """"label":${text(RoastTextKind.LABEL)},"line":${text(RoastTextKind.EVIDENCE_LINE)}},""" +
            """"required":["kind","line_id","chip_key","label","line"]}"""
        val conclusion = """{"type":"object","properties":{"title":${text(RoastTextKind.TITLE)},"hero_line_id":$NULLABLE_INTEGER,""" +
            """"hero_chip_key":$STRING,"final_line":${text(RoastTextKind.FINAL_LINE)}},""" +
            """"required":["title","hero_line_id","hero_chip_key","final_line"]}"""
        return """{"type":"object","properties":{"verdict_line":${text(RoastTextKind.VERDICT_LINE)},""" +
            """"evidence":{"type":"array","items":$evidence},"conclusion":$conclusion},""" +
            """"required":["verdict_line","evidence","conclusion"]}"""
    }

    private fun text(kind: RoastTextKind): String =
        """{"type":"string","maxLength":${kind.maxLength},"description":"공백과 문장부호를 포함해 ${kind.minLength}~${kind.maxLength}자"}"""
}
