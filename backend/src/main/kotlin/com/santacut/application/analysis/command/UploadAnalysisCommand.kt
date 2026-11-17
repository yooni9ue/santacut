package com.santacut.application.analysis.command

import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.RawChatFile
import java.util.UUID

/**
 * 원본 파일로 분석 작업을 접수한다. 원본을 읽는 함수를 담으므로 값 비교를 하는 data class로 두지 않는다.
 *
 * @property source 요청 검증을 통과한 [DataSource] 이름
 * @property selfSpeaker 참가자가 고른 본인 이름. 응답, 로그, 예외에 넣지 않는다.
 * @property readContent 원본 바이트를 읽는다. 한도와 같은 키 확인을 마친 뒤 [readRawFile]에서 한 번만 부른다.
 */
class UploadAnalysisCommand(
    val participantId: UUID,
    val idempotencyKey: String,
    val source: String,
    val selfSpeaker: String,
    private val readContent: () -> ByteArray,
) {
    /** 원본을 이때 처음 메모리로 읽는다. */
    fun readRawFile(): RawChatFile = RawChatFile(DataSource.valueOf(source), readContent())
}
