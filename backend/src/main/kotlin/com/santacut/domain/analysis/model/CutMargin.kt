package com.santacut.domain.analysis.model

/**
 * 총점이 컷(65점)에서 떨어진 정도. 팩폭 작가 입력의 margin으로 보낸다. 숫자 대신 말만 보낸다.
 *
 * @property word 작가에게 보내는 말
 */
enum class CutMargin(val word: String) {
    AMPLE("넉넉"),
    CLOSE("아슬아슬"),
    FAR("한참"),
    ;

    companion object {
        /** 컷과의 거리가 이보다 작으면(3점 미만) 위아래 상관없이 아슬아슬이다. */
        private const val CLOSE_WITHIN_HUNDREDTHS = 300

        /** 컷 거리 [distanceHundredths](총점 - 컷, 100분의 1점)의 말. 통과했으면 넉넉, 떨어졌으면 한참이다. */
        fun of(distanceHundredths: Int): CutMargin = when {
            distanceHundredths > -CLOSE_WITHIN_HUNDREDTHS && distanceHundredths < CLOSE_WITHIN_HUNDREDTHS -> CLOSE
            distanceHundredths >= 0 -> AMPLE
            else -> FAR
        }
    }
}
