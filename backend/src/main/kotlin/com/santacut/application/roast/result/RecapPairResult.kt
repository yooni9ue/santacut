package com.santacut.application.roast.result

import com.santacut.domain.roast.model.RoastPair

/**
 * 리캡 팩폭 카드 하나: 서버 숫자 칩, 놀림, 뒤집기.
 *
 * @property chipLabel 칩 이름. 사실 없이 쓴 대체 짝이면 null이다.
 * @property chipValue 칩 값(서버가 센 숫자). 사실 없이 쓴 대체 짝이면 null이다.
 */
data class RecapPairResult(val chipLabel: String?, val chipValue: String?, val roast: String, val flip: String) {
    companion object {
        fun from(pair: RoastPair): RecapPairResult = RecapPairResult(pair.chip?.label, pair.chip?.value, pair.roast, pair.flip)
    }
}
