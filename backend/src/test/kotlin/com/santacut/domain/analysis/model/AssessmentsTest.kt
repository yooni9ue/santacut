package com.santacut.domain.analysis.model

import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.participant.model.Participants
import com.santacut.domain.participant.model.Region
import com.santacut.domain.participant.model.Regions
import com.santacut.domain.ranking.model.RankingEntries
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AssessmentsTest {
    private val region = Region("KR-11-110", "종로구", 37.5729, 126.9794)
    private val origin = Coordinate(37.5252, 126.9253)
    private val deliveryStart = ZonedDateTime.of(2026, 12, 25, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    private val assessedAt = Instant.parse("2026-11-01T00:00:00Z")

    @Test
    fun `참가자와 지역이 모두 있는 평가를 점수 높은 순으로 줄 세운다`() {
        val assessments = Assessments(listOf(assessment("low", 7000), assessment("high", 9000)))
        val entries = assessments.toRankingEntries(
            Participants(listOf(participant("low", region.code), participant("high", region.code))),
            Regions(listOf(region)),
        )
        assertEquals(listOf("high", "low"), userIdsOf(entries))
    }

    @Test
    fun `참가자가 없거나 참가자의 지역이 없는 평가는 빠진다`() {
        val assessments = Assessments(listOf(assessment("kept", 9000), assessment("ghost", 8000), assessment("lost", 7000)))
        val entries = assessments.toRankingEntries(
            Participants(listOf(participant("kept", region.code), participant("lost", "KR-00-000"))),
            Regions(listOf(region)),
        )
        assertEquals(listOf("kept"), userIdsOf(entries))
    }

    @Test
    fun `평가가 없으면 순위 후보도 없다`() {
        val entries = Assessments(emptyList()).toRankingEntries(Participants(emptyList()), Regions(emptyList()))
        assertTrue(userIdsOf(entries).isEmpty())
    }

    private fun userIdsOf(entries: RankingEntries): List<String> =
        entries.toSnapshots(origin, deliveryStart, assessedAt).toList().map { it.userId }

    private fun assessment(userId: String, score: Int): Assessment =
        Assessment("assessment-$userId", userId, DataSource.KAKAO, score, "판정", "한마디", "[]", assessedAt)

    private fun participant(userId: String, regionCode: String): Participant =
        Participant(userId, userId, userId, regionCode, assessedAt)
}
