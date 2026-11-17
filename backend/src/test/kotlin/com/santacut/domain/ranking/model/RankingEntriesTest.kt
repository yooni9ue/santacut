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
import kotlin.test.assertTrue

class RankingEntriesTest {
    private val seoul = Coordinate(37.5252, 126.9253)
    private val region = Region("KR-11-560", "영등포구", seoul.latitude, seoul.longitude)
    private val busan = Region("KR-26-110", "중구", 35.1064, 129.0324)
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
        ).toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
        assertEquals(listOf("top", "a-early", "z-early", "late"), snapshots.toList().map { it.userId })
        assertEquals(listOf(1, 2, 3, 4), snapshots.toList().map { it.deliverySequence })
        assertEquals(listOf(1, 2, 2, 2), snapshots.toList().map { it.rank })
    }

    @Test
    fun `도착 예정 시각은 출발지부터 누적한 작업 시간으로 정한다`() {
        val snapshots = RankingEntries.sorted(listOf(candidate("a", 9000, 0), candidate("b", 8000, 1)))
            .toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
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
        val snapshots = entries.toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
        assertEquals(listOf("kept"), snapshots.toList().map { it.userId })
    }

    @Test
    fun `앞이나 뒤 참가자와 점수가 같으면 공동 순위로 표시한다`() {
        val snapshots = RankingEntries.sorted(
            listOf(candidate("a", 9000, 0), candidate("b", 8000, 1), candidate("c", 8000, 2), candidate("d", 7000, 3)),
        ).toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
        assertEquals(listOf(false, true, true, false), snapshots.toList().map { it.tied })
    }

    @Test
    fun `점수가 같으면 배려·공감, 존중, 응원, 협력 점수 순으로 순위를 가르고 공동 순위가 아니다`() {
        val snapshots = RankingEntries.sorted(
            listOf(
                candidate("respect", 8000, 0, "[70,60,90,40]"),
                candidate("care", 8000, 5, "[71,50,50,50]"),
                candidate("support", 8000, 1, "[70,90,80,40]"),
            ),
        ).toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
        assertEquals(listOf("care", "respect", "support"), snapshots.toList().map { it.userId })
        assertEquals(listOf(1, 2, 3), snapshots.toList().map { it.rank })
        assertEquals(listOf(false, false, false), snapshots.toList().map { it.tied })
    }

    @Test
    fun `모든 점수가 같으면 최근 재심사 시각이 아니라 시즌 첫 심사 성공 시각이 이른 참가자가 먼저 받는다`() {
        val rescored = RankingCandidate(
            assessment("rescored", 8000, 600), participant("rescored"), region, firstAssessedAt,
        )
        val snapshots = RankingEntries.sorted(listOf(candidate("newcomer", 8000, 30), rescored))
            .toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
        assertEquals(listOf("rescored", "newcomer"), snapshots.toList().map { it.userId })
        assertEquals(listOf(true, true), snapshots.toList().map { it.tied })
    }

    @Test
    fun `참가자가 10명, 1000명, 10만 명이어도 모두 배송 기간 안에 배송 순서대로 도착한다`() {
        val windowEnd = deliveryStart.plusDays(DELIVERY_DAYS - 1L).plusHours(6).toInstant()
        listOf(10, 1_000, 100_000).forEach { count ->
            val etas = RankingEntries.sorted(crowd(count)).toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
                .toList().map { it.eta }
            assertTrue(etas.all { eta -> eta.isAfter(deliveryStart.toInstant()) && !eta.isAfter(windowEnd) }, "참가자 ${count}명")
            assertEquals(etas.sorted(), etas, "참가자 ${count}명")
        }
    }

    @Test
    fun `참가자가 많아 기간을 줄여도 1위와 꼴찌의 도착일은 다르다`() {
        val etas = RankingEntries.sorted(crowd(100_000)).toSnapshots(seoul, deliveryStart, computedAt, DELIVERY_DAYS)
            .toList().map { it.eta }
        assertEquals(deliveryStart.toLocalDate(), etas.first().atZone(deliveryStart.zone).toLocalDate())
        assertEquals(deliveryStart.toLocalDate().plusDays(DELIVERY_DAYS - 1L), etas.last().atZone(deliveryStart.zone).toLocalDate())
    }

    /** 서울과 부산을 번갈아 가는 참가자 [count]명. 점수는 모두 달라 배송 순서가 정해진다. */
    private fun crowd(count: Int): List<RankingCandidate> = (0 until count).map { index ->
        val home = if (index % 2 == 0) region else busan
        RankingCandidate(assessment("u$index", 10_000 - index % 10_000, 0), participant("u$index"), home, firstAssessedAt)
    }

    private fun candidate(userId: String, score: Int, assessedAfterMinutes: Long, components: String = "[]"): RankingCandidate {
        val assessment = assessment(userId, score, assessedAfterMinutes, components)
        return RankingCandidate(assessment, participant(userId), region, assessment.createdAt)
    }

    private fun assessment(userId: String, score: Int, assessedAfterMinutes: Long, components: String = "[]"): Assessment =
        Assessment(
            "assessment-$userId", userId, DataSource.KAKAO, score, "판정", "한마디", components,
            firstAssessedAt.plusSeconds(assessedAfterMinutes * 60),
        )

    private fun participant(userId: String): Participant =
        Participant(userId, userId, userId, region.code, firstAssessedAt)

    companion object {
        private const val DELIVERY_DAYS = 7
    }
}
