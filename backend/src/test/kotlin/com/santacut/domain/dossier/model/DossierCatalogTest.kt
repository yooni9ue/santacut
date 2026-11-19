package com.santacut.domain.dossier.model

import com.santacut.TestDossiers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** 앱이 뜰 때 조서 리소스를 검사해, 위반이 있으면 띄우지 않는지 확인한다. */
class DossierCatalogTest {
    @Test
    fun `픽스처와 배포할 실제 리소스는 위반 없이 읽힌다`() {
        assertEquals(emptyList(), TestDossiers.fixture.violations())
        assertDoesNotThrow { TestDossiers.readReal() }
    }

    @Test
    fun `템플릿에 아라비아 숫자를 쓰면 띄우지 않는다`() {
        assertRejected("copy.json", "티저 {message_count}", "티저 3 {message_count}", "아라비아 숫자")
    }

    @Test
    fun `copy json slots에 없는 슬롯이나 금지 슬롯을 쓰면 띄우지 않는다`() {
        assertRejected("copy.json", "조서 {case_no}", "조서 {secret_no}", "미등록 슬롯")
        assertRejected("copy.json", "조서 {case_no}", "조서 {name}", "금지 슬롯")
    }

    @Test
    fun `극값 문구가 없는 지표가 있으면 띄우지 않는다`() {
        val extra = "{\"key\":\"emoji_density\",\"label\":\"이모지\",\"unit\":\"per100\",\"p10\":1,\"p50\":3,\"p90\":20,\"min_messages\":30,\"mockable\":true},"
        assertRejected("metrics.json", "\"metrics\":[", "\"metrics\":[$extra", "emoji_density")
    }

    @Test
    fun `엔진이 모르는 지표나 순서가 틀린 기준 분포는 띄우지 않는다`() {
        assertRejected("metrics.json", "\"key\":\"avg_length\"", "\"key\":\"mystery\"", "엔진이 계산하지 않는")
        assertRejected("metrics.json", "\"p10\":5,\"p50\":12", "\"p10\":50,\"p50\":12", "p10 < p50 < p90")
    }

    @Test
    fun `따뜻한 말의 채점 항목이 틀리거나 빠지면 띄우지 않는다`() {
        assertRejected("kind_words.json", "\"cooperation_sharing\"", "\"love\"", "dimension")
    }

    @Test
    fun `입버릇 key 형식이 틀리거나 겹치면 띄우지 않는다`() {
        assertRejected("phrases.json", "\"key\":\"heol\"", "\"key\":\"Heol!\"", "key는")
        assertRejected("phrases.json", "\"key\":\"heol\"", "\"key\":\"okay\"", "중복")
    }

    @Test
    fun `리소스 판이 1이 아니면 띄우지 않는다`() {
        assertRejected("bleep.json", "\"version\":1", "\"version\":2", "version")
    }

    /** [file]의 [target]을 [replacement]로 바꿔 읽으면 [expected]가 든 오류로 실패해야 한다. */
    private fun assertRejected(file: String, target: String, replacement: String, expected: String) {
        val exception = assertFailsWith<IllegalStateException> {
            TestDossiers.readFixture { name ->
                val text = TestDossiers.fixtureText(name)
                if (name == file) text.replaceFirst(target, replacement).also { changed -> check(changed != text) } else text
            }
        }
        assertContains(exception.message.orEmpty(), expected)
    }
}
