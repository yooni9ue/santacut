package com.santacut.application.prompt.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.application.prompt.command.CreatePromptCommand
import com.santacut.application.prompt.command.PromptNameCommand
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.prompt.model.Prompt
import com.santacut.infrastructure.prompt.repository.PromptRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 프롬프트 판 저장, 최신 판 조회, 판 목록 조회와 앱 시작 때 넣는 채점 프롬프트 1판을 확인한다.
 */
@IntegrationTest
class PromptFacadeTest @Autowired constructor(
    private val promptFacade: PromptFacade,
    private val promptRepository: PromptRepository,
) {
    @Test
    fun `처음 쓰는 이름은 1판이고 다음 저장마다 판 번호가 1씩 는다`() {
        val name = uniqueName()

        assertEquals(1, promptFacade.create(CreatePromptCommand(name, "1판")).version)
        assertEquals(2, promptFacade.create(CreatePromptCommand(name, "2판")).version)
        assertEquals(3, promptFacade.create(CreatePromptCommand(name, "3판")).version)
    }

    @Test
    fun `최신 판 조회는 가장 높은 판을 본문과 함께 돌려준다`() {
        val name = uniqueName()
        promptFacade.create(CreatePromptCommand(name, "1판"))
        promptFacade.create(CreatePromptCommand(name, "2판"))

        val latest = promptFacade.getLatest(PromptNameCommand(name))

        assertEquals(name, latest.name)
        assertEquals(2, latest.version)
        assertEquals("2판", latest.content)
    }

    @Test
    fun `판 목록은 높은 판부터 나온다`() {
        val name = uniqueName()
        promptFacade.create(CreatePromptCommand(name, "1판"))
        promptFacade.create(CreatePromptCommand(name, "2판"))

        val versions = promptFacade.listVersions(PromptNameCommand(name))

        assertEquals(listOf(2, 1), versions.map { it.version })
    }

    @Test
    fun `없는 이름은 최신 판과 판 목록 모두 PROMPT_NOT_FOUND다`() {
        val name = uniqueName()

        assertErrorCode(ErrorCode.PROMPT_NOT_FOUND) { promptFacade.getLatest(PromptNameCommand(name)) }
        assertErrorCode(ErrorCode.PROMPT_NOT_FOUND) { promptFacade.listVersions(PromptNameCommand(name)) }
    }

    @Test
    fun `앱이 뜰 때 채점 프롬프트 1판을 줄바꿈까지 그대로 넣는다`() {
        val first = promptRepository.findAllByNameOrderByVersionDesc(Prompt.SCORE).last()

        assertEquals(Prompt.FIRST_VERSION, first.version)
        assertTrue(first.content.startsWith("너는 재미 목적으로만 점수를 매기는 SantaCut Scorer다."))
        assertTrue(first.content.contains("\n\n입력 이벤트에 나타난"))
        assertTrue(first.content.endsWith("서버가 계산한다."))
    }

    private fun uniqueName(): String = "test-" + UUID.randomUUID().toString().take(NAME_SUFFIX_LENGTH)

    companion object {
        private const val NAME_SUFFIX_LENGTH = 8
    }
}
