package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 위기·무거운 표현(heavy.json). 횟수는 저장하지 않고 톤을 담백([DossierTone.MILD])으로 낮출지만 정한다.
 *
 * @property thresholdPer1000 본인 메시지 1,000개당 이 횟수 이상이면 담백 톤이다.
 */
@JsonIgnoreProperties("version")
data class HeavyWords(
    val forms: TextForms,
    @JsonProperty("threshold_per_1000") val thresholdPer1000: Double,
) {
    fun violations(): List<String> = listOfNotNull(
        "heavy: forms가 비었거나 빈 표면형이 있다".takeIf { forms.isMalformed() },
        "heavy: threshold_per_1000은 0보다 커야 한다".takeIf { thresholdPer1000 <= 0 },
    )

    /** 본인 메시지 [messageCount]개를 이은 [text]의 톤. */
    fun toneOf(text: String, messageCount: Int): DossierTone {
        if (messageCount == 0) return DossierTone.NORMAL
        val perThousand = forms.countIn(text) * PER_THOUSAND / messageCount
        return if (perThousand >= thresholdPer1000) DossierTone.MILD else DossierTone.NORMAL
    }

    companion object {
        private const val PER_THOUSAND = 1000.0
    }
}
