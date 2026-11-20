package com.santacut.application.roast.result

import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.roast.model.RoastContent
import java.math.BigDecimal
import java.time.Instant

/**
 * 리캡 판정 카드 값. 숫자는 모두 서버가 정한다.
 *
 * @property cutDistance 컷 거리 표시("+8.73", "-3.20")
 * @property cutDistanceValue 컷 거리 숫자(점, 소수 둘째 자리)
 * @property stamp 도장 문구(등급마다 다르다)
 * @property stampMild 농담을 끈 도장 문구. 탈락 세 등급은 "내년 명단 예약"이다.
 * @property grade 등급 이름(굴뚝 프리패스, 썰매 탑승 완료, 굴뚝에 걸쳐서 통과, 굴뚝 앞 유턴, 명단에 없음, 석탄 당첨)
 * @property gradeCode 등급 코드([com.santacut.domain.analysis.model.GiftGrade] 이름)
 * @property rank 전국 순위. 표본이 모자라거나 이 평가가 아직 순위에 오르지 않았으면 null이다.
 * @property deliveryEta 선물 도착 예정 시각. 석탄이거나 순위 계산 전이면 null이다.
 * @property title 칭호
 * @property verdict 판결 한 줄(농담)
 * @property verdictMild 놀림 없는 판결
 */
data class RecapVerdictResult(
    val cut: Int,
    val cutDistance: String,
    val cutDistanceValue: BigDecimal,
    val passed: Boolean,
    val stamp: String,
    val stampMild: String,
    val grade: String,
    val gradeCode: String,
    val rank: Int?,
    val rankingPending: Boolean,
    val deliveryEta: Instant?,
    val title: String,
    val verdict: String,
    val verdictMild: String,
) {
    companion object {
        private const val HUNDREDTHS_SCALE = 2

        /** 선물 판정 [gift]와 순위 [rank](표본 기준을 넘었을 때만), 도착 시각 [eta], 저장한 팩폭 문장 [content]로 만든다. */
        fun of(gift: GiftVerdict, rank: Int?, rankingPending: Boolean, eta: Instant?, content: RoastContent): RecapVerdictResult =
            RecapVerdictResult(
                GiftVerdict.CUT_POINTS, gift.cutDistanceText(), BigDecimal.valueOf(gift.cutDistanceHundredths().toLong(), HUNDREDTHS_SCALE),
                gift.passed, gift.stamp(), gift.stampMild(), gift.grade().label, gift.grade().name, rank, rankingPending,
                eta?.takeIf { gift.passed }, content.title.text, content.verdictLine.text, content.verdictMild.text,
            )
    }
}
