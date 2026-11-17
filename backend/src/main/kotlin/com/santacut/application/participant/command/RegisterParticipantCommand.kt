package com.santacut.application.participant.command

/**
 * 참가자를 등록하고 세션을 연다.
 *
 * @property clientIp 요청한 클라이언트 IP(믿는 프록시 헤더를 반영한 값). HTTP 밖에서 부르면 null이고 IP 한도를 세지 않는다.
 */
data class RegisterParticipantCommand(
    val nickname: String,
    val regionCode: String,
    val clientIp: String? = null,
)
