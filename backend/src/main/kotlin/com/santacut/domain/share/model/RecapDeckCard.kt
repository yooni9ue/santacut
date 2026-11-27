package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonInclude
import com.santacut.domain.global.model.SeoulTime
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 공유한 리캡 카드 한 장(저장 형식 2판). 종류([kind])마다 쓰는 필드만 채우고 나머지는 null로 두어 JSON에서 뺀다.
 * 모든 카드에 상단 바 값([rank], [topPercent])과 총점 표시([scoreText])가 있다.
 *
 * @property rank 전국 순위. 공유할 때 순위가 없었으면 null이다.
 * @property topPercent 상위 비율(%). 순위가 없으면 null이다.
 * @property verdict 판정·결론 카드의 큰 글자(착한 아이, 나쁜 아이)
 * @property grade 판정·컷 표·결론 카드의 구간 이름
 * @property cutDistance 판정·결론 카드의 컷 거리 표시("+8.73")
 * @property verdictLine 판정 카드의 한 줄
 * @property deliveryEta 판정 카드의 선물 도착 예정 시각(UTC, ISO-8601). 석탄이거나 순위 계산 전이면 null이다.
 * @property bandDetail 컷 표 카드의 구간 고정 문구
 * @property bands 컷 표 카드의 여섯 칸
 * @property ordinal 근거 카드의 같은 종류 안 순번(1부터)
 * @property quote 근거 카드의 본인 말풍선(이름 가림). 통계 카드면 null이다.
 * @property chipText 근거 카드의 서버 칩 글자
 * @property label 근거 카드의 상 이름 라벨
 * @property line 근거 카드의 한 줄
 * @property dimTag 근거 카드의 영역 태그("협력 +")
 * @property title 결론 카드의 칭호
 * @property archetypeName 결론 카드의 산타 유형 이름
 * @property bandTag 결론 카드의 해시태그 낱말
 * @property heroQuote 결론 카드의 대표 말풍선
 * @property heroChipText 결론 카드의 대표 칩 글자
 * @property finalLine 결론 카드의 마지막 한 줄
 * @property friendCall 결론 카드의 친구 소환 고정문
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
data class RecapDeckCard(
    val index: Int,
    val kind: RecapDeckKind,
    val scoreText: String,
    val rank: Int? = null,
    val topPercent: Int? = null,
    val verdict: String? = null,
    val passed: Boolean? = null,
    val grade: String? = null,
    val cutDistance: String? = null,
    val verdictLine: String? = null,
    val deliveryEta: String? = null,
    val bandDetail: String? = null,
    val bands: RecapDeckBands? = null,
    val ordinal: Int? = null,
    val quote: String? = null,
    val chipText: String? = null,
    val label: String? = null,
    val line: String? = null,
    val dimTag: String? = null,
    val title: String? = null,
    val archetypeName: String? = null,
    val bandTag: String? = null,
    val heroQuote: String? = null,
    val heroChipText: String? = null,
    val finalLine: String? = null,
    val friendCall: String? = null,
) {
    /** 서울 날짜로 표시한 선물 도착 예정일. 남이 보는 화면이라 시각은 뺀다. 없으면 null이다. */
    fun displayDeliveryEta(): String? = deliveryEta?.let { Instant.parse(it).atZone(SeoulTime.ZONE).format(SEOUL_DATE_FORMAT) }

    companion object {
        private val SEOUL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    }
}
