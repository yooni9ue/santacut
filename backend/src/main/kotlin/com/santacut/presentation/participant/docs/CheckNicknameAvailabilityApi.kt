package com.santacut.presentation.participant.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.participant.response.NicknameAvailabilityResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "checkNicknameAvailability",
    summary = "닉네임 가용성 확인",
    description = "로그인 없이 닉네임을 지금 쓸 수 있는지 확인합니다. 등록과 같은 규칙(NFKC 정규화, 대소문자 무시)으로 비교합니다. " +
        "확인 결과는 예약이 아니므로 등록할 때 다른 사람이 먼저 쓰면 NICKNAME_TAKEN이 날 수 있습니다.",
    tags = ["Identity"],
    parameters = [
        Parameter(
            name = "nickname",
            `in` = ParameterIn.QUERY,
            required = true,
            description = "확인할 닉네임. 한글, 영문, 숫자, 밑줄로 된 2~16자입니다.",
            schema = Schema(type = "string"),
        ),
    ],
    responses = [
        ApiResponse(responseCode = "200", description = "가용 여부", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = NicknameAvailabilityResponse::class))]),
        ApiResponse(responseCode = "400", description = "닉네임 형식 오류(INVALID_NICKNAME), nickname 쿼리 누락(INVALID_REQUEST)", content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class CheckNicknameAvailabilityApi
