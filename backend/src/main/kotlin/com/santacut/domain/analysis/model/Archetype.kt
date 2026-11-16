package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 재미용 산타 유형 10가지. 가장 높은 항목 × 장난기(높음·낮음)로 8가지, 위 두 항목 차이가 작으면 균형형 2가지다.
 * 순위에는 쓰지 않는다.
 *
 * @property koreanName 화면에 보여 줄 이름
 */
enum class Archetype(val koreanName: String) {
    HOT_COCOA_COURIER("핫초코 배달부"),
    FIREPLACE_KEEPER("벽난로 담당"),
    CAROL_LEADER("캐럴 떼창 리더"),
    TREE_TOP_STAR("트리 꼭대기 별"),
    MANNER_SNOWMAN("매너 눈사람"),
    GIFT_WRAPPER("선물 포장 장인"),
    RUDOLPH_DRIVER("썰매 모는 루돌프"),
    WORKSHOP_ELF("산타 공방 엘프"),
    SANTA_CANDIDATE("산타 후보생"),
    GRINCH_APPRENTICE("그린치 견습생"),
    ;

    companion object {
        /** 위 두 항목 점수 차이가 이보다 작으면 균형형이다. */
        val BALANCED_GAP = BigDecimal(5)

        /** 균형형에서 항목 평균이 이 이상이면 산타 후보생, 아래면 그린치 견습생이다. */
        val BALANCED_HIGH_AVERAGE = BigDecimal(60)

        /**
         * 항목 점수 [components]와 장난기 [playfulness]로 유형을 고른다.
         * 장난기가 [playfulnessSplit](참가자 중앙값)보다 크면 장난기 높음이다. 장난기를 모르는 예전 평가는 낮음으로 본다.
         */
        fun of(components: ScoreComponents, playfulness: Int?, playfulnessSplit: Int): Archetype {
            val (top, runnerUp) = components.rankedDimensions()
            val gap = components.scoreOf(top) - components.scoreOf(runnerUp)
            if (gap < BALANCED_GAP) return balanced(components.average())
            return byDimension(top, (playfulness ?: playfulnessSplit) > playfulnessSplit)
        }

        private fun balanced(average: BigDecimal): Archetype =
            if (average >= BALANCED_HIGH_AVERAGE) SANTA_CANDIDATE else GRINCH_APPRENTICE

        private fun byDimension(top: ScoreDimension, playful: Boolean): Archetype = when (top) {
            ScoreDimension.CARE_EMPATHY -> if (playful) HOT_COCOA_COURIER else FIREPLACE_KEEPER
            ScoreDimension.POSITIVITY_SUPPORT -> if (playful) CAROL_LEADER else TREE_TOP_STAR
            ScoreDimension.RESPECTFUL_COMMUNICATION -> if (playful) MANNER_SNOWMAN else GIFT_WRAPPER
            ScoreDimension.COOPERATION_SHARING -> if (playful) RUDOLPH_DRIVER else WORKSHOP_ELF
        }
    }
}
