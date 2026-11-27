package com.santacut.domain.roast.model

/**
 * 한 사람의 구간이 쓸 수 없는 다른 구간의 표현 목록([BandMarks.foreignTo])과 그 사람 구간의 이름. 다른 구간 표현은 판정 한 줄·결론 칭호·마지막 한 줄 검사에 쓰고
 * 작가 입력에도 싣는다. 구간 이름은 칭호가 구간 이름을 되풀이하는지('명단 실종자') 볼 때만 쓴다. 본인 입버릇을 따옴표로 든 것은 작가 말이 아니라 보지 않는다.
 *
 * @property ownNames 이 사람 구간의 이름 낱말(roast/punch.json의 band_names)
 */
class ForeignMarks(private val values: List<BandMark>, private val ownNames: List<String> = emptyList()) {
    /** 작가 입력의 banned_marks로 싣는 표현 목록. */
    fun toList(): List<String> = values.map(BandMark::mark)

    /** [text]의 따옴표 인용 밖에 처음 나온 다른 구간 표현. 없으면 null이다. */
    fun foundIn(text: String): String? {
        val bare = TextProblems.QUOTE.replace(text, "")
        return values.firstOrNull { mark -> mark.isIn(bare) }?.mark
    }

    /** 칭호 [title]에 띄어쓰기를 무시하고 처음 나온 이 사람 구간의 이름. 없으면 null이다. */
    fun ownNameIn(title: String): String? = ownNames.firstOrNull { name -> name.replace(SPACE, "") in title.replace(SPACE, "") }

    companion object {
        val EMPTY = ForeignMarks(emptyList())
        private const val SPACE = " "
    }
}
