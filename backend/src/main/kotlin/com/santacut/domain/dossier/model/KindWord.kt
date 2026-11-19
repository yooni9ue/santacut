package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoreDimension

/**
 * 따뜻한 말 하나(kind_words.json). 판결 사유와 공로의 증거 횟수로 쓴다.
 *
 * @property dimension 이 말이 증거가 되는 채점 항목 key([ScoreDimension.key])
 */
data class KindWord(
    val key: String,
    val label: String,
    val dimension: String,
    val forms: TextForms,
) {
    fun violations(): List<String> = listOfNotNull(
        "kind_words.$key: 모르는 dimension $dimension".takeIf { ScoreDimension.ofKey(dimension) == null },
        "kind_words.$key: label이 비었다".takeIf { label.isBlank() },
        "kind_words.$key: forms가 비었거나 빈 표면형이 있다".takeIf { forms.isMalformed() },
    )

    fun belongsTo(scoreDimension: ScoreDimension): Boolean = dimension == scoreDimension.key
}
