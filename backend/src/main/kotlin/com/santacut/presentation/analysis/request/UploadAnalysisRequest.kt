package com.santacut.presentation.analysis.request

import com.santacut.application.analysis.command.UploadAnalysisCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.springframework.web.bind.annotation.BindParam
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

/**
 * 원본 파일 업로드 multipart 요청. 파일 이름, 확장자, Content-Type은 쓰지 않고 내용만 넘긴다.
 */
data class UploadAnalysisRequest(
    @field:Schema(
        description = "카카오톡 대화 TXT(PC·iOS·Android 내보내기)나 인스타그램 message_1.json. 20MB 이하이며 zip은 받지 않습니다.",
        type = "string",
        format = "binary",
    )
    val file: MultipartFile,
    @field:Pattern(regexp = SubmitAnalysisRequest.SOURCE_PATTERN)
    @field:Schema(
        description = "내보내기 플랫폼",
        example = SubmitAnalysisRequest.KAKAO_SOURCE,
        allowableValues = [SubmitAnalysisRequest.KAKAO_SOURCE, SubmitAnalysisRequest.INSTAGRAM_SOURCE],
    )
    val source: String,
    @BindParam(SELF_SPEAKER_PART)
    @field:Size(min = MIN_SELF_SPEAKER_LENGTH, max = MAX_SELF_SPEAKER_LENGTH)
    @field:Schema(
        name = SELF_SPEAKER_PART,
        description = "대화에서 본인 이름. 브라우저가 파일에서 뽑은 화자 중 참가자가 고른 값을 그대로 보냅니다. 1~100자입니다. " +
            "서버는 앞뒤 공백을 떼고 NFC로 맞춘 값이 화자 이름과 완전히 같은 메시지만 분석합니다.",
        example = "산타",
        minLength = MIN_SELF_SPEAKER_LENGTH,
        maxLength = MAX_SELF_SPEAKER_LENGTH,
    )
    val selfSpeaker: String,
) {
    fun toCommand(participantId: UUID, idempotencyKey: String): UploadAnalysisCommand =
        UploadAnalysisCommand(participantId, idempotencyKey, source, selfSpeaker, file.bytes)

    companion object {
        /** 본인 이름 multipart 파트 이름. */
        const val SELF_SPEAKER_PART = "self_speaker"

        /** 본인 이름 길이의 하한과 상한(글자 수). 검증과 Swagger 스키마가 함께 쓴다. */
        const val MIN_SELF_SPEAKER_LENGTH = 1
        const val MAX_SELF_SPEAKER_LENGTH = 100
    }
}
