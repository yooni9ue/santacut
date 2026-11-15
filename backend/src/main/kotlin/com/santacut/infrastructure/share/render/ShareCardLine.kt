package com.santacut.infrastructure.share.render

import java.awt.Font

/**
 * 공유 카드 PNG에 그리는 글줄마다의 글꼴 굵기, 글자 크기, 기준선 위치(px).
 */
enum class ShareCardLine(val style: Int, val size: Int, val baseline: Int) {
    HEADLINE(Font.BOLD, 52, 95),
    NICKNAME_SCORE(Font.BOLD, 72, 220),
    RANK_VERDICT(Font.BOLD, 40, 315),
    ETA(Font.PLAIN, 34, 410),
    COMMENT(Font.PLAIN, 25, 480),
    META(Font.PLAIN, 22, 530),
    FOOTER(Font.PLAIN, 25, 590),
}
