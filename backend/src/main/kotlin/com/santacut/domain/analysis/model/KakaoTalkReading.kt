package com.santacut.domain.analysis.model

import java.time.LocalDate

/**
 * 카카오톡 TXT를 한 줄씩 읽는 중간 상태. 정해진 형식, PC 날짜 구분선이 알려 준 날짜, 아직 끝나지 않은 메시지를 들고 있다.
 * 메시지가 끝날 때마다 [read]·[finish]에 넘긴 함수로 내보낸다.
 */
data class KakaoTalkReading(
    private val format: ChatFileFormat?,
    private val date: LocalDate?,
    private val pending: ChatMessage?,
) {
    /**
     * 한 줄을 읽는다. 날짜 구분선은 날짜만 바꾸고, 메시지 줄은 앞 메시지를 끝내고 새 메시지를 연다.
     * 모바일 날짜 구분선과 시스템 줄은 버리고, 그 밖의 줄은 앞 메시지 본문에 잇는다. 첫 메시지 전 줄은 버린다.
     */
    fun read(line: KakaoTalkLine, emit: (ChatMessage) -> Unit): KakaoTalkReading {
        if (format != ChatFileFormat.KAKAO_MOBILE_TXT) line.pcDate()?.let { return copy(date = it) }
        pcMessage(line)?.let { return startMessage(ChatFileFormat.KAKAO_PC_TXT, it, emit) }
        mobileMessage(line)?.let { return startMessage(ChatFileFormat.KAKAO_MOBILE_TXT, it, emit) }
        if (format != ChatFileFormat.KAKAO_PC_TXT && line.isMobileNonMessage()) return this
        return copy(pending = pending?.let(line::appendTo))
    }

    /** 마지막 메시지를 뒤 빈 줄을 떼고 내보낸다. 사진·동영상·이모티콘·삭제 안내만 있는 메시지는 버린다. */
    fun finish(emit: (ChatMessage) -> Unit) {
        pending?.withoutTrailingBlankLines()?.takeUnless { it.hasTextIn(PLACEHOLDER_TEXTS) }?.let(emit)
    }

    private fun pcMessage(line: KakaoTalkLine): ChatMessage? =
        if (format == ChatFileFormat.KAKAO_MOBILE_TXT) null else date?.let(line::pcMessage)

    private fun mobileMessage(line: KakaoTalkLine): ChatMessage? =
        if (format == ChatFileFormat.KAKAO_PC_TXT) null else line.mobileMessage()

    private fun startMessage(matched: ChatFileFormat, message: ChatMessage, emit: (ChatMessage) -> Unit): KakaoTalkReading {
        finish(emit)
        return copy(format = matched, pending = message)
    }

    companion object {
        val START = KakaoTalkReading(format = null, date = null, pending = null)

        /** 본문이 정확히 이 값이면 첨부나 삭제 안내라서 버린다. */
        private val PLACEHOLDER_TEXTS = setOf("사진", "동영상", "이모티콘", "삭제된 메시지입니다.")
    }
}
