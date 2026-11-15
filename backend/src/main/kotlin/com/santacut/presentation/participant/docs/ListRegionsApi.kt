package com.santacut.presentation.participant.docs

import com.santacut.presentation.global.advice.ApiErrorResponse
import com.santacut.presentation.global.docs.ApiDocs
import com.santacut.presentation.participant.response.RegionResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.enums.ParameterIn
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Operation(
    operationId = "listRegions",
    summary = "지역 목록",
    description = "시·도 → 시·군·구 순서로 지역을 고르게 합니다. parent 없이 부르면 최상위 지역(시·도)을, " +
        "parent에 시·도 코드를 주면 그 바로 아래 지역을 돌려줍니다. 등록에는 selectable이 true인 지역 코드만 씁니다. " +
        "구가 없는 시·도는 그 자체로 selectable이 true입니다. 모르는 parent면 빈 배열입니다. 지금 데이터는 전국 16개 시·도와 시·군·구, 모두 271개 지역입니다.",
    tags = ["Identity"],
    parameters = [
        Parameter(
            name = "parent",
            `in` = ParameterIn.QUERY,
            required = false,
            description = "상위 지역 코드(예: KR-11). 없으면 최상위 지역을 돌려줍니다.",
            schema = Schema(type = "string"),
        ),
    ],
    responses = [
        ApiResponse(
            responseCode = "200",
            description = "지역 코드 순서의 지역 목록",
            content = [Content(mediaType = ApiDocs.JSON, array = ArraySchema(schema = Schema(implementation = RegionResponse::class)))],
        ),
        ApiResponse(responseCode = "500", description = ApiDocs.SERVER_ERROR_DESCRIPTION, content = [Content(mediaType = ApiDocs.JSON, schema = Schema(implementation = ApiErrorResponse::class))]),
    ],
)
annotation class ListRegionsApi
