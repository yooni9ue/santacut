package com.santacut.application.ranking.facade

import com.santacut.IntegrationTest
import com.santacut.application.ranking.command.RankingPageCommand
import com.santacut.domain.ranking.service.RankingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.BDDMockito.willThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import kotlin.test.assertNotNull

/** 앱 시작 때 장난기 기준값과 유형 분포를 다시 세다 실패해도 시작을 막지 않고, 결과 읽기는 계속 되는지 확인한다. */
@IntegrationTest
@MockitoSpyBean(types = [RankingService::class])
class ResultFacadeTest @Autowired constructor(
    private val resultFacade: ResultFacade,
    private val rankingService: RankingService,
) {
    @Test
    fun `분포를 다시 세다 실패하면 예외를 삼키고 순위 읽기는 그대로 된다`() {
        willThrow(IllegalStateException("DB 장애")).given(rankingService).refreshPopulation()

        assertDoesNotThrow { resultFacade.refreshPopulation() }

        assertNotNull(resultFacade.getRankings(RankingPageCommand(null)))
    }
}
