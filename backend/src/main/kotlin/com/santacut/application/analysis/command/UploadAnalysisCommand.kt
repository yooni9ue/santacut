package com.santacut.application.analysis.command

import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.RawChatFile
import java.util.UUID

/**
 * 원본 파일로 분석 작업을 접수한다. 바이트 배열을 담으므로 값 비교를 하는 data class로 두지 않는다.
 *
 * @property source 요청 검증을 통과한 [DataSource] 이름
 * @property selfSpeaker 참가자가 고른 본인 이름. 응답, 로그, 예외에 넣지 않는다.
 */
class UploadAnalysisCommand(
    val participantId: UUID,
    val idempotencyKey: String,
    val source: String,
    val selfSpeaker: String,
    private val content: ByteArray,
) {
    fun toRawFile(): RawChatFile = RawChatFile(DataSource.valueOf(source), content)
}
