package com.santacut.domain.dossier.model

/**
 * 메시지에서 센 조서 집계값과 톤. 위기 표현 횟수는 톤을 정하는 데만 쓰고 담지 않는다.
 */
data class DossierTally(val stats: DossierStats, val tone: DossierTone)
