package com.santacut.domain.analysis.model

import java.text.Normalizer

/**
 * 참가자가 고른 본인 이름. 앞뒤 공백을 떼고 NFC로 정규화한 값이 메시지 화자 이름과 완전히 같아야 본인 메시지다.
 * 브라우저가 화자 목록을 보여 줄 때도 같은 규칙으로 비교한다.
 */
class SpeakerName private constructor(private val value: String) {
    fun matches(speaker: String): Boolean = value == normalize(speaker)

    companion object {
        fun of(raw: String): SpeakerName = SpeakerName(normalize(raw))

        private fun normalize(raw: String): String = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC)
    }
}
