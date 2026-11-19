package com.santacut.domain.dossier.model

/**
 * 조서 집계값(저장용 stats_json). 본인 메시지만 세고, 상대 메시지는 답장 간격의 시각에만 쓴다.
 * 원문, 이름, 링크, 전화번호는 담지 않는다. 입버릇은 허용 어휘의 key와 표기, 날짜는 월과 시간대까지만 담는다.
 *
 * @property version 집계 규칙 판([VERSION]). 규칙이 바뀌면 올리고, 같은 입력 재사용 때 판이 다르면 다시 센다.
 */
data class DossierStats(
    val version: Int,
    val volume: VolumeStats,
    val laugh: LaughStats,
    val emoji: EmojiStats,
    val clock: ClockStats,
    val reply: ReplyStats,
    val style: MessageStyle,
    val words: WordStats,
) {
    /** 입버릇을 지금 사전 [entries]로 다시 읽은 집계. 조회할 때 쓴다. */
    fun refreshedBy(entries: PhraseEntries): DossierStats = copy(words = words.refreshedBy(entries))

    /** 본인 메시지 수. */
    fun messageCount(): Int = volume.messageCount

    /** 자리표시를 뺀 본인 글 메시지 수. 밀도의 분모다. */
    fun textCount(): Int = volume.textCount

    companion object {
        const val VERSION = 2
    }
}
