package com.santacut.domain.dossier.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.dossier.model.Dossier
import com.santacut.domain.dossier.model.DossierCatalog
import com.santacut.domain.dossier.model.DossierMessages
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.DossierStory
import com.santacut.domain.dossier.model.DossierSubject
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.dossier.model.DossierView
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.domain.global.model.SeoulTime
import com.santacut.infrastructure.dossier.repository.DossierRepository
import com.santacut.infrastructure.participant.repository.RegionRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant
import java.util.UUID

/**
 * 「2026 산타 조서」의 집계 저장과 장면 조립. 집계는 채점 입력과 같은 정규화 메시지에서 세고, 장면은 조회할 때 리소스로 만든다.
 */
@Service
class DossierService(
    private val dossierRepository: DossierRepository,
    private val regionRepository: RegionRepository,
    private val catalog: DossierCatalog,
    private val objectMapper: ObjectMapper,
) {
    private val logger = LoggerFactory.getLogger(DossierService::class.java)

    /**
     * 작업에 저장한 제출 내용에서 조서 집계를 센다. 조서는 채점 결과의 곁가지라 집계에 실패해도 채점 결과 저장을 막지 않는다.
     * 실패하면 예외 클래스 이름만 로그에 남기고 null을 돌려준다. 메시지 본문은 로그에 남기지 않는다.
     */
    fun tally(job: AnalysisJob): DossierTally? = try {
        val submission = objectMapper.readValue(job.requirePayload(), AnalysisSubmission::class.java)
        DossierMessages(submission.events.toScoringMessages(SeoulTime.ZONE).toList()).tally(catalog)
    } catch (exception: RuntimeException) {
        logger.warn("분석 작업 {} 조서 집계 실패, 조서 없이 결과를 저장한다: {}", job.id, exception.javaClass.simpleName)
        null
    }

    /** [assessment]의 조서를 저장한다. 집계가 없으면(실패) 아무것도 하지 않는다. */
    fun record(assessment: Assessment, tally: DossierTally?, recordedAt: Instant) {
        tally ?: return
        dossierRepository.save(Dossier.record(assessment, tally, objectMapper.writeValueAsString(tally.stats), recordedAt))
    }

    /**
     * 같은 채점 입력으로 다시 낸 작업이 지난 평가([previousAssessmentId])를 옮겨 적었을 때, 지난 조서 집계를 새 평가([assessment])에 복사한다.
     * 지난 조서가 없거나(이 기능 전 평가) 집계 규칙 판이 다르면 이 작업의 제출 내용으로 다시 센다.
     */
    fun reuse(job: AnalysisJob, previousAssessmentId: String, assessment: Assessment, recordedAt: Instant) {
        val previous = dossierRepository.findByAssessmentId(previousAssessmentId)?.takeIf(Dossier::isCurrentVersion)
        if (previous != null) dossierRepository.save(previous.copyFor(assessment, recordedAt)) else record(assessment, tally(job), recordedAt)
    }

    fun exists(assessmentId: String): Boolean = dossierRepository.existsByAssessmentId(assessmentId)

    /** [assessmentId] 평가의 조서. 이 기능 전 평가라 조서가 없으면 [ErrorCode.DOSSIER_NOT_FOUND]다. */
    fun get(assessmentId: String): Dossier =
        dossierRepository.findByAssessmentId(assessmentId) ?: throw SantaCutException(ErrorCode.DOSSIER_NOT_FOUND)

    /** 참가자의 지역 이름. 판결 장 메모에 쓴다. */
    fun regionNameOf(participantId: UUID): String? = regionRepository.findNameOfParticipantRegion(participantId.toString())

    /** 저장한 집계와 기존 결과([subject])로 장면을 조립한다. */
    fun write(dossier: Dossier, subject: DossierSubject): DossierView =
        DossierStory(catalog, objectMapper.readValue(dossier.statsJson, DossierStats::class.java), dossier.tone, subject).write()
}
