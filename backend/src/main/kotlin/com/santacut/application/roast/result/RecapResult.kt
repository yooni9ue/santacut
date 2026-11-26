package com.santacut.application.roast.result

import com.santacut.application.ranking.result.DimensionBarResults
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.roast.model.RoastContent
import java.math.BigDecimal

/**
 * 리캡 카드 덱에 실을 값. 최신 평가와 그 평가의 순위 스냅숏, 꾸밈 값, 저장한 팩폭 문장으로 만든다.
 *
 * @property skin 카드 스킨(GIFT, COAL)
 * @property score 총점(점, 소수 둘째 자리)
 * @property scoreText 총점 표시("73.73")
 * @property shareText 서버가 고정한 공유 문구. 등급마다 다르고 총점 표시가 들어간다.
 * @property shareTextMild 농담을 끈 사람의 공유 문구. 점수와 놀림 없이 통과·탈락 문구 하나씩이다.
 * @property archetypeCode 산타 유형 코드
 * @property archetypeName 산타 유형 이름
 */
data class RecapResult(
    val assessmentId: String,
    val skin: String,
    val score: BigDecimal,
    val scoreText: String,
    val shareText: String,
    val shareTextMild: String,
    val verdict: RecapVerdictResult,
    val pairs: RecapPairResults,
    val letter: String,
    val story: RecapStoryResults,
    val archetypeCode: String,
    val archetypeName: String,
    val dimensions: DimensionBarResults,
) {
    /**
     * 카드 덱 순서. 판정, 팩폭 세 장, 쪽지, 줄거리(있을 때만), 유형, 항목 막대 순으로 고정이고 index는 0부터 이 순서다.
     * 로그인 사용자 리캡 응답이 이 목록을 따른다.
     */
    fun cards(): RecapCards = RecapCards.of(this)

    companion object {
        private const val HUNDREDTHS_SCALE = 2

        /**
         * 최신 평가 [latest]와 그 평가의 순위 스냅숏 [snapshot](아직 순위에 오르지 않았으면 null), 꾸밈 값 [profile], 팩폭 문장 [content]로 만든다.
         * 순위는 상위 비율을 보여 줄 만큼 순위 계산 참가자가 모였을 때만 싣는다(표본 기준). 그 전이거나 계산 대기면 null이다.
         */
        fun of(latest: Assessment, snapshot: ResultSnapshot?, profile: ResultProfile, content: RoastContent): RecapResult {
            val gift = GiftVerdict(latest.scoreHundredths)
            val rank = snapshot?.rank?.takeIf { profile.topPercent != null }
            return RecapResult(
                latest.id, gift.skin(), BigDecimal.valueOf(latest.scoreHundredths.toLong(), HUNDREDTHS_SCALE), gift.scoreText(), gift.shareText(), gift.shareTextMild(),
                RecapVerdictResult.of(gift, rank, snapshot == null, snapshot?.eta, content),
                RecapPairResults.from(content.pairs), content.letter.text, RecapStoryResults.from(content.story),
                profile.archetype.name, profile.archetype.koreanName, DimensionBarResults.from(profile.dimensionScores),
            )
        }
    }
}
