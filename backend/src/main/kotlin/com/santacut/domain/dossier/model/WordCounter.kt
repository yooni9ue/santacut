package com.santacut.domain.dossier.model

/**
 * 본인 글 메시지의 사전 낱말을 쌓는다. 입버릇, 따뜻한 말, 막별 주제, 욕설, 위기 표현을 메시지마다 세어 더한다.
 * 본문은 줄이기만 하고 길이를 자르지 않은 정규화 본문([com.santacut.domain.analysis.model.ScoringMessage.normalizedFullText])이다.
 */
class WordCounter(private val catalog: DossierCatalog) {
    private val phrases = catalog.phrases.emptyCounts()
    private val kinds = catalog.kinds.emptyCounts()
    private val topics = catalog.topics.emptyCounts()
    private var bleep = 0
    private var heavy = 0

    /** [act] 막에 보낸 정규화 본문 [text]의 낱말을 더한다. */
    fun add(text: String, act: DossierAct) {
        catalog.phrases.addCountsIn(text, phrases)
        catalog.kinds.addCountsIn(text, kinds)
        catalog.topics.addCountsIn(text, act, topics)
        bleep += catalog.bleep.countIn(text)
        heavy += catalog.heavy.forms.countIn(text)
    }

    /** 쌓은 낱말 집계. 특이도의 분모는 글 메시지 수 [textCount]다. */
    fun stats(textCount: Int): WordStats {
        val hits = catalog.phrases.hitsOf(phrases)
        return WordStats(hits.frequent(), hits.distinctive(textCount), catalog.kinds.countsOf(kinds), catalog.topics.countsOf(topics), bleep)
    }

    /** 위기 표현 횟수로 정한 톤. 횟수는 남기지 않는다. */
    fun tone(textCount: Int): DossierTone = catalog.heavy.toneOf(heavy, textCount)
}
