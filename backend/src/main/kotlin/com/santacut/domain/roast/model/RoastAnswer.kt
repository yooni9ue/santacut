package com.santacut.domain.roast.model

import com.santacut.domain.analysis.model.ModelUsage

/**
 * 팩폭 작가 호출 한 번의 응답 본문과 사용량. 호출이 실패했거나 본문이 없으면 [text]가 null이다.
 *
 * @property failed 호출 자체가 실패했는지(제공자 오류, 한도 초과, 인증 실패, 시간 초과). 다시 써도 같은 실패라 다시 쓰지 않는다.
 */
data class RoastAnswer(val text: String?, val usage: ModelUsage, val failed: Boolean = false)
