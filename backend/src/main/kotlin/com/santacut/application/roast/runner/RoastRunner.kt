package com.santacut.application.roast.runner

import com.santacut.application.global.Runner
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.ModelUsage
import com.santacut.domain.analysis.model.ScoreComponents
import com.santacut.domain.dossier.model.DossierDraft
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.prompt.model.PromptName
import com.santacut.domain.prompt.service.PromptService
import com.santacut.domain.roast.model.RoastDraft
import com.santacut.domain.roast.model.RoastSheet
import com.santacut.domain.roast.service.RoastService
import org.slf4j.LoggerFactory

/**
 * 채점과 독립된 「팩폭 산타」 작가 호출을 감싸는 실행기. 무슨 이유로 실패하든(작가 꺼짐, 프롬프트 없음, 예산 막힘, 제공자 오류, 예외)
 * 예외를 밖으로 내지 않고 서버 대체 문장으로 채운 결과를 돌려준다. 채점 결과 저장과 작업 성공을 막지 않으려는 것이다.
 *
 * AI 비용 예약은 호출하는 쪽이 넘긴 [write]의 spending(채점과 같은 비용 가드 실행기의 함수)으로 한다. 다른 실행기를 주입받지 않기 위해서다.
 * 로그에는 작업 ID와 실패 종류만 남긴다.
 */
@Runner
class RoastRunner(
    private val roastService: RoastService,
    private val promptService: PromptService,
) {
    private val logger = LoggerFactory.getLogger(RoastRunner::class.java)

    /**
     * 작업 [job]의 총점 [hundredths]와 항목 점수 [dimensions], 미리 만든 조서 [dossier]로 팩폭 문장을 쓴다. 작업 페이로드를 읽으므로 작업 성공 전에 부른다.
     * [spending]은 어림 사용량을 예약한 채 작가 호출(호출마다 사용량을 기록하는 함수를 받는다)을 돌리고 예약을 푼다.
     */
    fun write(
        job: AnalysisJob,
        hundredths: Int,
        dimensions: ScoreComponents,
        dossier: DossierDraft?,
        spending: (ModelUsage, ((ModelUsage) -> Unit) -> RoastSheet) -> RoastSheet,
    ): RoastDraft {
        val seed = roastService.seedOf(job.userId, job.createdAt)
        if (!roastService.isEnabled()) return fallback(job, hundredths, dimensions, dossier, seed)
        return try {
            val prompt = promptService.latest(PromptName.ROAST)
            val brief = roastService.brief(job, hundredths, dimensions, dossier, seed)
            val sheet = spending(roastService.estimateUsage(brief, prompt)) { record -> roastService.ask(brief, prompt, record) }
            roastService.draft(brief, sheet, prompt)
        } catch (exception: Exception) {
            val reason = (exception as? SantaCutException)?.logReason() ?: exception.javaClass.simpleName
            logger.warn("분석 작업 {} 팩폭 작가 실패, 대체 문장으로 채운다: {}", job.id, reason)
            fallback(job, hundredths, dimensions, dossier, seed)
        }
    }

    /** 대체 문장만으로 만든 결과. 조서 집계로 만들다 실패하면 집계 없이 다시 만들어, 채점 결과 저장을 막지 않는다. */
    private fun fallback(job: AnalysisJob, hundredths: Int, dimensions: ScoreComponents, dossier: DossierDraft?, seed: Int): RoastDraft =
        try {
            roastService.fallbackDraft(hundredths, dimensions, dossier, seed)
        } catch (exception: RuntimeException) {
            logger.warn("분석 작업 {} 조서 집계로 대체 문장을 만들지 못해 집계 없이 만든다: {}", job.id, exception.javaClass.simpleName)
            roastService.fallbackDraft(hundredths, dimensions, null, seed)
        }
}
