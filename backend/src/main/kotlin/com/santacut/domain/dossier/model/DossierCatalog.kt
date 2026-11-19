package com.santacut.domain.dossier.model

/**
 * 조서를 세고 쓰는 데 필요한 리소스 묶음(resources/dossier 아래 JSON 일곱 개). 앱이 뜰 때 한 번 읽어 검사하고, 위반이 있으면 앱을 띄우지 않는다.
 *
 * @property bleep 일반 욕설 표면형. 합산 횟수만 세고 낱말은 남기지 않는다.
 */
class DossierCatalog(
    val phrases: PhraseEntries,
    val topics: TopicEntries,
    val kinds: KindWords,
    val bleep: TextForms,
    val heavy: HeavyWords,
    val metrics: MetricSpecs,
    val copy: DossierCopy,
) {
    /** 모든 리소스의 규칙 위반 설명. 비어 있어야 앱을 띄운다. */
    fun violations(): List<String> =
        phrases.violations() + topics.violations() + kinds.violations() +
            listOfNotNull("bleep: forms가 비었거나 빈 표면형이 있다".takeIf { bleep.isMalformed() }) +
            heavy.violations() + metrics.violations(copy.extremes) + copy.violations()
}
