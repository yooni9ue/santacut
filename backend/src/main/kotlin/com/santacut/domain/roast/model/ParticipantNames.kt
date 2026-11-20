package com.santacut.domain.roast.model

/**
 * 원본 파일에서 읽은 대화 참여자 표시 이름(본인 포함)으로 만든 가릴 이름 목록. 작가 입력에서 가리고 출력 차단 목록에 넣는다.
 * 로그·응답·예외에 넣지 않고 저장하지 않는다.
 *
 * 한글 음절이 없는 표시 이름('ㅇㅇ', 'Kim')은 뺀다. 나머지는 표시 이름 그대로와, 표시 이름을 공백·한글 아닌 글자로 나눈 한글 덩이에서 만든 이름을 넣는다.
 * - 두 글자 덩이: 그대로('민수❤'의 '민수')
 * - 세 글자 덩이: 그대로, 첫 글자가 흔한 성이면 끝 두 글자도('김민수'의 '민수')
 * - 네 글자 넘는 덩이: 그대로와 끝 두 글자('평친성필'의 '성필')
 * 참여자 모두에게 붙은 공통 머리말('평친')은 뗀 뒤에도 두 글자 넘게 남을 때만 떼고, 뗀 이름도 같은 규칙으로 넣는다.
 * 직함·호칭 같은 흔한 말([exceptions], 예: '김민수 대리'의 '대리')은 넣지 않는다.
 * 조사·부르는 말('성필이', '성필아')은 낱말 앞 경계에서 뒤에 무엇이 붙든 가리는 패턴([RoastRules.namePattern])이 덮는다.
 */
class ParticipantNames(private val values: List<String>) {
    fun toList(): List<String> = values.toList()

    fun isEmpty(): Boolean = values.isEmpty()

    companion object {
        val EMPTY = ParticipantNames(emptyList())
        private const val MIN_LENGTH = 2
        private const val GIVEN_NAME_LENGTH = 2
        private const val FULL_NAME_LENGTH = 3
        private val HANGUL = '가'..'힣'
        private val NON_HANGUL = Regex("[^가-힣]+")

        /** 표시 이름 [speakers]로 가릴 이름을 만든다. [surnames]는 흔한 성, [exceptions]는 이름으로 넣지 않을 흔한 말이다. */
        fun of(speakers: List<String>, surnames: Set<Char>, exceptions: Set<String>): ParticipantNames {
            val names = speakers.map(String::trim).filter { name -> name.any { character -> character in HANGUL } }.distinct()
            val prefix = commonPrefix(names)
            val variants = names.flatMap { name ->
                listOf(name, name.removePrefix(prefix)).flatMap { source -> listOf(source) + chunkNames(source, surnames) }
            }
            return ParticipantNames(variants.filter { name -> name.length >= MIN_LENGTH && name !in exceptions }.distinct())
        }

        private fun chunkNames(source: String, surnames: Set<Char>): List<String> =
            source.split(NON_HANGUL).filter(String::isNotEmpty).flatMap { chunk ->
                when {
                    chunk.length == GIVEN_NAME_LENGTH -> listOf(chunk)
                    chunk.length == FULL_NAME_LENGTH && chunk.first() in surnames -> listOf(chunk, chunk.takeLast(GIVEN_NAME_LENGTH))
                    chunk.length > FULL_NAME_LENGTH -> listOf(chunk, chunk.takeLast(GIVEN_NAME_LENGTH))
                    else -> listOf(chunk)
                }
            }

        /** 참여자가 둘 이상이고 모두 같은 머리말로 시작하면 그 머리말. 뗀 뒤 모든 이름에 두 글자 넘게 남도록 줄인다. */
        private fun commonPrefix(names: List<String>): String {
            if (names.size < 2) return ""
            val shared = (0 until names.minOf(String::length)).takeWhile { index -> names.all { name -> name[index] == names[0][index] } }.count()
            val keepable = names.minOf(String::length) - MIN_LENGTH
            return names[0].take(minOf(shared, maxOf(keepable, 0)))
        }
    }
}
