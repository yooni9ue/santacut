package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierStats
import tools.jackson.databind.ObjectMapper

/**
 * 팩폭 작가 호출 하나의 서버 조립 입력. 판정, 사실, 인용 후보, 화제 후보, 가린 본인 말풍선과 그 요청의 차단 목록을 함께 쥔다.
 * 차단 목록은 출력 검증에만 쓰고 저장하지 않는다.
 */
class RoastBrief(
    val verdict: GiftVerdict,
    val facts: RoastFacts,
    private val quotes: RoastQuotes,
    private val topics: RoastTopics,
    private val lines: RoastLines,
    private val blocked: BlockedWords,
) {
    /** 작가 입력. 다시 쓰기면 [rewrite]를 싣는다. */
    fun input(rewrite: RoastRewrite? = null): RoastInput =
        RoastInput(verdict.key(), verdict.margin().word, facts, quotes, topics, lines.toActs(), rewrite)

    /** 작가 사용자 메시지 본문(JSON 한 줄). 테스트·실측 도구에서도 이 함수로 입력을 만든다. */
    fun inputText(objectMapper: ObjectMapper, rewrite: RoastRewrite? = null): String = objectMapper.writeValueAsString(input(rewrite))

    fun hasFact(key: String): Boolean = facts.find(key) != null

    fun isQuotable(quote: String): Boolean = quote in quotes

    /** [text]가 본인 말풍선(가린 뒤)에서 [COPIED_LENGTH]자 이상 그대로 베낀 첫 구간. 따옴표 없이 옮겨 쓴 인용을 막는다. 없으면 null이다. */
    fun copiedIn(text: String): String? = lines.sharedRun(text, COPIED_LENGTH)

    /** [text]에 나온 차단 낱말(입력에서 가린 사람 이름·사는 곳 이름·영문). 없으면 null이다. */
    fun blockedIn(text: String): String? = blocked.foundIn(text)

    /** 작가에게 보내는 줄 수. 로그에는 이 수만 남긴다. */
    fun lineCount(): Int = lines.size()

    companion object {
        /** 막 하나에 보내는 본인 말풍선 글자 수 상한. 세 막 합이 약 6천 자(입력 약 4~5천 토큰)다. */
        const val CHARS_PER_ACT = 2_000

        /** 이만큼 이어진 글자가 본인 말풍선과 같으면 베낀 것으로 본다. */
        const val COPIED_LENGTH = 8

        /**
         * 메시지 [messages](본인과 상대, 시각순, 한국 시각)와 조서 집계 [stats], 선물 판정 [verdict]로 입력을 조립한다.
         * 본인 줄만 쓰고, 민감한 줄은 빼고, 사람 이름·사는 곳 이름·숫자·영문·링크는 가린다. 지명·가게·행사 이름은 그대로 둔다. [stats]가 없으면(조서 집계 실패) 사실·인용·화제 없이 보낸다.
         * 입버릇은 지금 사전으로 다시 읽은 집계를 쓴다. 테스트·실측 도구에서도 이 함수로 입력을 만든다.
         */
        fun compose(
            messages: List<ScoringMessage>,
            stats: DossierStats?,
            verdict: GiftVerdict,
            catalog: RoastCatalog,
            dossier: DossierCatalog,
        ): RoastBrief {
            val all = RoastLines.of(messages, catalog.rules)
            val refreshed = stats?.refreshedBy(dossier.phrases)
            return RoastBrief(
                verdict,
                refreshed?.let { counted -> RoastFacts.of(counted, dossier, catalog) } ?: RoastFacts.EMPTY,
                refreshed?.let { counted -> RoastQuotes.of(counted, catalog.rules) } ?: RoastQuotes.EMPTY,
                refreshed?.let { counted -> RoastTopics.of(counted, dossier.topics, catalog.rules.excludedTopics) } ?: RoastTopics.EMPTY,
                all.trimmed(CHARS_PER_ACT),
                all.blockedWords(),
            )
        }
    }
}
