package com.santacut.application.roast.result

import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind

/**
 * 리캡 덱 안 카드 한 장의 자리. 카드 값은 [RecapResult]에 있고, 여기에는 순서([index])와 종류([kind])만 둔다.
 * 로그인 사용자 리캡 응답과 공유 링크의 카드 스냅숏이 [RecapCards] 목록 하나로 순서를 맞춘다.
 *
 * @property pair 팩폭 카드([RecapCardKind.ROAST])가 싣는 짝. 다른 종류는 null이다.
 */
data class RecapCard(val index: Int, val kind: RecapCardKind, val pair: RecapPairResult? = null) {
    /** 공유 링크에 저장할 카드 값. [result]에서 이 종류가 쓰는 공유 가능한 필드만 옮긴다. 농담을 끈 문구는 옮기지 않는다. */
    fun toDeckCard(result: RecapResult): RecapDeckCard = when (kind) {
        RecapCardKind.VERDICT -> result.verdict.let { verdict ->
            deckCard(RecapDeckKind.VERDICT, result).copy(
                title = verdict.title, verdict = verdict.verdict, stamp = verdict.stamp, grade = verdict.grade,
                gradeCode = verdict.gradeCode, cut = verdict.cut, cutDistance = verdict.cutDistance, passed = verdict.passed,
                rank = verdict.rank, rankingPending = verdict.rankingPending, deliveryEta = verdict.deliveryEta?.toString(),
            )
        }
        RecapCardKind.ROAST -> requireNotNull(pair).let { pair ->
            deckCard(RecapDeckKind.ROAST, result).copy(chipLabel = pair.chipLabel, chipValue = pair.chipValue, roast = pair.roast, flip = pair.flip)
        }
        RecapCardKind.LETTER -> deckCard(RecapDeckKind.LETTER, result).copy(letter = result.letter)
        RecapCardKind.STORY ->
            deckCard(RecapDeckKind.STORY, result).copy(acts = result.story.toDeckActs())
        RecapCardKind.ARCHETYPE ->
            deckCard(RecapDeckKind.ARCHETYPE, result).copy(archetypeCode = result.archetypeCode, archetypeName = result.archetypeName)
        RecapCardKind.DIMENSIONS ->
            deckCard(RecapDeckKind.DIMENSIONS, result).copy(bars = result.dimensions.toDeckBars())
    }

    private fun deckCard(deckKind: RecapDeckKind, result: RecapResult): RecapDeckCard = RecapDeckCard(index, deckKind, result.scoreText)
}
