package com.santacut.domain.roast.model

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * 다시 쓰기 요청. 작가 입력 JSON의 rewrite로 실려, 검증에 떨어진 필드와 그 사유를 알린다.
 * 작가는 스키마 전체를 다시 내지만 서버는 [fields]만 갈아 끼운다. [kept]는 이미 받아 둔 문장이라 새로 쓰는 묶음이 이것과 겹치면 받지 않는다.
 * [reasons]에는 검증 사유뿐 아니라, 검증은 통과했지만 받아 둔 문장과 겹쳐 받지 않은 묶음·카드의 사유도 든다.
 */
data class RoastRewrite(
    val fields: List<String>,
    val reasons: List<String>,
    @field:JsonInclude(JsonInclude.Include.NON_NULL) val kept: RoastKept? = null,
)
