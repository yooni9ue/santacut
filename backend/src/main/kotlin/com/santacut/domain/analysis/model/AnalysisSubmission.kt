package com.santacut.domain.analysis.model

import com.fasterxml.jackson.annotation.JsonInclude
import com.santacut.domain.dossier.model.DossierTally
import com.santacut.domain.global.model.SeoulTime
import java.time.Instant

/**
 * 분석 요청 본문. 그대로 JSON으로 바꿔 작업 페이로드로 저장하고, 그 해시로 재전송을 알아본다.
 *
 * @property dossier 원본 파일 제출에서만 채우는 조서 집계. 제출 한도에 맞춰 고르기 전의 분석 기간 메시지 전체로 센 값이다.
 * 이벤트 JSON 제출은 null이고 JSON에 쓰지 않으므로 이벤트 제출의 요청 해시가 바뀌지 않는다.
 * 원본 파일 제출의 요청 해시는 파일 해시라 이 필드와 상관없다. 원문 없이 집계값만 담고, 작업이 끝나면 페이로드와 함께 지워진다.
 * @property speakerNames 원본 파일 제출에서만 채우는 대화 참여자 표시 이름(본인 포함). 팩폭 작가 입력과 출력에서 이름을 가리는 데만 쓴다.
 * 비었으면 JSON에 쓰지 않아 이벤트 제출의 요청 해시가 바뀌지 않고, 이 필드가 없는 예전 페이로드는 빈 목록으로 읽는다.
 * 작업 페이로드 말고는 어디에도 저장하지 않고 로그·응답·예외에 넣지 않는다. 작업이 끝나면 페이로드와 함께 지워진다.
 */
class AnalysisSubmission(
    val source: DataSource,
    val events: AnalysisEvents,
    @field:JsonInclude(JsonInclude.Include.NON_NULL)
    val dossier: DossierTally? = null,
    @field:JsonInclude(JsonInclude.Include.NON_EMPTY)
    val speakerNames: List<String> = emptyList(),
) {
    fun verifyAcceptableAt(now: Instant) = events.verifyAcceptableAt(now)

    /** 채점 입력(정규화한 대화문)을 한국 시각 기준으로 만든다. */
    fun toScoringDigest(): ScoringDigest = ScoringDigest.of(events.toScoringMessages(SeoulTime.ZONE))

    /** 원본 파일 전체로 센 조서 집계 [tally]를 담은 제출 내용. */
    fun withDossier(tally: DossierTally?): AnalysisSubmission = AnalysisSubmission(source, events, tally, speakerNames)

    /** 원본 파일에서 읽은 참여자 표시 이름 [names]를 담은 제출 내용. */
    fun withSpeakerNames(names: List<String>): AnalysisSubmission = AnalysisSubmission(source, events, dossier, names)
}
