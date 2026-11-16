package com.santacut.infrastructure.analysis.client

import com.santacut.domain.analysis.model.ScoreAnswer
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.Future

/** 같은 채점 호출을 가상 스레드로 동시에 부른 결과 목록. */
class ScoreAnswerCalls private constructor(private val values: List<Future<ScoreAnswer>>) {
    private fun answers(): List<ScoreAnswer> = values.map { future -> resultOf(future) }

    companion object {
        /**
         * [asker]를 [count]번 동시에 불러 답을 모은다. 실행기를 닫을 때 모든 호출이 끝나기를 기다리므로, 한 호출이 예외를 던져도
         * 나머지 호출의 사용량 기록은 끝나 있다. 호출 예외는 감싸지 않고 그대로 던진다.
         */
        fun concurrently(count: Int, asker: () -> ScoreAnswer): List<ScoreAnswer> =
            Executors.newVirtualThreadPerTaskExecutor().use { executor ->
                ScoreAnswerCalls(List(count) { executor.submit(Callable { asker() }) }).answers()
            }

        private fun resultOf(future: Future<ScoreAnswer>): ScoreAnswer =
            try {
                future.get()
            } catch (exception: ExecutionException) {
                throw exception.cause ?: exception
            }
    }
}
