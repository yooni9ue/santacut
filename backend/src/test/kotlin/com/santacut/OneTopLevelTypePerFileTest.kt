package com.santacut

import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readLines
import kotlin.test.assertTrue

/**
 * 컨벤션 9장: `src/main`과 `src/test`의 모든 Kotlin 파일은 최상위 타입을 하나만 가지고,
 * 파일 이름이 그 타입 이름과 같아야 한다.
 */
class OneTopLevelTypePerFileTest {
    @Test
    fun `모든 Kotlin 파일은 최상위 타입 하나만 선언하고 파일 이름이 타입 이름과 같다`() {
        val kotlinFiles = Files.walk(SOURCE_ROOT).use { paths -> paths.filter { it.extension == KOTLIN }.toList() }
        val violations = kotlinFiles.mapNotNull(::violationOf)
        assertTrue(kotlinFiles.isNotEmpty(), "검사할 Kotlin 파일을 찾지 못했습니다.")
        assertTrue(violations.isEmpty(), violations.joinToString(separator = "\n"))
    }

    private fun violationOf(file: Path): String? {
        val typeNames = file.readLines().mapNotNull { TOP_LEVEL_TYPE.find(it)?.groupValues?.get(1) }
        return when {
            typeNames.size > 1 -> "$file: 최상위 타입이 ${typeNames.size}개입니다 $typeNames"
            typeNames.size == 1 && typeNames.single() != file.nameWithoutExtension ->
                "$file: 파일 이름이 타입 이름(${typeNames.single()})과 다릅니다"
            else -> null
        }
    }

    companion object {
        private val SOURCE_ROOT: Path = Path.of("src")
        private const val KOTLIN = "kt"

        /** 들여쓰기 없이 시작하는 클래스, 인터페이스, object, 별칭 선언. 줄 앞 애노테이션과 변경자를 허용한다. */
        private val TOP_LEVEL_TYPE = Regex(
            "^(?:@[\\w.]+(?:\\(.*\\))?\\s+)*" +
                "(?:(?:public|internal|private|protected|sealed|abstract|open|final|data|enum|annotation|value|inline|fun)\\s+)*" +
                "(?:class|interface|object|typealias)\\s+(\\w+)",
        )
    }
}
