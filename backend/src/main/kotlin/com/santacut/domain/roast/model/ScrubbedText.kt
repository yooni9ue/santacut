package com.santacut.domain.roast.model

/**
 * 사람 이름·사는 곳 이름·숫자·영문을 일반명사로 가린 본문.
 *
 * @property masked 가린 원래 낱말. 그 요청의 출력 검증 차단 목록으로만 쓰고 저장하지 않는다.
 * @property names 호칭·부르는 말에서 떼어 낸 이름 줄기('현규형'의 '현규'). 모든 줄을 다시 가리고 차단 목록에 넣는다.
 */
data class ScrubbedText(val text: String, val masked: List<String>, val names: List<String> = emptyList())
