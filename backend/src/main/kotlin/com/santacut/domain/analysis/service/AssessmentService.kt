package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.analysis.repository.AssessmentRepository
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper
import java.time.Instant

@Service
class AssessmentService(
    private val assessmentRepository: AssessmentRepository,
    private val objectMapper: ObjectMapper,
) {
    /** [promptVersion]은 점수를 매길 때 쓴 채점 프롬프트 판 번호다. */
    fun record(job: AnalysisJob, score: Score, promptVersion: Int, recordedAt: Instant): Assessment {
        val componentsJson = objectMapper.writeValueAsString(score.components.toList())
        return assessmentRepository.save(Assessment.record(job, score, componentsJson, promptVersion, recordedAt))
    }

    /** [previousAssessmentId] 평가를 이 작업의 새 평가로 다시 저장한다. */
    fun reuse(job: AnalysisJob, previousAssessmentId: String, recordedAt: Instant): Assessment =
        assessmentRepository.save(Assessment.reuse(job, get(previousAssessmentId), recordedAt))

    fun get(assessmentId: String): Assessment =
        assessmentRepository.findById(assessmentId).orElse(null)
            ?: throw SantaCutException(ErrorCode.RESULT_NOT_FOUND)
}
