package com.santacut.application.roast.facade

import com.santacut.application.global.Facade
import com.santacut.application.roast.command.MyRecapCommand
import com.santacut.application.roast.result.RecapResult
import com.santacut.domain.ranking.service.RankingService
import com.santacut.domain.roast.service.RoastService
import org.springframework.transaction.annotation.Transactional

/**
 * 「팩폭 산타」 리캡 카드 덱 조회. 참가자의 최신 평가와 그 평가의 팩폭 문장, 순위, 꾸밈 값을 묶는다.
 * 재업로드 직후 순위 다시 계산 전에도 새 평가의 카드를 보여 주고, 그동안 순위는 비워 둔다.
 */
@Facade
class RecapFacade(
    private val rankingService: RankingService,
    private val roastService: RoastService,
) {
    /** 최신 결과 찾기 → 그 결과의 팩폭 문장 찾기 → 순위·꾸밈 값과 함께 카드 값 만들기. 결과가 없으면 RESULT_NOT_FOUND, 팩폭 문장이 없으면 RECAP_NOT_FOUND다. */
    @Transactional(readOnly = true)
    fun getMyRecap(command: MyRecapCommand): RecapResult {
        val result = rankingService.findLatestResult(command.participantId)
        val content = roastService.get(result.latest.id)
        val snapshot = result.latestSnapshot()
        return RecapResult.of(result.latest, snapshot, rankingService.findProfile(result.latest, snapshot), content)
    }
}
