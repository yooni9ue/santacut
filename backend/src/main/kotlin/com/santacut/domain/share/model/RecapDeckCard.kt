package com.santacut.domain.share.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.santacut.domain.global.model.SeoulTime
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * 공유한 리캡 카드 한 장. 종류([kind])마다 쓰는 필드만 채우고 나머지는 null로 두어 JSON에서 뺀다.
 * 농담을 끈 문구(mild)는 담지 않는다. 공개 페이지는 늘 기본(농담) 문구를 보여 준다.
 *
 * @property scoreText 모든 카드 상단의 총점 표시("73.73")
 * @property title 판정 카드의 칭호
 * @property verdict 판정 카드의 판결 한 줄
 * @property stamp 판정 카드의 도장 문구
 * @property grade 판정 카드의 등급 이름
 * @property cutDistance 판정 카드의 컷 거리 표시("+8.73")
 * @property rank 판정 카드의 전국 순위. 표본이 모자라면 null이다.
 * @property deliveryEta 판정 카드의 선물 도착 예정 시각(UTC, ISO-8601). 석탄이거나 순위 계산 전이면 null이다.
 * @property chipLabel 팩폭 카드의 서버 숫자 칩 이름. 사실 없이 쓴 대체 짝이면 null이다.
 * @property acts 줄거리 카드의 막 목록
 * @property bars 항목 막대 카드의 막대 4개
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
data class RecapDeckCard(
    val index: Int,
    val kind: RecapDeckKind,
    val scoreText: String,
    val title: String? = null,
    val verdict: String? = null,
    val stamp: String? = null,
    val grade: String? = null,
    val gradeCode: String? = null,
    val cut: Int? = null,
    val cutDistance: String? = null,
    val passed: Boolean? = null,
    val rank: Int? = null,
    val rankingPending: Boolean? = null,
    val deliveryEta: String? = null,
    val chipLabel: String? = null,
    val chipValue: String? = null,
    val roast: String? = null,
    val flip: String? = null,
    val letter: String? = null,
    val acts: RecapDeckActs? = null,
    val archetypeCode: String? = null,
    val archetypeName: String? = null,
    val bars: RecapDeckBars? = null,
) {
    /** 서울 날짜로 표시한 선물 도착 예정일. 남이 보는 화면이라 시각은 뺀다. 없으면 null이다. */
    fun displayDeliveryEta(): String? = deliveryEta?.let { Instant.parse(it).atZone(SeoulTime.ZONE).format(SEOUL_DATE_FORMAT) }

    companion object {
        private val SEOUL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
    }
}
