package com.santacut.domain.service

import com.santacut.domain.exception.ErrorCode
import com.santacut.domain.exception.SantaCutException
import com.santacut.domain.model.AnalysisJob
import com.santacut.domain.model.Assessment
import com.santacut.domain.model.Score
import com.santacut.infrastructure.repository.AssessmentRepository
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
