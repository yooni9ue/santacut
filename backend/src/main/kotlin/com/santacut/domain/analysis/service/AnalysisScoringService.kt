package com.santacut.domain.analysis.service

import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import com.santacut.infrastructure.analysis.client.ScoreClient
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class AnalysisScoringService(
    private val scoreClient: ScoreClient,
    private val objectMapper: ObjectMapper,
) {
    fun isEnabled(): Boolean = scoreClient.isEnabled()

    fun verifyEnabled() {
        if (!isEnabled()) throw SantaCutException(ErrorCode.AI_NOT_CONFIGURED)
    }

    /** 작업에 저장한 제출 내용을 AI에 보내고, 답을 검증해 점수로 만든다. */
    fun score(job: AnalysisJob): Score {
        val submission = objectMapper.readValue(job.requirePayload(), AnalysisSubmission::class.java)
        return scoreClient.requestAnswer(submission).toScore(submission.eventCount())
    }
}
