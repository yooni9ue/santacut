package com.santacut.presentation.request

import com.santacut.domain.model.DataSource
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SubmitAnalysisRequestTest {
    @Test
    fun `요청이 받는 출처 값은 도메인 출처 열거형 상수와 같다`() {
        val requestSources = setOf(SubmitAnalysisRequest.KAKAO_SOURCE, SubmitAnalysisRequest.INSTAGRAM_SOURCE)
        assertEquals(DataSource.entries.map { it.name }.toSet(), requestSources)
    }

    @Test
    fun `출처 검증 정규식은 정해진 출처만 통째로 받는다`() {
        val pattern = Regex(SubmitAnalysisRequest.SOURCE_PATTERN)
        DataSource.entries.forEach { assertTrue(pattern.matches(it.name)) }
        assertFalse(pattern.matches("kakao"))
        assertFalse(pattern.matches("KAKAOINSTAGRAM"))
    }
}
