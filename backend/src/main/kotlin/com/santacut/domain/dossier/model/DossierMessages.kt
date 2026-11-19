package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoringMessage
import com.santacut.domain.global.model.SeoulTime

/**
 * 조서를 셀 분석 기간의 메시지(본인과 상대, 시각순, 한국 시각). 처음부터 끝까지 한 번만 훑으며 메시지마다 집계값만 더한다.
 * 1년치 단체방 원본(수십만 개)에서도 본문을 이어 붙이거나 목록을 다시 만들지 않으려는 것이다.
 * 상대 메시지는 답장 간격과 연속 메시지 묶음을 나누는 데만 쓰고 그 본문은 세지도 저장하지도 않는다.
 */
class DossierMessages(private val values: Sequence<ScoringMessage>) {
    /** 사전 [catalog]로 집계값과 톤을 낸다. 답장 간격은 출처 [source]가 카카오톡 1:1일 때만 잰다. */
    fun tally(catalog: DossierCatalog, source: DataSource): DossierTally {
        val counter = DossierCounter(catalog)
        values.forEach(counter::add)
        return counter.tally(source)
    }

    companion object {
        /** 제출 이벤트 [events]를 한국 시각 메시지로 하나씩 바꿔 센다. */
        fun of(events: AnalysisEvents): DossierMessages = DossierMessages(events.toScoringSequence(SeoulTime.ZONE))
    }
}
