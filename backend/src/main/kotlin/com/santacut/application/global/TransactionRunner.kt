package com.santacut.application.global

import org.springframework.transaction.support.TransactionTemplate

/**
 * 한 흐름 안에서 트랜잭션을 여러 번 나눠 열어야 하는 facade·runner가 쓰는 트랜잭션 경계.
 * 쓰는 경우는 컨벤션 §7을 따른다. 보통의 facade 메서드는 `@Transactional`을 쓴다.
 */
@Runner
class TransactionRunner(private val transactionTemplate: TransactionTemplate) {
    /** [step]을 트랜잭션 하나로 실행하고 커밋한다. 예외가 나면 롤백하고 다시 던진다. */
    fun run(step: () -> Unit) = transactionTemplate.executeWithoutResult { step() }

    /** [step]을 트랜잭션 하나로 실행해 결과를 돌려준다. 예외가 나면 롤백하고 다시 던진다. */
    fun <T> call(step: () -> T): T? = transactionTemplate.execute { step() }
}
