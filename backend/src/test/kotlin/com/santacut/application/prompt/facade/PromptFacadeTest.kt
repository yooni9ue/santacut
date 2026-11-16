package com.santacut.application.prompt.facade

import com.santacut.ErrorCodeAssertions.assertErrorCode
import com.santacut.IntegrationTest
import com.santacut.application.prompt.command.CreatePromptCommand
import com.santacut.application.prompt.command.PromptNameCommand
import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.prompt.model.Prompt
import com.santacut.domain.prompt.model.PromptName
import com.santacut.infrastructure.prompt.repository.PromptRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ClassPathResource
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import java.util.UUID
import javax.sql.DataSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 프롬프트 판 저장, 최신 판 조회, 판 목록 조회와 앱 시작 때 넣는 채점 프롬프트 1판·2판·3판을 확인한다.
 */
@IntegrationTest
class PromptFacadeTest @Autowired constructor(
    private val promptFacade: PromptFacade,
    private val promptRepository: PromptRepository,
    private val dataSource: DataSource,
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
    fun `이름이 규칙을 벗어나면 저장과 조회 모두 INVALID_REQUEST다`() {
        assertErrorCode(ErrorCode.INVALID_REQUEST) { promptFacade.create(CreatePromptCommand("Score", "본문")) }
        assertErrorCode(ErrorCode.INVALID_REQUEST) { promptFacade.getLatest(PromptNameCommand("score_v2")) }
        assertErrorCode(ErrorCode.INVALID_REQUEST) { promptFacade.listVersions(PromptNameCommand("")) }
    }

    @Test
    fun `앱이 뜰 때 채점 프롬프트 1판을 줄바꿈까지 그대로 넣는다`() {
        val first = promptRepository.findAllByNameOrderByVersionDesc(PromptName.SCORE.value).last()

        assertEquals(Prompt.FIRST_VERSION, first.version)
        assertTrue(first.content.startsWith("너는 재미 목적으로만 점수를 매기는 SantaCut Scorer다."))
        assertTrue(first.content.contains("\n\n입력 이벤트에 나타난"))
        assertTrue(first.content.endsWith("서버가 계산한다."))
    }

    @Test
    fun `앱이 뜰 때 신호와 표본 형식을 설명하는 채점 프롬프트 2판도 넣는다`() {
        val second = promptRepository.findAllByNameOrderByVersionDesc(PromptName.SCORE.value).first { it.version == SEED_SECOND_VERSION }

        assertTrue(second.content.startsWith("너는 재미용 산타 점수를 매기는 SantaCut Scorer다."))
        assertTrue(second.content.contains("[signals]: 항목마다 한 줄이다."))
        assertTrue(second.content.contains("`번호|본문` 한 줄이 메시지 하나"))
        assertTrue(second.content.contains("evidence는 항목마다 근거 표본 번호 1~3개"))
        assertTrue(second.content.endsWith("다른 필드는 넣지 않는다."))
    }

    @Test
    fun `앱이 뜰 때 부정 낱말 수와 날짜 간격 줄을 설명하는 채점 프롬프트 3판도 넣는다`() {
        val third = promptRepository.findAllByNameOrderByVersionDesc(PromptName.SCORE.value).first { it.version == SEED_THIRD_VERSION }

        assertTrue(third.content.contains("neg는 낱말 앞뒤에 부정이나 반어"))
        assertTrue(third.content.contains("`(3일 뒤)` 같은 줄은 앞 표본 줄과 그만큼 날짜가 벌어졌다는 표시"))
        assertTrue(third.content.endsWith("다른 필드는 넣지 않는다."))
    }

    @Test
    fun `프롬프트 시드를 다시 실행해도 판이 늘거나 본문이 바뀌지 않는다`() {
        val before = promptRepository.findAllByNameOrderByVersionDesc(PromptName.SCORE.value).map { it.version to it.content }

        ResourceDatabasePopulator(ClassPathResource(PROMPT_SEED)).execute(dataSource)

        val after = promptRepository.findAllByNameOrderByVersionDesc(PromptName.SCORE.value).map { it.version to it.content }
        assertEquals(before, after)
    }

    private fun uniqueName(): String = "test-" + UUID.randomUUID().toString().take(NAME_SUFFIX_LENGTH)

    companion object {
        private const val NAME_SUFFIX_LENGTH = 8
        private const val SEED_SECOND_VERSION = 2
        private const val SEED_THIRD_VERSION = 3
        private const val PROMPT_SEED = "prompts.sql"
    }
}
