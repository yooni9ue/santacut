package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.ScoringMessage
import java.time.Duration

/**
 * 상대 화자 수와 답장 간격을 쌓는다. 상대 메시지 뒤 [QUIET_MINUTES]분 넘게 조용했다가 온 본인 첫 메시지의 간격만 잰다.
 * 상대 화자가 둘 이상(단체방)이 되면 더 모으지 않는다.
 */
class ReplyCounter {
    private val partners = PartnerSpeakers()
    private val gaps = ReplyGaps()

    /** 앞 메시지 [previous] 뒤에 온 [current]를 센다. */
    fun add(previous: ScoringMessage?, current: ScoringMessage) {
        if (!current.isSelf()) partners.add(current.speaker)
        if (previous == null || previous.isSelf() || !current.isSelf() || partners.isGroup()) return
        val minutes = Duration.between(previous.localTime, current.localTime).toMinutes()
        if (minutes >= QUIET_MINUTES) gaps.add(minutes)
    }

    /**
     * 답장 간격. 상대가 한 명인 카카오톡 대화만 중앙값을 낸다. 인스타그램 DM은 대화방 하나라도 알림·읽음 흐름이 달라 재지 않는다.
     */
    fun stats(source: DataSource): ReplyStats {
        val oneOnOne = partners.isOneOnOne()
        return ReplyStats(oneOnOne, if (oneOnOne && source == DataSource.KAKAO) gaps.medianMinutes() else null)
    }

    companion object {
        /** 상대 메시지 뒤 이만큼 조용했던 본인 첫 메시지만 답장 간격으로 잰다. */
        const val QUIET_MINUTES = 10L
    }
}
