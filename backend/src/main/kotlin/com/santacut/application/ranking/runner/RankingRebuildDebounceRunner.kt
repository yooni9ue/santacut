package com.santacut.application.ranking.runner

import com.santacut.application.global.Runner
import com.santacut.domain.ranking.service.RankingService
import jakarta.annotation.PreDestroy
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicReference

/**
 * 순위 다시 계산 요청이 커밋될 때마다 계산을 곧바로 하지 않고, 기다린 뒤 한 번만 돌리는 실행기.
 *
 * 기다리는 시간은 짧은 모으기 시간([RankingService.rebuildDebounce])과, 지난 계산이 끝난 뒤 최소 간격
 * ([RankingService.rebuildMinInterval])까지 남은 시간 가운데 긴 쪽이다. 그래서 작업이 꾸준히 끝나도 계산은 최소 간격마다 한 번을 넘지 않는다.
 * 기다리는 동안 들어온 요청은 이미 예약한 한 번에 모은다. 예약 표시는 계산하는 쪽이 잠금을 잡은 직후([rebuildStarted]) 내리므로,
 * 잠금을 기다리는 동안 커밋된 요청도 그 한 번에 들어가고, 계산 도중 커밋된 요청은 다음 한 번을 새로 예약한다.
 * 계산끼리 겹치지 않게 하는 잠금은 계산하는 쪽([rebuildSoon]의 rebuild)이 맡는다. 기다림은 가상 스레드에서 해 다른 스레드를 붙잡지 않는다.
 * 앱이 꺼지는 중이면([close]) 예약한 계산을 건너뛴다. 요청 표시가 남아 다음 앱 시작이나 예약 작업이 처리한다.
 *
 * @property pause 기다리는 방법. 테스트가 바꿔 끼운다.
 */
@Runner
class RankingRebuildDebounceRunner(
    private val rankingService: RankingService,
    private val clock: Clock,
    private val pause: (Duration) -> Unit = Thread::sleep,
) {
    /** 예약한 계산의 표시. 비어 있으면 예약한 계산이 없다. */
    private val scheduled = AtomicReference<Any?>(null)

    @Volatile
    private var closed = false

    @Volatile
    private var lastFinishedAt: Instant? = null

    /** 아직 예약한 계산이 없으면 기다린 뒤 [rebuild]를 한 번 돌리도록 예약한다. 커밋 직후 다시 계산을 꺼 두었거나 앱이 꺼지는 중이면 아무것도 하지 않는다. */
    fun rebuildSoon(rebuild: () -> Unit) {
        val debounce = rankingService.rebuildDebounce() ?: return
        val mark = Any()
        if (closed || !scheduled.compareAndSet(null, mark)) return
        Thread.ofVirtual().name(THREAD_NAME).start { rebuildAfter(mark, waitFor(debounce), rebuild) }
    }

    /** 계산하는 쪽이 잠금을 잡은 직후 부른다. 이 뒤에 커밋된 요청은 다음 계산을 새로 예약한다. */
    fun rebuildStarted() = scheduled.set(null)

    /** 계산(성공이든 실패든)이 끝나면 부른다. 최소 간격은 이 시각부터 잰다. */
    fun rebuildFinished() {
        lastFinishedAt = clock.instant()
    }

    /** 앱이 꺼지기 시작하면 예약한 계산을 더 돌리지 않는다. */
    @PreDestroy
    fun close() {
        closed = true
    }

    /** 모으기 시간과, 지난 계산이 끝난 뒤 최소 간격까지 남은 시간 가운데 긴 쪽. */
    private fun waitFor(debounce: Duration): Duration {
        val finishedAt = lastFinishedAt ?: return debounce
        val remaining = rankingService.rebuildMinInterval().minus(Duration.between(finishedAt, clock.instant()))
        return maxOf(debounce, remaining)
    }

    /** 기다린 뒤 [rebuild]를 돌린다. 계산이 잠금까지 가지 못하고 끝나도(건너뜀, 예외) 이 예약의 표시[mark]는 내려 다음 요청을 막지 않는다. */
    private fun rebuildAfter(mark: Any, delay: Duration, rebuild: () -> Unit) {
        try {
            pause(delay)
            if (!closed) rebuild()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            scheduled.compareAndSet(mark, null)
        }
    }

    companion object {
        private const val THREAD_NAME = "ranking-rebuild-debounce"
    }
}
