package com.santacut.domain.dossier.model

/**
 * 메시지에서 센 조서 집계값과 톤. 위기 표현 횟수는 톤을 정하는 데만 쓰고 담지 않는다.
 * 원본 파일 제출에서는 작업 페이로드에도 실린다([com.santacut.domain.analysis.model.AnalysisSubmission.dossier]).
 */
data class DossierTally(val stats: DossierStats, val tone: DossierTone) {
    /** 지금 집계 규칙 판으로 센 값인지. 배포 전에 대기열에 들어간 작업의 집계는 판이 다를 수 있다. */
    fun isCurrentVersion(): Boolean = stats.version == DossierStats.VERSION

    /** 저장할 조서. [statsJson]은 [stats]를 JSON으로 쓴 것이다. */
    fun toDraft(statsJson: String): DossierDraft = DossierDraft(statsJson, tone, stats.version)
}
