package com.santacut.domain.analysis.model

/**
 * 채점 항목마다 둔 대표 낱말 묶음. 서버가 본인 메시지에서 항목별로 낱말이 든 메시지를 세어 AI에 신호로 넘기고,
 * 표본을 고를 때 낱말이 든 메시지를 먼저 뽑는다. 낱말은 일부러 적게 두고, 활용형(고마워·고마웠어)도 걸리도록 어간으로 적는다.
 * 낱말 하나가 메시지 어디에든 있으면 그 항목으로 센다.
 *
 * @property dimension 이 낱말 묶음이 뒷받침하는 채점 항목
 */
enum class ScoreTheme(val dimension: ScoreDimension, keywordPattern: String) {
    CARE(ScoreDimension.CARE_EMPATHY, "괜찮|걱정|힘들|조심|아프"),
    CHEER(ScoreDimension.POSITIVITY_SUPPORT, "고마|고맙|좋아|사랑|응원|화이팅|최고"),
    RESPECT(ScoreDimension.RESPECTFUL_COMMUNICATION, "미안|죄송|감사|부탁"),
    TOGETHER(ScoreDimension.COOPERATION_SHARING, "약속|만나|만날|같이|도와|나눠"),
    ;

    private val keywords = Regex(keywordPattern)

    fun isMentionedIn(text: String): Boolean = keywords.containsMatchIn(text)
}
