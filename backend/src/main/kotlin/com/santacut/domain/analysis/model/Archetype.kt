package com.santacut.domain.analysis.model

import java.math.BigDecimal

/**
 * 재미용 산타 유형 10가지. 가장 높은 항목 × 장난기(높음·낮음)로 8가지, 위 두 항목 차이가 작으면 균형형 2가지다.
 * 위 두 항목 차이가 경계 근처면 수축 전 평균으로 가른다([of]).
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
    RUDOLPH_DRIVER("맨 앞에서 끄는 루돌프"),
    WORKSHOP_ELF("일 벌이는 공방 엘프"),
    SANTA_CANDIDATE("산타 후보생"),
    GRINCH_APPRENTICE("그린치 견습생"),
    ;

    companion object {
        /** 위 두 항목 점수 차이가 이보다 작으면 수축 노이즈와 상관없이 균형형이다. */
        val CLEAR_BALANCED_GAP = BigDecimal(3)

        /** 위 두 항목 점수 차이가 이 이상이면 수축 노이즈와 상관없이 가장 높은 항목의 유형이다. */
        val CLEAR_LEAD_GAP = BigDecimal(7)

        /** 수축 전 평균이 없는 예전 평가는 띠 안에서 저장한 항목 점수의 차이가 이보다 작으면 균형형이다. */
        val BALANCED_GAP = BigDecimal(5)

        /**
         * 띠 안에서 수축 전 평균의 위 두 항목 차이가 이보다 작으면 균형형이다.
         * 모델 점수는 5점 단위에 몰려 차이도 5·7·10점이 잦다. 5점에 경계를 두면 흔한 값이 경계에 걸리므로 5와 7 사이에 둔다.
         * 수축 전 차이 5점은 수축 뒤 약 4.4점이라 예전 규칙(5점 미만 균형형)에서도 균형형이었다.
         */
        val RAW_BALANCED_GAP = BigDecimal(6)

        /** 균형형에서 항목 평균이 이 이상이면 산타 후보생, 아래면 그린치 견습생이다. */
        val BALANCED_HIGH_AVERAGE = BigDecimal(60)

        /**
         * 항목 점수 [components]와 장난기 [playfulness]로 유형을 고른다.
         * 위 두 항목 차이가 애매한 띠([CLEAR_BALANCED_GAP] 이상 [CLEAR_LEAD_GAP] 미만)면 수축 전 평균 [rawMeans]의 차이와 순서로 가른다.
         * 수축 무게는 모델이 답한 기회 수에 따라 흔들리므로, 같은 대화가 기회 수 차이만으로 유형이 바뀌지 않게 하려는 것이다.
         * [rawMeans]가 없는 예전 평가는 [components]로 가른다.
         * 장난기가 [playfulnessSplit](참가자 중앙값)보다 크면 장난기 높음이다. 장난기를 모르는 예전 평가는 낮음으로 본다.
         */
        fun of(components: ScoreComponents, playfulness: Int?, playfulnessSplit: Int, rawMeans: ScoreComponents? = null): Archetype {
            val gap = components.leadGap()
            val average = components.average()
            val playful = (playfulness ?: playfulnessSplit) > playfulnessSplit
            if (gap < CLEAR_BALANCED_GAP) return balanced(average)
            if (gap >= CLEAR_LEAD_GAP || rawMeans == null) return byLead(components, BALANCED_GAP, average, playful)
            return byLead(rawMeans, RAW_BALANCED_GAP, average, playful)
        }

        /** [basis]의 위 두 항목 차이가 [balancedGap]보다 작으면 평균 [average]에 따른 균형형, 아니면 [basis]에서 가장 높은 항목의 유형이다. */
        private fun byLead(basis: ScoreComponents, balancedGap: BigDecimal, average: BigDecimal, playful: Boolean): Archetype {
            if (basis.leadGap() < balancedGap) return balanced(average)
            return byDimension(basis.rankedDimensions().first(), playful)
        }

        /** 상수 이름 [name]의 유형. 모르는 이름이나 null이면 null이다. */
        fun ofName(name: String?): Archetype? = name?.let { code -> runCatching { valueOf(code) }.getOrNull() }

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
