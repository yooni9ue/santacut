package com.santacut.domain.analysis.model

/**
 * 채점 항목마다 둔 대표 낱말 묶음. 서버가 본인 메시지에서 항목별로 낱말이 든 메시지를 세어 AI에 신호로 넘기고,
 * 표본을 고를 때 낱말이 든 메시지를 먼저 뽑는다. 낱말은 일부러 적게 두고, 활용형(고마워·고마웠어)도 걸리도록 어간으로 적는다.
 * 낱말 하나가 메시지 어디에든 있으면 그 항목으로 센다. 낱말 바로 앞에 `안`·`못`이 있거나 낱말 뒤 몇 글자 안에
 * `-지 않`·`-지 못`·`-긴 뭐가`·`-긴 무슨`이나 띄어 쓴 `안`·`못`이 붙으면("안 괜찮아", "고맙긴 뭐가", "같이 못 가") 부정 낱말로 따로 센다.
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
    /** 낱말 바로 앞의 `안`·`못`, 또는 낱말 어간 뒤 두 글자(예: 걱정"하"지 않아) 안에 붙은 부정·반어. */
    private val negatedKeywords = Regex(
        "(?<![가-힣])(?:안|못) ?(?:$keywordPattern)" +
            "|(?:$keywordPattern)[가-힣]{0,2}(?: ?(?:지 ?않|지 ?못|긴 ?뭐|긴 ?무슨)| (?:안|못)(?![가-힣]))",
    )

    fun isMentionedIn(text: String): Boolean = keywords.containsMatchIn(text)

    /** 항목 낱말에 부정·반어가 붙어 있는지. */
    fun isNegatedIn(text: String): Boolean = negatedKeywords.containsMatchIn(text)
}
