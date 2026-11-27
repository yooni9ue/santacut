package com.santacut.application.roast.result

import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.Assessment
import com.santacut.domain.analysis.model.Assessments
import com.santacut.domain.analysis.model.DataSource
import com.santacut.domain.analysis.model.GiftVerdict
import com.santacut.domain.analysis.model.ScoreDimension
import com.santacut.domain.ranking.model.ResultPopulation
import com.santacut.domain.ranking.model.ResultProfile
import com.santacut.domain.ranking.model.ResultSnapshot
import com.santacut.domain.roast.model.EvidenceKind
import com.santacut.domain.roast.model.RoastChip
import com.santacut.domain.roast.model.RoastContent
import com.santacut.domain.roast.model.RoastEvidence
import com.santacut.domain.roast.model.RoastEvidences
import com.santacut.domain.roast.model.RoastSource
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 리캡 덱 순서와 장수, 모든 카드의 상단 바. */
class RecapResultTest {
    private val fallback = TestRoasts.catalog.fallback

    @Test
    fun `선물 덱은 판정, 컷 표, 이유 셋, 깎인 말 둘, 결론으로 최대 여덟 장이다`() {
        val result = resultOf(GIFT, "[90,80,70,50]", cards(EvidenceKind.REASON, 4) + cards(EvidenceKind.CUT, 3))

        assertEquals(
            listOf("VERDICT", "CUTLINE", "REASON", "REASON", "REASON", "CUT", "CUT", "CONCLUSION"),
            result.cards.values.map { card -> card.kind.name },
        )
        assertEquals((0 until MAX_CARDS).toList(), result.cards.values.map(RecapCard::index))
    }

    @Test
    fun `석탄 덱은 판정, 컷 표, 깎인 말 셋, 살아남은 말 하나, 결론이다`() {
        val result = resultOf(COAL, "[40,40,40,70]", cards(EvidenceKind.CUT, 3) + cards(EvidenceKind.KEEP, 2))

        assertEquals(listOf("VERDICT", "CUTLINE", "CUT", "CUT", "CUT", "KEEP", "CONCLUSION"), result.cards.values.map { card -> card.kind.name })
    }

    @Test
    fun `근거가 없는 저활동 사용자도 항목 점수 통계 카드로 다섯 장을 채운다`() {
        val gift = resultOf(GIFT, "[90,80,70,50]", emptyList())
        val coal = resultOf(COAL, "[40,40,40,70]", emptyList())

        assertEquals(listOf("VERDICT", "CUTLINE", "REASON", "CUT", "CONCLUSION"), gift.cards.values.map { card -> card.kind.name })
        assertEquals(listOf("VERDICT", "CUTLINE", "CUT", "KEEP", "CONCLUSION"), coal.cards.values.map { card -> card.kind.name })
        val statCard = gift.cards.values.filterIsInstance<RecapEvidenceCard>().first()
        assertNull(statCard.quote)
        assertEquals("공감 점수 90점", statCard.chipText)
        assertEquals("공감 +", statCard.dimTag)
        val cut = coal.cards.values.filterIsInstance<RecapEvidenceCard>().first()
        assertTrue(cut.chipText.startsWith(cut.dimTag.orEmpty().removeSuffix(" −")))
        assertTrue(cut.dimTag.orEmpty().endsWith("−"))
    }

    @Test
    fun `회귀 - 참가자가 넷뿐인 4등은 상위 100퍼센트 대신 순위만 모든 공유 카드에 싣는다`() {
        val result = resultOf(GIFT, "[90,80,70,50]", emptyList(), snapshot = snapshot(4), participants = 4)

        assertEquals(RecapTopResult(4, null, 4, false), result.top)
        result.cards.toDeckCards(result.top, result.scoreText).convertEach { card ->
            assertEquals(4, card.rank)
            assertNull(card.topPercent)
        }
    }

    @Test
    fun `참가자가 백 명 이상이면 상위 비율을 싣는다`() {
        val result = resultOf(GIFT, "[90,80,70,50]", emptyList(), snapshot = snapshot(2), participants = 200)

        assertEquals(RecapTopResult(2, 1, 200, false), result.top)
    }

    @Test
    fun `근거 카드 영역 태그는 칩 방향이 가리키는 항목으로 단다`() {
        val thanks = RoastEvidence(EvidenceKind.CUT, null, "thanks_count", RoastChip("'고마워'", "0번"), null, "고마움실종상", "고맙다는 말이 실종됐다.",
            RoastSource.AI, ScoreDimension.RESPECTFUL_COMMUNICATION.key)
        val result = resultOf(COAL, "[40,50,60,70]", listOf(thanks) + cards(EvidenceKind.KEEP, 1))
        val evidence = result.cards.values.filterIsInstance<RecapEvidenceCard>()

        assertEquals("존중 −", evidence.first().dimTag)
    }

    @Test
    fun `회귀 - 석탄 덱은 호들갑 원탑 같은 신나는 유형 대신 같은 항목의 차분한 유형을 싣는다`() {
        assertEquals(Archetype.TREE_TOP_STAR, Archetype.CAROL_LEADER.forCoal())
        assertEquals(Archetype.GRINCH_APPRENTICE, Archetype.SANTA_CANDIDATE.forCoal())
        assertEquals(Archetype.FIREPLACE_KEEPER, Archetype.FIREPLACE_KEEPER.forCoal())
        val conclusion = resultOf(COAL, "[40,50,60,70]", emptyList()).cards.values.filterIsInstance<RecapConclusionCard>().single()

        assertEquals(Archetype.ofName(conclusion.archetypeCode)?.forCoal(), Archetype.ofName(conclusion.archetypeCode))
    }

    @Test
    fun `최신 결과가 아직 순위에 오르지 않았으면 순위 없이 대기다`() {
        val result = resultOf(GIFT, "[90,80,70,50]", emptyList(), snapshot = null, participants = 0)

        assertEquals(RecapTopResult(null, null, null, true), result.top)
    }

    @Test
    fun `근거 카드는 종류 안 순번을 갖고 작가 카드는 영역 태그가 없으며 결론은 친구 소환 고정문과 해시태그 낱말을 싣는다`() {
        val result = resultOf(COAL, "[40,50,60,70]", cards(EvidenceKind.CUT, 2) + cards(EvidenceKind.KEEP, 1))
        val evidence = result.cards.values.filterIsInstance<RecapEvidenceCard>()
        val conclusion = result.cards.values.filterIsInstance<RecapConclusionCard>().single()

        assertEquals(listOf(1, 2, 1), evidence.map(RecapEvidenceCard::ordinal))
        assertEquals(listOf(null, null, null), evidence.map(RecapEvidenceCard::dimTag))
        assertEquals("우리 방 석탄 동기 구함", conclusion.friendCall)
        assertEquals("석탄당첨", conclusion.bandTag)
        assertTrue(result.cards.values.filterIsInstance<RecapCutlineCard>().single().bands.values.single { band -> band.current }.code == "COAL")
    }

    private fun resultOf(
        hundredths: Int,
        components: String,
        evidence: List<RoastEvidence>,
        snapshot: ResultSnapshot? = snapshot(RANK),
        participants: Long = PARTICIPANTS,
    ): RecapResult {
        val assessment = Assessment("me", "me", DataSource.KAKAO, hundredths, "판정", "한마디", components, AT, 6, null, 50, 2, null, null)
        val gift = GiftVerdict(hundredths)
        val profile = ResultProfile.of(assessment, snapshot, ResultPopulation.of(Assessments(listOf(assessment))), participants)
        val content = RoastContent.fallbackOf(gift, fallback).copy(evidence = RoastEvidences(evidence))
            .forDeck(gift, assessment.dimensionScores(), fallback)
        return assertNotNull(RecapResult.of(assessment, snapshot, profile, content))
    }

    private fun cards(kind: EvidenceKind, count: Int): List<RoastEvidence> = List(count) { index ->
        RoastEvidence(kind, index + 1, "${kind.key}_$index", RoastChip("칩", "${index + 1}번"), "말풍선 $index", "산타인정상", "산타도 인정한다.", RoastSource.AI)
    }

    private fun snapshot(rank: Int): ResultSnapshot = ResultSnapshot("s", "me", "me", rank, rank, AT, AT)

    companion object {
        private const val GIFT = 7373
        private const val COAL = 4200
        private const val RANK = 3
        private const val PARTICIPANTS = 10L
        private const val MAX_CARDS = 8
        private val AT: Instant = Instant.parse("2026-11-01T00:00:00Z")
    }
}
