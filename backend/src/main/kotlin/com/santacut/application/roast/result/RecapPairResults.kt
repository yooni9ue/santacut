package com.santacut.application.roast.result

import com.santacut.domain.roast.model.RoastPairs

/** 리캡 팩폭 카드 목록. 저장한 짝의 앞 세 개다. */
data class RecapPairResults(val values: List<RecapPairResult>) {
    companion object {
        /** 화면에 싣는 팩폭 카드 수. */
        const val SHOWN = 3

        fun from(pairs: RoastPairs): RecapPairResults = RecapPairResults(pairs.toList().take(SHOWN).map(RecapPairResult::from))
    }
}
