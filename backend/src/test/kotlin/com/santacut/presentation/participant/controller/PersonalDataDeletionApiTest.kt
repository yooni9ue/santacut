package com.santacut.presentation.participant.controller

import com.jayway.jsonpath.JsonPath
import com.santacut.IntegrationTest
import com.santacut.TestChatFiles
import com.santacut.TestChatFiles.SELF_SPEAKER
import com.santacut.TestCsrf.withCsrf
import com.santacut.TestDossiers
import com.santacut.TestParticipants.REGION_CODE
import com.santacut.TestParticipants.uniqueNickname
import com.santacut.application.participant.command.RegisterParticipantCommand
import com.santacut.application.participant.facade.ParticipantFacade
import com.santacut.application.share.command.CreateShareCommand
import com.santacut.application.share.facade.ShareFacade
import com.santacut.domain.analysis.model.AnalysisEvents
import com.santacut.domain.analysis.model.AnalysisJob
import com.santacut.domain.analysis.model.AnalysisPayload
import com.santacut.domain.analysis.model.AnalysisSubmission
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.EarnedBadges
import com.santacut.domain.analysis.model.IdempotencyKey
import com.santacut.domain.analysis.model.LowConfidenceDimensions
import com.santacut.domain.analysis.model.Score
import com.santacut.domain.analysis.model.ScoredAssessment
import com.santacut.domain.analysis.service.AssessmentService
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.ranking.service.RankingService
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.time.Clock
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DELETE /v1/me가 참가자 행 하나만 지워도 DB의 외래 키 삭제 규칙(ON DELETE CASCADE)이 그 참가자의 모든 행을 함께 지우는지 확인한다.
 * 세션, 지역 변경 기록, 업로드 원본, 분석 작업, 평가, 순위 스냅숏, 공유 링크, 산타 조서를 모두 만든 뒤 지우고 남은 행을 센다.
 */
@IntegrationTest
class PersonalDataDeletionApiTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val participantFacade: ParticipantFacade,
    private val shareFacade: ShareFacade,
    private val assessmentService: AssessmentService,
    private val dossierService: DossierService,
    private val rankingService: RankingService,
    private val transactionTemplate: TransactionTemplate,
    private val jdbcTemplate: JdbcTemplate,
    private val clock: Clock,
) {
    @Test
    fun `내 데이터를 지우면 참가자가 남긴 모든 테이블의 행이 사라지고 외래 키 위반도 없다`() {
        val participant = participantFacade.register(RegisterParticipantCommand(uniqueNickname(), REGION_CODE))
        val session = Cookie(SESSION_COOKIE, participant.sessionToken)
        val userId = participant.id.toString()
        changeRegion(session)
        val jobId = upload(session)
        recordAssessmentAndRebuild(participant.id)
        shareFacade.createShare(CreateShareCommand(participant.id))
        assertTrue(rowsOf(userId, jobId).values.all { it > 0 }, "지우기 전 행: ${rowsOf(userId, jobId)}")

        mockMvc.perform(withCsrf(mockMvc, delete(ME_PATH).cookie(session))).andExpect(status().isNoContent)

        assertEquals(USER_TABLES.associateWith { 0 } + (UPLOADS to 0), rowsOf(userId, jobId))
        assertEquals(emptyList(), jdbcTemplate.queryForList("PRAGMA foreign_key_check"))
    }

    private fun changeRegion(session: Cookie) {
        val request = patch(REGION_PATH).cookie(session).contentType(MediaType.APPLICATION_JSON)
            .content("{\"regionCode\":\"$OTHER_REGION_CODE\"}")
        mockMvc.perform(withCsrf(mockMvc, request)).andExpect(status().isOk)
    }

    private fun upload(session: Cookie): String {
        val file = MockMultipartFile(FILE_PART, FILE_NAME, MediaType.TEXT_PLAIN_VALUE, TestChatFiles.kakaoIos(clock.instant(), EVENT_COUNT))
        val request = withCsrf(mockMvc, multipart(UPLOAD_PATH).file(file)).cookie(session)
            .header(IDEMPOTENCY_KEY, "key-" + UUID.randomUUID())
            .param(SOURCE_PART, SOURCE)
            .param(SELF_SPEAKER_PART, SELF_SPEAKER)
        val body = mockMvc.perform(request).andExpect(status().isAccepted).andReturn().response.contentAsString
        return JsonPath.read(body, "$.id")
    }

    private fun recordAssessmentAndRebuild(participantId: UUID) {
        val now = clock.instant()
        val score = Score.shrunk(SCORES.map { BigDecimal(it) }, LowConfidenceDimensions.NONE, null)
        val job = AnalysisJob.enqueue(
            participantId,
            IdempotencyKey.from("key-" + UUID.randomUUID()),
            AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())),
            AnalysisPayload("{}"),
            now,
        )
        transactionTemplate.executeWithoutResult {
            val assessment = assessmentService.record(job, ScoredAssessment(score, 1, null, EarnedBadges(emptyList())), now)
            dossierService.record(assessment, TestDossiers.tally(TestDossiers.selves(listOf("고마워", "진짜"))), now)
            rankingService.rebuild(now)
        }
    }

    /** 참가자 행이 남을 수 있는 테이블마다 그 참가자의 행 수. 업로드 원본은 작업 id로 센다. */
    private fun rowsOf(userId: String, jobId: String): Map<String, Int> =
        USER_TABLES.associateWith { table ->
            val column = if (table == PARTICIPANTS) "id" else "user_id"
            jdbcTemplate.queryForObject("SELECT count(*) FROM $table WHERE $column = ?", Int::class.java, userId) ?: 0
        } + (UPLOADS to (jdbcTemplate.queryForObject("SELECT count(*) FROM $UPLOADS WHERE job_id = ?", Int::class.java, jobId) ?: 0))

    companion object {
        private const val PARTICIPANTS = "participants"
        private const val UPLOADS = "analysis_uploads"
        private val USER_TABLES = listOf(
            PARTICIPANTS, "sessions", "region_changes", "analysis_jobs", "assessments", "result_snapshots", "shares", "dossiers",
        )
        private const val ME_PATH = "/v1/me"
        private const val REGION_PATH = "/v1/me/region"
        private const val UPLOAD_PATH = "/v1/analyses/files"
        private const val SESSION_COOKIE = "sc_session"
        private const val IDEMPOTENCY_KEY = "Idempotency-Key"
        private const val FILE_PART = "file"
        private const val FILE_NAME = "KakaoTalkChats.txt"
        private const val SOURCE_PART = "source"
        private const val SELF_SPEAKER_PART = "self_speaker"
        private const val SOURCE = "KAKAO"
        private const val OTHER_REGION_CODE = "KR-11-650"
        private const val EVENT_COUNT = 20
        private val SCORES = listOf(60, 60, 60, 60)
    }
}
