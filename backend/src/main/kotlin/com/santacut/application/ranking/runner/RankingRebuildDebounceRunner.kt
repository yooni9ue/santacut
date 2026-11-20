package com.santacut.application.ranking.runner

import com.santacut.application.global.Runner
import com.santacut.domain.ranking.service.RankingService
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 순위 다시 계산 요청이 커밋될 때마다 계산을 곧바로 하지 않고, 짧게 기다린 뒤 한 번만 돌리는 실행기.
 *
 * 기다리는 동안([RankingService.rebuildDebounce]) 들어온 요청은 이미 예약한 한 번에 모은다. 그래서 작업이 몰려 끝나도 계산은 한 번이다.
 * 예약한 계산이 시작할 때 예약 표시를 먼저 내리므로, 계산 도중 커밋된 요청은 다음 한 번을 새로 예약한다.
 * 계산끼리 겹치지 않게 하는 잠금은 계산하는 쪽([rebuild])이 맡는다. 기다림은 가상 스레드에서 해 다른 스레드를 붙잡지 않는다.
 * 앱이 꺼지면 예약한 계산은 사라지지만 요청 표시가 남아 앱 시작이나 예약 작업이 처리한다.
 */
@Runner
class RankingRebuildDebounceRunner(private val rankingService: RankingService) {
    private val scheduled = AtomicBoolean(false)

    /** 아직 예약한 계산이 없으면 기다린 뒤 [rebuild]를 한 번 돌리도록 예약한다. 커밋 직후 다시 계산을 꺼 두었으면 아무것도 하지 않는다. */
    fun rebuildSoon(rebuild: () -> Unit) {
        val delay = rankingService.rebuildDebounce() ?: return
        if (!scheduled.compareAndSet(false, true)) return
        Thread.ofVirtual().name(THREAD_NAME).start { rebuildAfter(delay, rebuild) }
    }

    private fun rebuildAfter(delay: Duration, rebuild: () -> Unit) {
        try {
            Thread.sleep(delay)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            scheduled.set(false)
        }
        if (!Thread.currentThread().isInterrupted) rebuild()
    }

    companion object {
        private const val THREAD_NAME = "ranking-rebuild-debounce"
    }
}
