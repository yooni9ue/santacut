package com.santacut.domain.analysis.model

/**
 * 원본 파일 형식. 카카오톡 TXT는 처음 맞은 메시지 줄의 형식으로 정하고, 그 뒤로는 같은 형식의 줄만 메시지로 읽는다.
 */
enum class ChatFileFormat {
    /** 카카오톡 PC 내보내기. 날짜 구분선과 `[이름] [오후 3:00] 본문` 줄로 되어 있다. */
    KAKAO_PC_TXT,

    /** 카카오톡 iOS·Android 내보내기. 줄마다 날짜, 시각, `이름 : 본문`이 있다. */
    KAKAO_MOBILE_TXT,
}
