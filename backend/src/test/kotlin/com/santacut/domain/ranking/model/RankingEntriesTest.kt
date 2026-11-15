package com.santacut.domain.ranking.model

import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.Assessments
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.global.model.Coordinate
import com.santacut.domain.participant.model.Participant
import com.santacut.domain.participant.model.Participants
import com.santacut.domain.participant.model.Region
import com.santacut.domain.participant.model.Regions
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals

class RankingEntriesTest {
    private val seoul = Coordinate(37.5252, 126.9253)
    private val region = Region("KR-11-560", "영등포구", seoul.latitude, seoul.longitude)
    private val deliveryStart = ZonedDateTime.of(2026, 12, 25, 0, 0, 0, 0, ZoneId.of("Asia/Seoul"))
    private val computedAt = Instant.parse("2026-12-01T00:00:00Z")
    private val firstAssessedAt = Instant.parse("2026-11-01T00:00:00Z")

    @Test
    fun `점수가 같으면 같은 순위를 받고 다음 순위는 그만큼 건너뛴다`() {
        val entries = RankingEntries.sorted(
            listOf(candidate("a", 9000, 0), candidate("b", 8000, 1), candidate("c", 8000, 2), candidate("d", 7000, 3)),
        )
        assertEquals(listOf(1, 2, 2, 4), entries.competitionRanks())
    }

    @Test
    fun `배송 순서는 점수 내림차순, 같으면 먼저 평가받은 순, 그다음 참가자 ID 순이다`() {
        val snapshots = RankingEntries.sorted(
            listOf(candidate("late", 8000, 5), candidate("z-early", 8000, 1), candidate("a-early", 8000, 1), candidate("top", 9500, 9)),
        ).toSnapshots(seoul, deliveryStart, computedAt)
        assertEquals(listOf("top", "a-early", "z-early", "late"), snapshots.toList().map { it.userId })
        assertEquals(listOf(1, 2, 3, 4), snapshots.toList().map { it.deliverySequence })
        assertEquals(listOf(1, 2, 2, 2), snapshots.toList().map { it.rank })
    }

    @Test
    fun `도착 예정 시각은 출발지부터 누적한 작업 시간으로 정한다`() {
        val snapshots = RankingEntries.sorted(listOf(candidate("a", 9000, 0), candidate("b", 8000, 1)))
            .toSnapshots(seoul, deliveryStart, computedAt)
        val firstStop = deliveryStart.plusSeconds(180).toInstant()
        assertEquals(listOf(firstStop, firstStop.plusSeconds(180)), snapshots.toList().map { it.eta })
        assertEquals(listOf(computedAt, computedAt), snapshots.toList().map { it.computedAt })
    }

    @Test
    fun `참가자나 지역이 사라진 평가는 순위에서 빠진다`() {
        val kept = participant("kept")
        val orphan = Participant("orphan", "orphan", "orphan", "KR-00-000", firstAssessedAt)
        val entries = Assessments(listOf(assessment("kept", 9000, 0), assessment("orphan", 8000, 0), assessment("ghost", 7000, 0)))
            .toRankingEntries(Participants(listOf(kept, orphan)), Regions(listOf(region)))
        val snapshots = entries.toSnapshots(seoul, deliveryStart, computedAt)
        assertEquals(listOf("kept"), snapshots.toList().map { it.userId })
    }

    private fun candidate(userId: String, score: Int, assessedAfterMinutes: Long): RankingCandidate =
        RankingCandidate(assessment(userId, score, assessedAfterMinutes), participant(userId), region)

    private fun assessment(userId: String, score: Int, assessedAfterMinutes: Long): Assessment = Assessment(
        "assessment-$userId", userId, DataSource.KAKAO, score, "판정", "한마디", "[]",
        firstAssessedAt.plusSeconds(assessedAfterMinutes * 60),
    )

    private fun participant(userId: String): Participant =
        Participant(userId, userId, userId, region.code, firstAssessedAt)
}
