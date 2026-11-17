package com.santacut.application.analysis.runner

import com.santacut.application.global.Runner
import com.santacut.domain.analysis.model.AnalysisJob
import org.springframework.core.task.TaskRejectedException
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Semaphore

/**
 * 채점 스레드([analysisWorkerExecutor])의 빈 자리마다 작업을 하나씩 잡아 넘기는 실행기.
 * 채점 스레드는 작업을 끝내면 기다리지 않고 다음 작업을 잡고, 대기열이 비면 자리를 돌려주고 물러난다.
 * 그래서 스레드 수(자리 수)만큼 작업을 동시에 채점한다.
 *
 * 자리는 네 경우에 돌려준다. 잡을 작업이 없을 때, 잡다가 예외가 났을 때, 앱이 꺼지는 중이라 스레드가 작업을 받지 않을 때,
 * 채점 스레드가 이어 하던 작업이 끝났을 때(정상이든 예외든)다.
 */
@Runner
class AnalysisWorkerRunner(private val analysisWorkerExecutor: ThreadPoolTaskExecutor) {
    private val workerSlots = Semaphore(analysisWorkerExecutor.maxPoolSize)

    /** 빈 자리마다 [claim]으로 작업을 잡아 채점 스레드에 넘긴다. 스레드는 [work]를 마칠 때마다 [claim]으로 다음 작업을 잡는다. */
    fun dispatch(claim: () -> AnalysisJob?, work: (AnalysisJob) -> Unit) = dispatchWhileSlotsFree(claim, work)

    private tailrec fun dispatchWhileSlotsFree(claim: () -> AnalysisJob?, work: (AnalysisJob) -> Unit) {
        if (!workerSlots.tryAcquire()) return
        val job = claimForSlot(claim) ?: return
        startWorker(job, claim, work)
        dispatchWhileSlotsFree(claim, work)
    }

    /** 방금 얻은 자리로 작업을 잡는다. 잡을 작업이 없거나 잡다가 예외가 나면 자리를 돌려준다. */
    private fun claimForSlot(claim: () -> AnalysisJob?): AnalysisJob? {
        val job = try {
            claim()
        } catch (exception: RuntimeException) {
            workerSlots.release()
            throw exception
        }
        if (job == null) workerSlots.release()
        return job
    }

    /** 잡은 작업을 채점 스레드에 넘긴다. 스레드는 대기열이 빌 때까지 이어서 채점한 뒤 자리를 돌려준다. */
    private fun startWorker(job: AnalysisJob, claim: () -> AnalysisJob?, work: (AnalysisJob) -> Unit) {
        try {
            analysisWorkerExecutor.execute { workUntilEmpty(job, claim, work) }
        } catch (exception: TaskRejectedException) {
            // 앱이 꺼지는 중이다. 잡은 작업은 리스가 끝난 뒤 다시 잡힌다.
            workerSlots.release()
            throw exception
        }
    }

    private fun workUntilEmpty(job: AnalysisJob, claim: () -> AnalysisJob?, work: (AnalysisJob) -> Unit) {
        try {
            workChain(job, claim, work)
        } finally {
            workerSlots.release()
        }
    }

    private tailrec fun workChain(job: AnalysisJob, claim: () -> AnalysisJob?, work: (AnalysisJob) -> Unit) {
        work(job)
        workChain(claim() ?: return, claim, work)
    }
}
