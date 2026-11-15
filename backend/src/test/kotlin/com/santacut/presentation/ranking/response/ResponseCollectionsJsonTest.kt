package com.santacut.presentation.ranking.response

import com.santacut.application.participant.result.RegionResult
import com.santacut.application.participant.result.RegionResults
import com.santacut.application.ranking.result.RankingRowResult
import com.santacut.application.ranking.result.RankingRowResults
import com.santacut.presentation.participant.response.RegionResponses
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.assertEquals

class ResponseCollectionsJsonTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `지역 목록 응답은 JSON 배열 하나로 나간다`() {
        val responses = RegionResponses.from(RegionResults(listOf(RegionResult("KR-11-110", "종로구", "KR-11", true))))
        assertEquals("""[{"code":"KR-11-110","name":"종로구","parent_code":"KR-11","selectable":true}]""", mapper.writeValueAsString(responses))
    }

    @Test
    fun `순위표 응답은 순서를 지킨 JSON 배열 하나로 나간다`() {
        val eta = Instant.parse("2026-12-25T00:03:00Z")
        val results = RankingRowResults(
            listOf(RankingRowResult("첫째", 1, false, 8000, eta), RankingRowResult("둘째", 2, false, 7000, eta)),
        )
        val expected = listOf(
            RankingResponse("첫째", 1, false, 8000, eta.toString()),
            RankingResponse("둘째", 2, false, 7000, eta.toString()),
        )
        assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(RankingResponses.from(results)))
    }
}
