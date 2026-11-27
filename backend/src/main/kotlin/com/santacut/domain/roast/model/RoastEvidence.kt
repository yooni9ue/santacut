package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 저장한 근거 카드 하나. 말풍선([quote])과 칩([chip])은 서버가 채우고, 라벨과 한 줄은 작가 문장이거나 서버 대체 문장이다.
 *
 * @property lineId 근거로 고른 본인 줄 번호. 칩만 쓴 통계 카드면 null이다.
 * @property chipKey 칩 후보 key(사실 key, 항목 점수는 dimension_로 시작)
 * @property chip 서버가 센 칩 값
 * @property quote 말풍선에 싣는 본인 말([QuoteText]). 이름을 가리고 20자 안으로 자른 것이다. 통계 카드면 null이다.
 * @property label 상 이름 꼴 라벨(8자 이하)
 * @property line 한 줄(20자 이하, 숫자 없음)
 * @property source 라벨과 한 줄의 출처
 * @property dimension 칩 방향이 가리키는 항목 점수 key([com.santacut.domain.analysis.model.ScoreDimension.key]). 근거 카드 영역 태그에 쓴다.
 * 이어 볼 항목이 없는 칩(중립·어느 쪽으로도 읽히는 입버릇)이나 이 필드 전에 저장한 결과면 null이다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class RoastEvidence(
    val kind: EvidenceKind,
    @JsonProperty("line_id") val lineId: Int?,
    @JsonProperty("chip_key") val chipKey: String,
    val chip: RoastChip?,
    val quote: String?,
    val label: String,
    val line: String,
    val source: RoastSource,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) val dimension: String? = null,
) {
    /** 대체 문장이면 1, 작가 문장이면 0. */
    fun fallbackCount(): Int = if (source == RoastSource.FALLBACK) 1 else 0
}
