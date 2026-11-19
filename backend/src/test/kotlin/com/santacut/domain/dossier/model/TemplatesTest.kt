package com.santacut.domain.dossier.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TemplatesTest {
    private val registered = setOf("phrase", "count")

    @Test
    fun `등록한 슬롯만 쓴 문구는 위반이 없다`() {
        assertEquals(emptyList(), Templates(listOf("'{phrase}'{phrase:을} {count}")).violations("p", registered))
    }

    @Test
    fun `아라비아 숫자, 금지 슬롯, 미등록 슬롯, 틀린 슬롯 문법, 빈 후보는 위반이다`() {
        val violations = listOf("숫자 3번", "{name} 등장", "{topic} 등장", "{phrase:에} 등장", "닫는 괄호 }")
            .map { text -> Templates(listOf(text)).violations("p", registered) }

        assertTrue(violations.all { found -> found.size == 1 }, violations.toString())
        assertEquals(1, Templates(emptyList()).violations("p", registered).size)
    }

    @Test
    fun `같은 시드는 늘 같은 문구를 고르고 채울 수 없는 문구는 건너뛴다`() {
        val templates = Templates(listOf("가 {phrase}", "나 {topic}", "다 {phrase}"))
        val seed = TemplateSeed("결과").of("훅")
        val slots = Slots.of("phrase" to "진짜")

        val picked = templates.pick(seed, slots)

        assertEquals(picked, templates.pick(TemplateSeed("결과").of("훅"), slots))
        assertTrue(picked == "가 진짜" || picked == "다 진짜")
        assertNull(Templates(listOf("{topic}")).pick(seed, slots))
    }

    @Test
    fun `여러 줄을 고르면 서로 다른 문구를 한도까지 고른다`() {
        val lines = Templates(listOf("가", "나", "다")).pickLines(TemplateSeed("결과"), Slots.EMPTY, 2, true)

        assertEquals(2, lines.map(LineDraft::text).distinct().size)
    }
}
