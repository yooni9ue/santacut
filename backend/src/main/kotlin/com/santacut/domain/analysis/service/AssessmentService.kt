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
    fun record(job: AnalysisJob, score: Score, recordedAt: Instant): Assessment {
        val componentsJson = objectMapper.writeValueAsString(score.components.toList())
        return assessmentRepository.save(Assessment.record(job, score, componentsJson, recordedAt))
    }

    fun get(assessmentId: String): Assessment =
        assessmentRepository.findById(assessmentId).orElse(null)
            ?: throw SantaCutException(ErrorCode.RESULT_NOT_FOUND)
}
