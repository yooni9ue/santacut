package com.santacut.application.participant.result

/**
 * 닉네임 가용성. 확인 시점의 값일 뿐 닉네임을 예약하지 않는다.
 */
data class NicknameAvailabilityResult(
    val available: Boolean,
)
