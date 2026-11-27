package com.santacut.application.roast.runner

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.dossier.model.DossierDraft
import com.santacut.domain.dossier.model.DossierTone
import com.santacut.domain.prompt.service.PromptService
import com.santacut.domain.roast.model.RoastDraft
import com.santacut.domain.roast.model.RoastSheet
import com.santacut.domain.roast.service.RoastService
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import java.time.Instant
import kotlin.test.assertEquals

/** 작가 실행기의 마지막 대체 경로. 어떤 실패도 채점 결과 저장을 막지 않는다. */
class RoastRunnerTest {
    private val roastService = mock(RoastService::class.java)
    private val runner = RoastRunner(roastService, mock(PromptService::class.java))

    @Test
    fun `조서 집계로 대체 문장을 만들다 실패하면 집계 없이 만든 대체 문장을 돌려준다`() {
        val job = mock(AnalysisJob::class.java)
        val dossier = DossierDraft(STATS_JSON, DossierTone.NORMAL, 1)
        given(job.id).willReturn(JOB_ID)
        given(job.userId).willReturn(USER_ID)
        given(job.createdAt).willReturn(CREATED_AT)
        given(roastService.seedOf(USER_ID, CREATED_AT)).willReturn(SEED)
        given(roastService.isEnabled()).willReturn(false)
        given(roastService.fallbackDraft(SCORE, DIMENSIONS, dossier, SEED)).willThrow(IllegalStateException("집계 깨짐"))
        given(roastService.fallbackDraft(SCORE, DIMENSIONS, null, SEED)).willReturn(PLAIN)

        val draft = runner.write(job, SCORE, DIMENSIONS, dossier) { _, _ -> RoastSheet.EMPTY }

        assertEquals(PLAIN, draft)
    }

    companion object {
        private const val JOB_ID = "job-1"
        private const val USER_ID = "user-1"
        private val CREATED_AT: Instant = Instant.parse("2026-11-20T00:00:00Z")
        private const val SEED = 7
        private const val SCORE = 7373
        private val DIMENSIONS = ScoreComponents.fromColumn("[80,70,60,75]")
        private const val STATS_JSON = "{}"
        private val PLAIN = RoastDraft("{}", null, "")
    }
}
