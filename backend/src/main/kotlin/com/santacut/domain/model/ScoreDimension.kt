package com.santacut.domain.model

/**
 * 점수 평가 항목과 가중치(%). [key]는 AI 응답 JSON의 키다.
 *
 * 가중치 합은 [TOTAL_WEIGHT](100%)다. 그래서 항목 점수에 가중치(%)를 곱해 더하면
 * 곱셈 자체가 100분의 1점 단위 합계가 된다. 항목 점수 상한(`ScoreComponents.MAX_DIMENSION_SCORE`, 100점)과는
 * 숫자만 같을 뿐 뜻이 다르다. 하나는 백분율 합계, 하나는 항목 점수 척도다.
 */
enum class ScoreDimension(val key: String, val weight: Int) {
    CARE_EMPATHY("care_empathy", 30),
    POSITIVITY_SUPPORT("positivity_support", 25),
    RESPECTFUL_COMMUNICATION("respectful_communication", 25),
    COOPERATION_SHARING("cooperation_sharing", 20),
    ;

    companion object {
        /** 모든 항목 가중치(%)의 합. 이 값이 100이어야 가중 합계가 100분의 1점 단위가 된다. */
        const val TOTAL_WEIGHT = 100
    }
}
