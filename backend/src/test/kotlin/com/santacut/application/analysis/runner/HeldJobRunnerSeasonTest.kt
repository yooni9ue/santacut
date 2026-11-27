package com.santacut.application.analysis.runner

import com.santacut.application.global.TransactionRunner
import com.santacut.domain.analysis.model.ScoringAttempt
import com.santacut.domain.analysis.service.AnalysisJobService
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.roast.service.RoastService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.verifyNoMoreInteractions
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.TransactionStatus
import org.springframework.transaction.support.SimpleTransactionStatus
import org.springframework.transaction.support.TransactionTemplate
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertNull

/** 같은 입력 재사용 전 작가를 새로 부를지 묻는 단계. 시즌이 마감됐으면 재사용이 동결로 끝나므로 묻지도 않는다. */
class HeldJobRunnerSeasonTest {
    private val analysisJobService = mock(AnalysisJobService::class.java)
    private val roastService = mock(RoastService::class.java)
    private val transaction = TransactionRunner(TransactionTemplate(NoTransactions))
    private val runner = HeldJobRunner(
        analysisJobService, mock(AssessmentService::class.java), mock(DossierService::class.java), roastService,
        mock(RankingService::class.java), transaction, Clock.fixed(NOW, ZoneOffset.UTC),
    )

    @Test
    fun `시즌이 마감됐으면 지난 결과를 찾지 않고 작가를 새로 부를 평가도 없다`() {
        given(analysisJobService.isSeasonClosedAt(NOW)).willReturn(true)

        assertNull(runner.assessmentNeedingRoast(mock(ScoringAttempt::class.java)))
        verify(analysisJobService).isSeasonClosedAt(NOW)
        verifyNoMoreInteractions(analysisJobService)
        verifyNoInteractions(roastService)
    }

    /** 트랜잭션 경계만 흉내 내는 관리자. 이 테스트는 DB를 쓰지 않는다. */
    private object NoTransactions : PlatformTransactionManager {
        override fun getTransaction(definition: TransactionDefinition?): TransactionStatus = SimpleTransactionStatus()

        override fun commit(status: TransactionStatus) = Unit

        override fun rollback(status: TransactionStatus) = Unit
    }

    companion object {
        private val NOW: Instant = Instant.parse("2099-12-24T00:00:00Z")
    }
}
