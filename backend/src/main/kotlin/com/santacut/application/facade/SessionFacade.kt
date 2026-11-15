package com.santacut.application.facade

import com.santacut.application.command.SessionAuthenticationCommand
import com.santacut.application.result.AuthenticatedSessionResult
import com.santacut.domain.service.SessionService
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Facade
class SessionFacade(
    private val sessionService: SessionService,
    private val clock: Clock,
) {
    /** 유효한 세션이면 참가자를, 없거나 만료됐으면 null을 돌려준다. */
    @Transactional(readOnly = true)
    fun authenticate(command: SessionAuthenticationCommand): AuthenticatedSessionResult? =
        sessionService.findActive(command.token, clock.instant())?.let(AuthenticatedSessionResult::from)
}
