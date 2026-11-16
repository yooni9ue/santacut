package com.santacut.domain.analysis.model

import java.time.LocalDateTime

/**
 * 채점 대화문을 만들 때 쓰는 메시지 하나. [localTime]은 한국 시각이다.
 *
 * @property speaker 화자 표시. 본인은 [SELF], 다른 참여자는 `상대`나 `상대1`, `상대2`다. 실제 이름은 담지 않는다.
 */
data class ScoringMessage(
    val localTime: LocalDateTime,
    val text: String,
    val speaker: String = SELF,
) {
    fun isSelf(): Boolean = speaker == SELF

    /** 사진·동영상·이모티콘·파일 첨부나 삭제 안내처럼 본문이 없는 메시지. 대화문에서 뺀다. */
    fun isPlaceholder(): Boolean = PLACEHOLDER.matches(text.trim())

    /**
     * 뜻을 잃지 않게 줄인 본문. 줄바꿈과 겹친 공백을 공백 하나로 바꾸고, ㅋ·ㅎ는 세 개, ㅠ·ㅜ와 문장 부호는 두 개까지만 남긴다.
     */
    fun normalizedText(): String = text
        .replace(WHITESPACE, SPACE)
        .replace(REPEATED_LAUGH, "$1$1$1")
        .replace(REPEATED_CRY_OR_MARK, "$1$1")
        .trim()

    companion object {
        /** 본인 화자 표시. */
        const val SELF = "나"

        private const val SPACE = " "
        private val PLACEHOLDER = Regex("""사진( \d+장)?|동영상|이모티콘|파일(: .*)?|음성메시지|삭제된 메시지입니다\.|메시지가 삭제되었습니다\.""")
        private val WHITESPACE = Regex("""\s+""")
        private val REPEATED_LAUGH = Regex("""([ㅋㅎ])\1{3,}""")
        private val REPEATED_CRY_OR_MARK = Regex("""([ㅠㅜ!?.~])\1{2,}""")
    }
}
