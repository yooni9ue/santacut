package com.santacut.domain.roast.model

/**
 * 다시 쓰기 요청. 작가 입력 JSON의 rewrite로 실려, 검증에 떨어진 필드와 그 사유를 알린다.
 * 작가는 스키마 전체를 다시 내지만 서버는 [fields]만 갈아 끼운다.
 */
data class RoastRewrite(val fields: List<String>, val reasons: List<String>)
