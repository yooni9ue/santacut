package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.UtcInstantConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant

/**
 * 검증을 기다리는 원본 파일. 작업([AnalysisJob])과 따로 둔다. 상태 폴링이 작업 행을 자주 읽으므로
 * 작업 행에 최대 20MB 원본을 담지 않기 위해서다. 검증이 끝나는 트랜잭션에서 지우고, 늦어도 24시간 뒤에는 지운다.
 * 파일 이름은 저장하지 않는다.
 */
@Entity
@Table(name = "analysis_uploads")
class AnalysisUpload(
    jobId: String,
    source: DataSource,
    selfSpeaker: String,
    content: ByteArray,
    createdAt: Instant,
) {
    @Id
    @Column(name = "job_id")
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
     * 원본을 읽어 본인 메시지만 이벤트로 바꾸고 [now] 기준 제출 한도에 맞춘다.
     * 인스타그램 JSON은 [readInstagramThread]로 읽는다. 이벤트가 20개보다 적으면 INSUFFICIENT_EVENTS다.
     */
    fun normalize(now: Instant, readInstagramThread: (String) -> InstagramThread): AnalysisSubmission {
        val events = toRawFile().readMessages(readInstagramThread).eventsWrittenBy(selfSpeaker).recentAt(now)
        return AnalysisSubmission(source, events).also { it.verifyAcceptableAt(now) }
    }

    private fun toRawFile(): RawChatFile = RawChatFile(source, content)

    companion object {
        fun of(job: AnalysisJob, selfSpeaker: String, file: RawChatFile, createdAt: Instant): AnalysisUpload =
            AnalysisUpload(job.id, file.source, selfSpeaker, file.bytes(), createdAt)
    }
}
