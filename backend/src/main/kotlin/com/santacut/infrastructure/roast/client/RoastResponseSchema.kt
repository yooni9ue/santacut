package com.santacut.infrastructure.roast.client

/**
 * 팩폭 작가 응답의 JSON 스키마. Gemini 구조화 출력으로 보내 응답 형태를 모델 쪽에서 강제한다.
 * 모든 키를 필수로 둔다. 글자 수와 개수는 모델이 지키도록 돕는 힌트일 뿐이고, 검증은 도메인 모델([com.santacut.domain.roast.model.RoastReview])이 한다.
 */
object RoastResponseSchema {
    private const val STRING = """{"type":"string"}"""
    private const val PAIR =
        """{"type":"object","properties":{"fact_key":$STRING,"roast":$STRING,"flip":$STRING},"required":["fact_key","roast","flip"]}"""
    private const val STORY_LINE =
        """{"type":"object","properties":{"act":{"type":"integer","minimum":1,"maximum":3},"line":$STRING},"required":["act","line"]}"""

    /** 스키마 JSON 문자열. 짝은 5개, 줄거리는 3줄이다. */
    const val JSON: String =
        """{"type":"object","properties":{"title":$STRING,"verdict":$STRING,"verdict_mild":$STRING,""" +
            """"pairs":{"type":"array","items":$PAIR,"minItems":5,"maxItems":5},"letter":$STRING,""" +
            """"story":{"type":"array","items":$STORY_LINE,"minItems":3,"maxItems":3}},""" +
            """"required":["title","verdict","verdict_mild","pairs","letter","story"]}"""
}
