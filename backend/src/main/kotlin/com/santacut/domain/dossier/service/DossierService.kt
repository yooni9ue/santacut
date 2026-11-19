package com.santacut.domain.dossier.service

import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.dossier.model.Dossier
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierDraft
import com.santacut.domain.dossier.model.DossierMessages
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.DossierStory
import com.santacut.domain.dossier.model.DossierSubject
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.dossier.model.DossierView
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.dossier.repository.DossierRepository
import com.santacut.infrastructure.participant.repository.RegionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * 「2026 산타 조서」의 집계 저장과 장면 조립. 원본 파일 제출은 정규화할 때 분석 기간 전체로, 이벤트 JSON 제출은 채점을 끝낼 때
 * 제출한 이벤트로 센다. 장면은 조회할 때 리소스로 만든다.
 *
 * 조서는 채점 결과의 곁가지라 집계에 실패해도 검증·채점을 막지 않는다. 실패하면 예외 클래스 이름과 누적 실패 수만 로그에 남기고
 * 조서 없이 진행한다. 누적 수가 늘면 운영자가 리소스나 입력 문제를 알아챌 수 있다. 메시지 본문은 로그에 남기지 않는다.
 */
@Service
class DossierService(
    private val dossierRepository: DossierRepository,
    private val regionRepository: RegionRepository,
    private val catalog: DossierCatalog,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(DossierService::class.java)
    private val failures = AtomicLong()

    /** 원본 파일 작업 [jobId]를 정규화할 때 분석 기간 이벤트 전체([events])로 센 집계. 실패하면 null이다. */
    fun tally(jobId: String, source: DataSource, events: AnalysisEvents): DossierTally? =
        tallying(jobId) { DossierMessages.of(events).tally(catalog, source) }

    /**
     * 작업 페이로드로 저장할 조서를 만든다. 원본 파일 제출은 정규화 때 전체로 센 집계를 쓰고, 그 값이 없거나(이벤트 JSON 제출,
     * 정규화 때 집계 실패) 집계 규칙 판이 다르면 페이로드의 이벤트로 센다. 쓰기 트랜잭션 밖에서 부른다. 실패하면 null이다.
     */
    fun draft(job: AnalysisJob): DossierDraft? = tallying(job.id) {
        val submission = objectMapper.readValue(job.requirePayload(), AnalysisSubmission::class.java)
        val tally = submission.dossier?.takeIf(DossierTally::isCurrentVersion)
            ?: DossierMessages.of(submission.events).tally(catalog, submission.source)
        tally.toDraft(objectMapper.writeValueAsString(tally.stats))
    }

    /** [assessment]의 조서를 저장한다. 집계가 없으면(실패) 아무것도 하지 않는다. */
    fun record(assessment: Assessment, draft: DossierDraft?, recordedAt: Instant) {
        draft ?: return
        dossierRepository.save(Dossier.record(assessment, draft, recordedAt))
    }

    /**
     * 같은 채점 입력으로 다시 낸 작업이 지난 평가([previousAssessmentId])를 옮겨 적었을 때, 지난 조서 집계를 새 평가([assessment])에 복사한다.
     * 지난 조서가 없거나(이 기능 전 평가) 집계 규칙 판이 다르면 이 작업으로 미리 만든 [draft]를 저장한다.
     */
    fun reuse(previousAssessmentId: String, assessment: Assessment, draft: DossierDraft?, recordedAt: Instant) {
        val previous = dossierRepository.findByAssessmentId(previousAssessmentId)?.takeIf(Dossier::isCurrentVersion)
        if (previous != null) dossierRepository.save(previous.copyFor(assessment, recordedAt)) else record(assessment, draft, recordedAt)
    }

    /** [assessmentId] 평가에 지금 집계 규칙 판의 조서가 있는지. */
    fun exists(assessmentId: String): Boolean =
        dossierRepository.existsByAssessmentIdAndStatsVersion(assessmentId, DossierStats.VERSION)

    /**
     * [assessmentId] 평가의 조서. 이 기능 전 평가라 조서가 없거나, 집계 규칙 판이 지금과 달라 장면을 조립할 수 없으면
     * [ErrorCode.DOSSIER_NOT_FOUND]다.
     */
    fun get(assessmentId: String): Dossier =
        dossierRepository.findByAssessmentId(assessmentId)?.takeIf(Dossier::isCurrentVersion)
            ?: throw SantaCutException(ErrorCode.DOSSIER_NOT_FOUND)

    /** 참가자의 지역 이름. 판결 장 메모에 쓴다. */
    fun regionNameOf(participantId: UUID): String? = regionRepository.findNameOfParticipantRegion(participantId.toString())

    /** 저장한 집계와 기존 결과([subject])로 장면을 조립한다. */
    fun write(dossier: Dossier, subject: DossierSubject): DossierView =
        DossierStory(catalog, objectMapper.readValue(dossier.statsJson, DossierStats::class.java), dossier.tone, subject).write()

    private fun <T : Any> tallying(jobId: String, action: () -> T): T? = try {
        action()
    } catch (exception: RuntimeException) {
        logger.warn(
            "분석 작업 {} 조서 집계 실패(누적 {}번), 조서 없이 진행한다: {}", jobId, failures.incrementAndGet(), exception.javaClass.simpleName,
        )
        null
    }
}
