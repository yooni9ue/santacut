package com.santacut.domain.roast.model

import com.santacut.TestDossiers
import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.ScoreDimension
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 통계 카드 대체 문구. 라벨과 한 줄이 칩마다·근거 종류마다 따로 있어 그 칩을 말한다. */
class RoastFallbackTest {
    private val catalog = TestRoasts.catalog
    private val fallback = catalog.fallback
    private val gift = TestRoasts.brief(TestDossiers.selves(List(MESSAGES) { "다음 주말에 같이 놀러 가자" }))
    private val coal = TestRoasts.brief(TestDossiers.selves(List(MESSAGES) { "다음 주말에 같이 놀러 가자" }), TestRoasts.COAL_SCORE)

    @Test
    fun `모든 칩 후보와 항목 점수 칩, 입버릇 칩에 칩별 문구가 있다`() {
        assertEquals(emptyList(), catalog.violations())
        assertEquals(emptyList(), fallback.chips.missing(chipFacts().map(RoastChipFact::key)))
    }

    @Test
    fun `칩별 대체 라벨과 한 줄은 모든 순번에서 검증 규칙을 지키고 자기 칩과 낱말을 나눈다`() {
        chipFacts().forEach { fact ->
            EvidenceKind.entries.forEach { kind ->
                listOf(false, true).forEach { mild ->
                    (0 until INDICES).forEach { index -> assertCardPasses(fallback.evidenceOf(kind, index, fact, mild), fact, kind) }
                }
            }
        }
    }

    @Test
    fun `입버릇 틀은 칩이 될 수 있는 사전의 모든 입버릇 표기로 채워도 검증 규칙을 지키고 자기 칩과 낱말을 나눈다`() {
        val chipPhrases = TestRoasts.dossier.phrases.toList().filter { entry -> catalog.rules.isQuotable(entry.key, entry.display) }
        assertTrue(chipPhrases.size > MIN_CHIP_PHRASES, chipPhrases.size.toString())
        chipPhrases.forEach { entry ->
            val fact = phraseFact(entry.key, entry.display)
            EvidenceKind.entries.forEach { kind ->
                (0 until INDICES).forEach { index -> assertCardPasses(fallback.evidenceOf(kind, index, fact), fact, kind) }
            }
        }
    }

    @Test
    fun `항목 점수 칩은 컷 이상일 때 쓰는 순한 깎인 말 문구를 따로 지닌다`() {
        ScoreDimension.entries.forEach { dimension ->
            val fact = RoastChipFact(RoastEvidences.DIMENSION_KEY_PREFIX + dimension.key, RoastChip(dimension.label + " 점수", "85점"))
            val harsh = fallback.evidenceOf(EvidenceKind.CUT, 0, fact)
            val mild = fallback.evidenceOf(EvidenceKind.CUT, 0, fact, mild = true)
            assertFalse(harsh.label == mild.label || harsh.line == mild.line, dimension.name)
        }
    }

    private fun assertCardPasses(card: RoastEvidence, fact: RoastChipFact, kind: EvidenceKind) {
        val brief = if (kind == EvidenceKind.KEEP) coal else gift
        val where = "${fact.key} ${kind.key} ${card.label} / ${card.line}"
        assertEquals(emptyList(), problemsOf(card.label, RoastTextKind.LABEL, brief), where)
        assertEquals(emptyList(), problemsOf(card.line, RoastTextKind.EVIDENCE_LINE, brief), where)
        assertEquals(emptyList(), TextProblems.grounding(card.line, null, listOf(fact.chip.label)).toList(), where)
        assertEquals(0, catalog.rules.slangCountIn(card.label + card.line), where)
    }

    @Test
    fun `회귀 - 짧은 말풍선 칩과 고마워 칩은 일반론 대신 그 칩을 말한다`() {
        val short = fallback.evidenceOf(EvidenceKind.REASON, 0, RoastChipFact("short_ratio", RoastChip("짧은 말풍선", "4%")))
        val thanks = fallback.evidenceOf(EvidenceKind.CUT, 0, RoastChipFact(ThanksFactSpec.KEY, RoastChip("'고마워'", "0번")))

        assertFalse(short.line == "선물 썰매에 힘을 보탰다.")
        assertTrue(short.line.contains("짧"))
        assertEquals("감사품절상", thanks.label)
        assertFalse(thanks.line == "산타 수첩에 따로 적혔다.")
        assertTrue(thanks.line.contains("고맙"))
    }

    @Test
    fun `칩마다 상 이름 라벨이 다르다`() {
        assertEquals("연타폭주상", labelOf(EvidenceKind.CUT, "burst_avg", "연달아 보내기"))
        assertEquals("야간출몰상", labelOf(EvidenceKind.CUT, "night_ratio", "새벽 출몰률"))
        assertEquals("물음표살포상", labelOf(EvidenceKind.CUT, "question_ratio", "물음표 살포율"))
        assertEquals("주말잠수상", labelOf(EvidenceKind.CUT, "weekend_ratio", "주말 출몰률"))
    }

    @Test
    fun `입버릇 칩 라벨은 따옴표 없는 행동 이름 상이고 한 줄은 따옴표로 든 입버릇을 쓴다`() {
        val sugo = fallback.evidenceOf(EvidenceKind.REASON, 0, phraseFact("sugo", "수고"))
        val go = fallback.evidenceOf(EvidenceKind.REASON, 0, phraseFact("go_q", "ㄱ?"))
        val long = fallback.evidenceOf(EvidenceKind.REASON, 0, phraseFact("maldo_an_dwae", "말도 안 돼"))

        assertEquals("입버릇장인상", sugo.label)
        assertTrue(sugo.line.contains("'수고'"))
        assertEquals("입버릇장인상", go.label)
        assertTrue(go.line.contains("'ㄱ?'"))
        assertEquals("입버릇장인상", long.label)
        assertEquals("말버릇명수상", fallback.evidenceOf(EvidenceKind.REASON, 1, phraseFact("sugo", "수고")).label)
        assertTrue(long.line.contains("'말도 안 돼'"))
    }

    @Test
    fun `문구가 없는 칩(옛 결과의 칩 등)은 근거 종류별 기본 문구를 쓴다`() {
        val card = fallback.evidenceOf(EvidenceKind.CUT, 0, RoastChipFact("retired_metric", RoastChip("옛 지표", "1번")))

        assertEquals(fallback.evidence.getValue(EvidenceKind.CUT.key).labelAt(0, null), card.label)
    }

    private fun problemsOf(text: String, kind: RoastTextKind, brief: RoastBrief): List<String> =
        TextProblems.of(text, kind, brief, catalog.rules).toList().filterNot { reason -> reason.startsWith(OWN_PHRASE_QUOTE) }

    private fun labelOf(kind: EvidenceKind, key: String, label: String): String =
        fallback.evidenceOf(kind, 0, RoastChipFact(key, RoastChip(label, "1"))).label

    private fun phraseFact(key: String, display: String) =
        RoastChipFact(PhraseFactSpec.KEY_PREFIX + key, RoastChip(catalog.phrase.chipLabel.replace(PhraseFactSpec.QUOTE, display), "51번"))

    /** 칩 후보가 될 수 있는 모든 칩. 지표 칩, 고마워 칩, 입버릇 칩 둘, 항목 점수 칩이다. */
    private fun chipFacts(): List<RoastChipFact> =
        catalog.facts.toList().map { spec -> RoastChipFact(spec.key, RoastChip(spec.chipLabel, "1")) } +
            RoastChipFact(ThanksFactSpec.KEY, RoastChip(catalog.thanks.chipLabel, "0번")) +
            listOf(phraseFact("sugo", "수고"), phraseFact("go_q", "ㄱ?")) +
            ScoreDimension.entries.map { dimension ->
                RoastChipFact(RoastEvidences.DIMENSION_KEY_PREFIX + dimension.key, RoastChip(dimension.label + " 점수", "90점"))
            }

    companion object {
        private const val MESSAGES = 60

        /** 문구를 돌려 쓰는 순번. 후보 목록 길이(최대 둘)보다 넉넉히 본다. */
        private const val INDICES = 4

        /** 입버릇 칩은 작가 인용 후보가 될 수 있는 입버릇(비속어·사람 부르는 말·긴 말 제외)으로만 만든다. 사전 대부분이 여기에 든다. */
        private const val MIN_CHIP_PHRASES = 100

        /** 입버릇 틀은 그 칩의 입버릇만 따옴표로 든다. 서버가 센 표기라 작가 인용 후보 목록 검사는 보지 않는다. */
        private const val OWN_PHRASE_QUOTE = "목록 밖 인용"
    }
}
