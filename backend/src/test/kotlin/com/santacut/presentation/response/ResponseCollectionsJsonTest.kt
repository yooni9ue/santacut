package com.santacut.presentation.response

import com.santacut.application.result.RankingRowResult
import com.santacut.application.result.RankingRowResults
import com.santacut.application.result.RegionResult
import com.santacut.application.result.RegionResults
import org.junit.jupiter.api.Test
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.Instant
import kotlin.test.assertEquals

class ResponseCollectionsJsonTest {
    private val mapper = jacksonObjectMapper()

    @Test
    fun `지역 목록 응답은 JSON 배열 하나로 나간다`() {
        val responses = RegionResponses.from(RegionResults(listOf(RegionResult("KR-11-110", "종로구"))))
        assertEquals("""[{"code":"KR-11-110","name":"종로구"}]""", mapper.writeValueAsString(responses))
    }

    @Test
    fun `순위표 응답은 순서를 지킨 JSON 배열 하나로 나간다`() {
        val computedAt = Instant.parse("2026-12-01T00:00:00Z")
        val eta = Instant.parse("2026-12-25T00:03:00Z")
        val results = RankingRowResults(
            listOf(RankingRowResult("첫째", 1, 8000, eta, computedAt), RankingRowResult("둘째", 2, 7000, eta, computedAt)),
        )
        val expected = listOf(
            RankingResponse("첫째", 1, 8000, eta.toString(), computedAt.toString()),
            RankingResponse("둘째", 2, 7000, eta.toString(), computedAt.toString()),
        )
        assertEquals(mapper.writeValueAsString(expected), mapper.writeValueAsString(RankingResponses.from(results)))
    }
}
