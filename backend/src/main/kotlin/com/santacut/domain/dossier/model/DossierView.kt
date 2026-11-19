package com.santacut.domain.dossier.model

/**
 * 조회 시점에 조립한 조서. [title]은 칭호다.
 */
data class DossierView(val title: String, val tone: DossierTone, val scenes: DossierScenes)
