package com.santacut.domain.roast.model

/**
 * 원본 파일에서 읽은 대화 참여자 표시 이름(본인 포함)으로 만든 가릴 이름 목록. 작가 입력에서 가리고 출력 차단 목록에 넣는다.
 * 로그·응답·예외에 넣지 않고 저장하지 않는다.
 *
 * 이름마다 표시 이름 그대로, 참여자 모두에게 붙은 공통 머리말('평친동준', '평친성필'의 '평친')을 뗀 이름,
 * 한글 세 글자 이상이면 끝 두 글자(이름 부분)를 넣는다. 조사·부르는 말('성필이', '성필아', '성필이랑')은
 * 낱말 앞 경계에서 뒤에 무엇이 붙든 가리는 패턴([RoastRules.namePattern])이 덮는다.
 */
class ParticipantNames(private val values: List<String>) {
    fun toList(): List<String> = values.toList()

    fun isEmpty(): Boolean = values.isEmpty()

    companion object {
        val EMPTY = ParticipantNames(emptyList())
        private const val MIN_LENGTH = 2
        private const val GIVEN_NAME_LENGTH = 2
        private const val FULL_NAME_LENGTH = 3

        /** 표시 이름 [speakers]로 가릴 이름을 만든다. 두 글자보다 짧은 이름은 흔한 글자를 가리지 않게 뺀다. */
        fun of(speakers: List<String>): ParticipantNames {
            val names = speakers.map(String::trim).filter(String::isNotEmpty).distinct()
            val prefix = commonPrefix(names)
            val variants = names.flatMap { name ->
                val bare = name.removePrefix(prefix)
                val hangul = bare.filter { character -> character in HANGUL }
                listOfNotNull(name, bare, hangul.takeLast(GIVEN_NAME_LENGTH).takeIf { hangul.length >= FULL_NAME_LENGTH })
            }
            return ParticipantNames(variants.filter { name -> name.length >= MIN_LENGTH }.distinct())
        }

        /** 참여자가 둘 이상이고 모두 같은 머리말로 시작하면 그 머리말. 이름이 통째로 머리말이 되지 않게 가장 짧은 이름보다 짧게 자른다. */
        private fun commonPrefix(names: List<String>): String {
            if (names.size < 2) return ""
            val shortest = names.minOf(String::length)
            val length = (0 until shortest - 1).takeWhile { index -> names.all { name -> name[index] == names[0][index] } }.count()
            return names[0].take(length)
        }

        private val HANGUL = '가'..'힣'
    }
}
