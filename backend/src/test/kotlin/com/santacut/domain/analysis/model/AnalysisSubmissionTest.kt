package com.santacut.domain.analysis.model

import com.santacut.TestDossiers
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** 작업 페이로드의 참여자 이름. 비었으면 JSON에 쓰지 않아 요청 해시가 그대로이고, 예전 페이로드는 빈 목록으로 읽는다. */
class AnalysisSubmissionTest {
    private val mapper = TestDossiers.mapper

    @Test
    fun `참여자 이름이 없으면 JSON에 쓰지 않아 이벤트 제출의 페이로드가 예전과 같다`() {
        val json = mapper.writeValueAsString(AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())))

        assertFalse(json.contains("speakerNames"))
    }

    @Test
    fun `참여자 이름 필드가 없는 예전 페이로드는 빈 목록으로 읽고 담은 이름은 다시 읽힌다`() {
        val old = mapper.readValue("""{"source":"KAKAO","events":[]}""", AnalysisSubmission::class.java)
        val named = AnalysisSubmission(DataSource.KAKAO, AnalysisEvents(emptyList())).withSpeakerNames(listOf("평친성필"))

        assertTrue(old.speakerNames.isEmpty())
        assertEquals(listOf("평친성필"), mapper.readValue(mapper.writeValueAsString(named), AnalysisSubmission::class.java).speakerNames)
    }
}
