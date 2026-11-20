package com.santacut.domain.roast.model

/**
 * 고유명사·숫자·영문을 일반명사로 가린 본문.
 *
 * @property masked 가린 원래 낱말. 그 요청의 출력 검증 차단 목록으로만 쓰고 저장하지 않는다.
 */
data class ScrubbedText(val text: String, val masked: List<String>)
