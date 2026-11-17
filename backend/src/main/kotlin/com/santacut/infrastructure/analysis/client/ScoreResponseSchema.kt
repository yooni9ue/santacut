package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ScoreDimension

/**
 * 채점 응답(6판 형식)의 JSON 스키마. Gemini 구조화 출력(`responseJsonSchema`)으로 보내 응답 형태를 모델 쪽에서 강제한다.
 * 모든 키를 필수로 둔다. 근거가 부족한 답도 `sufficient=false`와 함께 나머지 키를 채워 오고, 읽는 쪽은 `sufficient`만 본다.
 * 범위(`minimum`·`maximum`)는 모델이 지키도록 돕는 힌트일 뿐이다. 범위 검증은 여전히 도메인 모델이 한다.
 */
object ScoreResponseSchema {
    private const val DIMENSION =
        """{"type":"object","properties":{"e":{"type":"array","items":{"type":"integer","minimum":1},"maxItems":2},""" +
            """"n":{"type":"integer","minimum":0,"maximum":2},"s":{"type":"integer","minimum":0,"maximum":100}},"required":["e","n","s"]}"""

    private val CARE = ScoreDimension.CARE_EMPATHY.key
    private val POSITIVITY = ScoreDimension.POSITIVITY_SUPPORT.key
    private val RESPECT = ScoreDimension.RESPECTFUL_COMMUNICATION.key
    private val COOPERATION = ScoreDimension.COOPERATION_SHARING.key

    /** 스키마 JSON 문자열. 항목 키 순서는 [ScoreDimension] 순서다. */
    val JSON: String =
        """{"type":"object","properties":{"sufficient":{"type":"boolean"},""" +
            """"$CARE":$DIMENSION,"$POSITIVITY":$DIMENSION,"$RESPECT":$DIMENSION,"$COOPERATION":$DIMENSION,""" +
            """"playfulness":{"type":"integer","minimum":0,"maximum":100}},""" +
            """"required":["sufficient","$CARE","$POSITIVITY","$RESPECT","$COOPERATION","playfulness"]}"""
}
