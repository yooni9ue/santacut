package com.santacut.application.dossier.facade

import com.santacut.application.dossier.command.MyDossierCommand
import com.santacut.application.dossier.result.DossierResult
import com.santacut.application.global.Facade
import com.santacut.domain.dossier.model.DossierSubject
import com.santacut.domain.dossier.service.DossierService
import com.santacut.domain.ranking.service.RankingService
import org.springframework.transaction.annotation.Transactional

/**
 * 「2026 산타 조서」 조회. 결과 화면과 같은 평가(순위에 반영된 평가, 없으면 최신 평가)의 조서를 그 평가의 점수·순위와 함께 조립한다.
 */
@Facade
class DossierFacade(
    private val rankingService: RankingService,
    private val dossierService: DossierService,
) {
    /** 결과 찾기 → 그 결과의 조서 찾기 → 기존 결과와 함께 장면 조립. 결과가 없으면 RESULT_NOT_FOUND, 조서가 없으면 DOSSIER_NOT_FOUND다. */
    @Transactional(readOnly = true)
    fun getMyDossier(command: MyDossierCommand): DossierResult {
        val result = rankingService.findLatestResult(command.participantId)
        val dossier = dossierService.get(result.assessment.id)
        val subject = DossierSubject.of(
            result.assessment, result.snapshot?.rank, result.snapshot?.eta, dossierService.regionNameOf(command.participantId),
        )
        return DossierResult.from(dossierService.write(dossier, subject))
    }
}
