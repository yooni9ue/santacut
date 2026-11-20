package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import tools.jackson.databind.JsonNode

/**
 * 놀림·뒤집기 짝 목록. 저장할 때는 검증을 통과한 짝(최대 [RoastSheet.MAX_PAIRS]개, 모자라면 대체 짝으로 [RoastSheet.MIN_PAIRS]개까지)이고,
 * 화면에는 앞의 세 짝을 쓴다. JSON에서는 배열 하나로 읽고 쓴다.
 */
class RoastPairs @JsonCreator(mode = JsonCreator.Mode.DELEGATING) constructor(
    private val values: List<RoastPair>,
) {
    @JsonValue
    fun toList(): List<RoastPair> = values.toList()

    fun size(): Int = values.size

    fun factKeys(): List<String> = values.map(RoastPair::factKey)

    /** 대체 짝 수. 로그에는 이 수만 남긴다. */
    fun fallbackCount(): Int = values.count { pair -> pair.source == RoastSource.FALLBACK }

    /** 짝마다의 검증. 문장 검증에 더해 사실 key가 입력 사실에 있는지, 앞 짝과 겹치지 않는지 본다. */
    fun checks(brief: RoastBrief, rules: RoastRules): List<RoastCheck> = values.mapIndexed { index, pair ->
        val duplicated = values.take(index).any { previous -> previous.factKey == pair.factKey }
        RoastCheck(
            fieldOf(index),
            listOfNotNull(
                "없는 사실 key(${pair.factKey})".takeUnless { brief.hasFact(pair.factKey) },
                "앞 짝과 같은 사실 key(${pair.factKey})".takeIf { duplicated },
            ) + TextProblems.of(pair.roast, RoastTextKind.ROAST, brief, rules).labeled(ROAST) +
                TextProblems.of(pair.flip, RoastTextKind.FLIP, brief, rules).labeled(FLIP),
        )
    }

    /** 검증을 통과한 짝만. */
    fun passing(checks: RoastChecks): RoastPairs = RoastPairs(values.filterIndexed { index, _ -> checks.passed(fieldOf(index)) })

    /** 지금 짝에 [other]의 짝을 사실 key가 겹치지 않게 더한다. [RoastSheet.MAX_PAIRS]개까지다. */
    fun plus(other: RoastPairs): RoastPairs =
        RoastPairs((values + other.values).distinctBy(RoastPair::factKey).take(RoastSheet.MAX_PAIRS))

    /**
     * 짝이 [RoastSheet.MIN_PAIRS]개보다 적으면 아직 쓰지 않은 사실의 대체 짝으로, 그래도 모자라면 일반 대체 짝으로 채운다.
     * 마지막에 사실마다의 서버 숫자 칩을 붙인다.
     */
    fun completedWith(facts: RoastFacts, fallback: RoastFallback): RoastPairs {
        val missing = (RoastSheet.MIN_PAIRS - values.size).coerceAtLeast(0)
        val byFact = facts.fallbackPairsExcept(factKeys(), fallback).take(missing)
        val filled = values + byFact + fallback.genericPairs.take(missing - byFact.size)
        return RoastPairs(filled.map { pair -> pair.copy(chip = facts.find(pair.factKey)?.chip) })
    }

    companion object {
        val EMPTY = RoastPairs(emptyList())
        const val FIELD = "pairs"
        private const val ROAST = "roast"
        private const val FLIP = "flip"
        private const val FACT_KEY_FIELD = "fact_key"

        fun fieldOf(index: Int): String = "$FIELD[$index]"

        /** 작가 응답의 pairs 배열. 배열이 아니면 빈 목록이다. 빠진 문자열은 빈 문자열로 읽어 검증에서 떨어지게 한다. */
        fun from(node: JsonNode): RoastPairs = RoastPairs(
            node.values().map { item ->
                RoastPair(RoastJson.textOf(item, FACT_KEY_FIELD), RoastJson.textOf(item, ROAST), RoastJson.textOf(item, FLIP))
            },
        )
    }
}
