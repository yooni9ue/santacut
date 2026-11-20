package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreAnswer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicReference

/** 채점 호출을 가상 스레드로 동시에 부른 결과 목록. */
class ScoreAnswerCalls private constructor(private val values: List<Future<ScoreAnswer>>) {
    private fun answers(): List<ScoreAnswer> = values.map { future -> resultOf(future) }

    companion object {
        /** 먼저 온 구간부터 자리를 받게 해, 동시에 부를 수 있는 수보다 구간이 많을 때 앞 구간이 늦게 불리지 않게 한다. */
        private const val FAIR_SLOTS = true

        /**
         * [asker]를 [count]번 동시에 불러 답을 모은다. 실행기를 닫을 때 모든 호출이 끝나기를 기다리므로, 한 호출이 예외를 던져도
         * 나머지 호출의 사용량 기록은 끝나 있다. 실행 예외([RuntimeException])를 던진 호출은 작업을 바로 끝내지 않고
         * 버릴 답([ScoreAnswer.threw])으로 바꾼다. 남은 답이 과반이면 그 답들로 합치고, 아니면 합칠 때 그 예외를 그대로 던진다.
         * [Error]처럼 실행 예외가 아닌 것은 그대로 던진다.
         */
        fun concurrently(count: Int, asker: () -> ScoreAnswer): List<ScoreAnswer> =
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                ScoreAnswerCalls(List(count) { executor.submit(Callable { asker() }) }).answers()
            }

        /**
         * [items]마다 [asker]를 가상 스레드로 부르되, 한 번에 많아야 [limit]개만 동시에 부른다. 답은 [items] 순서대로 돌려준다.
         * 예외 처리와 기다림은 [concurrently]와 같다. 모든 호출이 끝난 뒤 돌아오므로 호출마다 사용량 기록도 끝나 있다.
         * 쓸 수 없는 답(제공자 차단, 읽기 실패, 호출 예외)이 하나라도 나오면 작업 전체가 그 답으로 끝나므로, 아직 자리를 못 잡은 호출은
         * 부르지 않고 그 실패 답을 사용량 없이 대신 돌려준다. 이미 불린 호출은 끝까지 기다린다.
         */
        fun <T> each(items: List<T>, limit: Int, asker: (T) -> ScoreAnswer): List<ScoreAnswer> {
            val slots = Semaphore(limit.coerceAtLeast(1), FAIR_SLOTS)
            val failure = AtomicReference<ScoreAnswer?>(null)
            return Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                ScoreAnswerCalls(items.map { item -> executor.submit(Callable { holding(slots) { unlessFailed(failure) { asker(item) } } }) })
                    .answers()
            }
        }

        /**
         * 첫 항목을 먼저 혼자 부르고, 끝나면 나머지를 [each]로 한 번에 많아야 [limit]개씩 동시에 부른다. 답은 [items] 순서대로 돌려준다.
         * 첫 답이 쓸 수 없으면(호출 예외 포함) 작업 전체가 그 답으로 끝나므로 나머지는 부르지 않고 그 답 하나만 돌려준다.
         * 모든 호출의 앞부분(시스템 프롬프트)이 같으므로 첫 호출이 끝난 뒤 보내면 Gemini 암묵적 캐시에서 읽혀 입력 단가가 낮아진다.
         * 모두 동시에 보내면 아무 호출도 캐시를 못 탄다(backend-design.md, 캐시 토큰).
         */
        fun <T> firstThenEach(items: List<T>, limit: Int, asker: (T) -> ScoreAnswer): List<ScoreAnswer> {
            if (items.isEmpty()) return emptyList()
            val first = answerOf { asker(items.first()) }
            if (!first.isUsable()) return listOf(first)
            return listOf(first) + each(items.drop(1), limit, asker)
        }

        /**
         * 앞서 쓸 수 없는 답이 나왔으면 [call]을 부르지 않고 그 답을 사용량 없이 돌려준다. 아니면 부르고, 그 답이 쓸 수 없으면 남긴다.
         * 호출 예외도 버릴 답으로 바꿔 남긴다.
         */
        private fun unlessFailed(failure: AtomicReference<ScoreAnswer?>, call: () -> ScoreAnswer): ScoreAnswer {
            failure.get()?.let { failed -> return failed.withUsage(ModelUsage.NONE) }
            val answer = answerOf(call)
            if (!answer.isUsable()) failure.compareAndSet(null, answer)
            return answer
        }

        /** 이 스레드에서 [call]을 부른다. 실행 예외는 [concurrently]처럼 버릴 답으로 바꾼다. */
        private fun answerOf(call: () -> ScoreAnswer): ScoreAnswer =
            try {
                call()
            } catch (exception: RuntimeException) {
                ScoreAnswer.threw(exception)
            }

        /** 자리 하나를 잡은 동안만 [call]을 부른다. */
        private fun holding(slots: Semaphore, call: () -> ScoreAnswer): ScoreAnswer {
            slots.acquire()
            try {
                return call()
            } finally {
                slots.release()
            }
        }

        private fun resultOf(future: Future<ScoreAnswer>): ScoreAnswer =
            try {
                future.get()
            } catch (exception: ExecutionException) {
                val cause = exception.cause
                if (cause !is RuntimeException) throw cause ?: exception
                ScoreAnswer.threw(cause)
            }
    }
}
