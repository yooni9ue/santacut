package com.santacut.application.roast.result

import com.santacut.domain.dossier.model.DossierAct
import com.santacut.domain.roast.model.RoastStoryLine

/** 리캡 줄거리 카드의 막 하나. [months]는 "1~4월" 같은 달 범위다. */
data class RecapStoryResult(val act: Int, val months: String, val line: String) {
    companion object {
        fun from(line: RoastStoryLine): RecapStoryResult = RecapStoryResult(line.act, DossierAct.entries[line.act - 1].monthsText(), line.line)
    }
}
