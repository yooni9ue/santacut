package com.santacut.domain.analysis.model

import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.global.model.ForeignKeyColumns
import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * 검증을 기다리는 원본 파일. 작업([AnalysisJob])과 따로 둔다. 상태 폴링이 작업 행을 자주 읽으므로
 * 작업 행에 최대 20MB 원본을 담지 않기 위해서다. 검증이 끝나는 트랜잭션에서 지우고, 늦어도 24시간 뒤에는 지운다.
 * 파일 이름은 저장하지 않는다.
 */
@Entity
@Table(name = "analysis_uploads", indexes = [Index(name = "analysis_uploads_created", columnList = "created_at")])
class AnalysisUpload(
    jobId: String,
    source: DataSource,
    selfSpeaker: String,
    content: ByteArray,
    createdAt: Instant,
) {
    @Id
    @Column(name = "job_id", columnDefinition = ForeignKeyColumns.ANALYSIS_JOB)
    var jobId: String = jobId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    var source: DataSource = source
        protected set

    /** 참가자가 고른 본인 이름. 응답, 로그, 예외에 넣지 않는다. */
    @Column(name = "self_speaker", nullable = false)
    var selfSpeaker: String = selfSpeaker
        protected set

    // @Lob을 쓰면 SQLite 드라이버가 지원하지 않는 Blob 객체로 읽으므로 바이트 배열 컬럼으로 둔다.
    @Column(name = "content", nullable = false, columnDefinition = "BLOB")
    var content: ByteArray = content
        protected set

    @Convert(converter = UtcInstantConverter::class)
    @Column(name = "created_at", nullable = false)
    var createdAt: Instant = createdAt
        protected set

    /**
     * 원본을 읽어 본인과 상대 메시지를 화자 표시만 남긴 이벤트로 바꾸고 [now] 기준 제출 한도에 맞춘다.
     * 인스타그램 JSON은 [readInstagramThread]로 읽는다. 본인 이벤트가 20개보다 적으면 INSUFFICIENT_EVENTS다.
     * 한도에 맞춰 고르기 전의 분석 기간 이벤트 전체로 [tallyDossier]가 조서 집계를 세어 함께 담는다. 채점 입력은 고른 표본이지만
     * 메시지 수·달별 분포 같은 조서 숫자는 올해 전체 기준이어야 하기 때문이다. 집계에 실패하면 null이고 검증은 그대로 통과한다.
     * 참여자 표시 이름(본인 포함)도 담아 팩폭 작가 입력에서 이름을 가리게 한다.
     */
    fun normalize(
        now: Instant,
        readInstagramThread: (String) -> InstagramThread,
        tallyDossier: (DataSource, AnalysisEvents) -> DossierTally?,
    ): AnalysisSubmission {
        val messages = toRawFile().readMessages(readInstagramThread)
        val events = messages.eventsFor(selfSpeaker).inPeriodAt(now)
        val submission = AnalysisSubmission(source, events.selectedAt(now)).also { it.verifyAcceptableAt(now) }
        return submission.withDossier(tallyDossier(source, events)).withSpeakerNames(messages.speakerNames())
    }

    private fun toRawFile(): RawChatFile = RawChatFile(source, content)

    companion object {
        fun of(job: AnalysisJob, selfSpeaker: String, file: RawChatFile, createdAt: Instant): AnalysisUpload =
            AnalysisUpload(job.id, file.source, selfSpeaker, file.content(), createdAt)
    }
}
