package com.santacut.application.dossier.facade

import com.santacut.application.dossier.command.MyDossierCommand
import com.santacut.application.dossier.result.DossierResult
import com.santacut.application.global.Facade
import com.santacut.domain.dossier.model.DossierSubject
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.ranking.service.RankingService
import org.springframework.transaction.annotation.Transactional

/**
 * 「2026 산타 조서」 조회. 참가자의 최신 평가의 조서를 그 평가의 점수·순위와 함께 조립한다.
 * 재업로드 직후 순위 다시 계산(최대 30초) 전에도 새 평가의 조서를 보여 주고, 그동안 순위는 비워 둔다.
 * 결과 화면은 그동안 지난 평가의 점수·순위를 보여 주므로 `ranking_pending`이 true인 동안은 둘이 다를 수 있다.
 */
@Facade
class DossierFacade(
    private val rankingService: RankingService,
    private val dossierService: DossierService,
) {
    /** 최신 결과 찾기 → 그 결과의 조서 찾기 → 기존 결과와 함께 장면 조립. 결과가 없으면 RESULT_NOT_FOUND, 조서가 없으면 DOSSIER_NOT_FOUND다. */
    @Transactional(readOnly = true)
    fun getMyDossier(command: MyDossierCommand): DossierResult {
        val result = rankingService.findLatestResult(command.participantId)
        val dossier = dossierService.get(result.latest.id)
        val snapshot = result.latestSnapshot()
        val subject = DossierSubject.of(result.latest, snapshot?.rank, snapshot?.eta, dossierService.regionNameOf(command.participantId))
        return DossierResult.from(dossierService.write(dossier, subject))
    }
}
