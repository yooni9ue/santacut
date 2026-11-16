package com.santacut.application.share.command

import java.util.UUID

/**
 * 로그인한 참가자의 최신 결과로 공유 링크를 만든다.
 *
 * @property includeQuote 본인 한마디를 공유 카드에 실을지. 기본은 싣지 않는다.
 */
data class CreateShareCommand(
    val participantId: UUID,
    val includeQuote: Boolean = false,
)
