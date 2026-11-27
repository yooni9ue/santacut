package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import com.santacut.domain.analysis.model.Archetype
import com.santacut.domain.analysis.model.GiftGrade
import org.junit.jupiter.api.Test
import tools.jackson.databind.JsonNode
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 서버가 고정한 화면 문구 전부에 영포티 금지 목록(roast/slang.json의 banned·banned_patterns, 명세 5절 검증 규칙 8: 엘프, 공방, 추동가, ~의 달인 등)을 돌린다.
 * 작가 문장만 검사하던 금지 목록이 유형 이름('일 벌이는 공방 엘프')처럼 서버 상수에서 새지 않게 CI에서 막는다.
 * 대상은 유형 이름, 구간 이름·도장·컷 표 문구·공유 문구, 대체 문장(roast/fallback.json)과 펀치 틀(roast/punch.json)의 모든 글이다.
 */
class ServerCopyBlacklistTest {
    private val slang = TestRoasts.catalog.rules.slang
    private val blacklist = Regex((slang.banned.map(Regex::escape) + slang.bannedPatterns).joinToString("|") { pattern -> "(?:$pattern)" })

    @Test
    fun `금지 목록은 명세 5절 영포티 말투를 모두 품는다`() {
        listOf("엘프", "공방", "추동가", "약속의 달인", "답장하는 편", "독촉하는 자", "MZ", "갓생러", "킹받", "오히려 좋아").forEach { sample ->
            assertTrue(blacklist.containsMatchIn(sample), sample)
        }
    }

    @Test
    fun `유형 이름과 구간 문구에 금지 말투가 없다`() {
        val texts = Archetype.entries.map(Archetype::koreanName) +
            GiftGrade.entries.flatMap { grade -> listOf(grade.label, grade.stamp, grade.detail, grade.shareText, grade.temperature) }

        assertEquals(emptyList(), texts.filter(blacklist::containsMatchIn))
    }

    @Test
    fun `대체 문장과 펀치 틀의 모든 글에 금지 말투가 없다`() {
        val texts = listOf("fallback.json", "punch.json").flatMap { name -> textsOf(TestRoasts.mapper.readTree(Files.readString(DIRECTORY.resolve(name)))) }

        assertTrue(texts.size > MIN_TEXTS, texts.size.toString())
        assertEquals(emptyList(), texts.filter(blacklist::containsMatchIn))
    }

    /** JSON 트리의 모든 문자열 값. 설명 문장(notes)은 금지 말을 예로 들 수 있어 뺀다. */
    private fun textsOf(node: JsonNode): List<String> = when {
        node.isString -> listOf(node.asString())
        node.isArray -> node.values().flatMap(::textsOf)
        node.isObject -> node.properties().filterNot { (name, _) -> name == NOTES }.flatMap { (_, child) -> textsOf(child) }
        else -> emptyList()
    }

    companion object {
        private val DIRECTORY: Path = Path.of("src/main/resources/roast")
        private const val NOTES = "notes"
        private const val MIN_TEXTS = 100
    }
}
