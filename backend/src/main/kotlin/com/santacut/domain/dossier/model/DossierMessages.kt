package com.santacut.domain.dossier.model

import com.santacut.domain.analysis.model.ScoringMessage
import java.time.Duration

/**
 * 조서를 셀 분석 기간의 메시지 전체(본인과 상대, 시각순, 한국 시각). 상대 메시지는 답장 간격과 연속 메시지 묶음을 나누는 데만 쓰고
 * 그 본문은 세지도 저장하지도 않는다.
 */
class DossierMessages(private val values: List<ScoringMessage>) {
    /** 사전 [catalog]로 집계값과 톤을 낸다. */
    fun tally(catalog: DossierCatalog): DossierTally {
        val selves = SelfMessages(values.filter { message -> message.isSelf() })
        val stats = DossierStats(
            DossierStats.VERSION, selves.volume(), selves.laugh(), selves.emoji(), selves.clock(), reply(),
            selves.style(burstSizes()), selves.words(catalog),
        )
        return DossierTally(stats, catalog.heavy.toneOf(selves.joinedText(), selves.size()))
    }

    /** 상대 화자가 한 명일 때만 답장 간격을 잰다. 단체방이나 상대 메시지가 없는 제출은 null이다. */
    private fun reply(): ReplyStats {
        val oneOnOne = values.filterNot { message -> message.isSelf() }.map { message -> message.speaker }.distinct().size == 1
        return ReplyStats(oneOnOne, if (oneOnOne) replyGaps().medianMinutes() else null)
    }

    private fun replyGaps(): ReplyGaps = ReplyGaps(
        values.zipWithNext()
            .filter { (previous, current) -> !previous.isSelf() && current.isSelf() }
            .map { (previous, current) -> Duration.between(previous.localTime, current.localTime).toMinutes() }
            .filter { minutes -> minutes >= QUIET_MINUTES },
    )

    /** 본인 메시지 묶음 크기. 상대 메시지가 끼거나 앞 메시지와 [BURST_GAP]보다 벌어지면 새 묶음이다. */
    private fun burstSizes(): Counts {
        val sizes = mutableListOf<Int>()
        values.forEachIndexed { index, message ->
            if (message.isSelf()) startOrExtend(sizes, values.getOrNull(index - 1), message)
        }
        return Counts(sizes)
    }

    private fun startOrExtend(sizes: MutableList<Int>, previous: ScoringMessage?, current: ScoringMessage) {
        if (previous != null && continuesBurst(previous, current)) sizes[sizes.lastIndex]++ else sizes += 1
    }

    private fun continuesBurst(previous: ScoringMessage, current: ScoringMessage): Boolean =
        previous.isSelf() && Duration.between(previous.localTime, current.localTime) <= BURST_GAP

    companion object {
        /** 상대 메시지 뒤 이만큼 조용했던 본인 첫 메시지만 답장 간격으로 잰다. */
        const val QUIET_MINUTES = 10L
        private val BURST_GAP: Duration = Duration.ofMinutes(3)
    }
}
