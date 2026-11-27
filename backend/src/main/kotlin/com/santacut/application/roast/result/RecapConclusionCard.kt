package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.roast.model.RoastConclusion
import com.santacut.domain.share.model.RecapDeckCard
import com.santacut.domain.share.model.RecapDeckKind

/**
 * 결론 카드. 덱에서 가장 큰 글자인 구간 이름, 칭호·산타 유형, 대표 말풍선과 칩, 마지막 한 줄, 점수·컷 거리 칩, 친구 소환 고정문을 싣는다.
 * 공유할 때 기본으로 고르는 카드다.
 *
 * @property verdict 판정 큰 글자(착한 아이, 나쁜 아이)
 * @property grade 구간 이름(가장 큰 글자)
 * @property gradeCode 구간 코드
 * @property bandTag 해시태그 낱말(예: 걸쳐서통과)
 * @property bandPosition 미니 컷 표에서 내 칸의 자리(0부터)
 * @property title 밈형 칭호(8자 이하)
 * @property archetypeCode 산타 유형 코드
 * @property archetypeName 산타 유형 이름
 * @property archetypeSharePercent 같은 유형 비율(%). 참가자가 모자라면 null이다.
 * @property heroQuote 대표 본인 말풍선. 없으면 null이다.
 * @property heroChipText 대표 칩 글자. 없으면 null이다.
 * @property finalLine 마지막 한 줄
 * @property cutDistance 컷 거리 표시
 * @property friendCall 친구 소환 고정문(서버 상수)
 */
data class RecapConclusionCard(
    override val index: Int,
    val verdict: String,
    val passed: Boolean,
    val grade: String,
    val gradeCode: String,
    val bandTag: String,
    val bandPosition: Int,
    val title: String,
    val archetypeCode: String,
    val archetypeName: String,
    val archetypeSharePercent: Int?,
    val heroQuote: String?,
    val heroChipText: String?,
    val finalLine: String,
    val cutDistance: String,
    val friendCall: String,
) : RecapCard {
    override val kind: RecapCardKind = RecapCardKind.CONCLUSION

    override fun at(index: Int): RecapCard = copy(index = index)

    override fun toDeckCard(top: RecapTopResult, scoreText: String): RecapDeckCard = RecapDeckCard(
        index, RecapDeckKind.CONCLUSION, scoreText, top.rank, top.topPercent, verdict = verdict, passed = passed, grade = grade,
        cutDistance = cutDistance, title = title, archetypeName = archetypeName, bandTag = bandTag, heroQuote = heroQuote,
        heroChipText = heroChipText, finalLine = finalLine, friendCall = friendCall,
    )

    companion object {
        /**
         * 판정 [gift]와 꾸밈 값 [profile](산타 유형), 저장한 결론 문장 [conclusion]으로 만든다. 석탄 덱은 유형을 석탄 덱용([Archetype.forCoal])으로 바꾸고,
         * 바뀌었으면 같은 유형 비율은 원래 유형의 것이라 싣지 않는다.
         */
        fun of(gift: GiftVerdict, profile: ResultProfile, conclusion: RoastConclusion): RecapConclusionCard {
            val grade = gift.grade()
            val archetype = if (gift.passed) profile.archetype else profile.archetype.forCoal()
            val sharePercent = profile.archetypeSharePercent.takeIf { archetype == profile.archetype }
            return RecapConclusionCard(
                0, gift.childLabel(), gift.passed, grade.label, grade.name, grade.tag, grade.ordinal, conclusion.title.text,
                archetype.name, archetype.koreanName, sharePercent, conclusion.heroQuote,
                conclusion.heroChip?.text(), conclusion.finalLine.text, gift.cutDistanceText(), gift.friendCall(),
            )
        }
    }
}
