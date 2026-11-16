package com.santacut.presentation.analysis.response

import org.junit.jupiter.api.Test
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * 진행 상황 스트림 목록이 참가자당 하나만 두고, 다시 연결하면 이전 것을 대신하며, 끝난 스트림을 치우는지 확인한다.
 */
class AnalysisStatusStreamsTest {
    private val streams = AnalysisStatusStreams()
    private val participant = UUID.randomUUID()

    @Test
    fun `같은 참가자가 다시 열면 이전 스트림을 닫고 새 스트림 하나만 남긴다`() {
        val first = stream()
        val second = stream()

        streams.open(participant, first, event(QUEUED))
        streams.open(participant, second, event(QUEUED))

        assertEquals(1, streams.size())
        assertFalse(first.isOpen())
        assertTrue(second.isOpen())
    }

    @Test
    fun `참가자마다 스트림을 따로 세어 앱 전체 수를 알려 준다`() {
        streams.open(participant, stream(), event(QUEUED))
        streams.open(UUID.randomUUID(), stream(), event(QUEUED))

        assertEquals(2, streams.size())
        assertTrue(streams.hasStreamOf(participant))
        assertFalse(streams.hasStreamOf(UUID.randomUUID()))
    }

    @Test
    fun `끝난 단계를 보내면 스트림을 닫고 목록에서 뺀다`() {
        val stream = stream()
        streams.open(participant, stream, event(QUEUED))

        streams.publish(participant, event(SUCCEEDED, terminal = true))

        assertFalse(stream.isOpen())
        assertEquals(0, streams.size())
    }

    @Test
    fun `이미 끝난 작업에 연결하면 끝난 단계를 보내자마자 닫는다`() {
        val stream = stream()

        streams.open(participant, stream, event(FAILED, terminal = true))

        assertFalse(stream.isOpen())
        assertFalse(streams.hasStreamOf(participant))
    }

    @Test
    fun `다른 작업의 상태는 보내지 않고 스트림을 그대로 둔다`() {
        val stream = stream()
        streams.open(participant, stream, event(QUEUED))

        streams.publish(participant, event(SUCCEEDED, terminal = true, jobId = OTHER_JOB_ID))

        assertTrue(stream.isOpen())
        assertEquals(1, streams.size())
    }

    @Test
    fun `직접 닫은 스트림도 목록에서 빠진다`() {
        val stream = stream()
        streams.open(participant, stream, event(QUEUED))

        stream.close()

        assertEquals(0, streams.size())
    }

    private fun stream(): AnalysisStatusStream = AnalysisStatusStream(JOB_ID, SseEmitter(TIMEOUT_MS))

    private fun event(status: String, terminal: Boolean = false, jobId: String = JOB_ID): AnalysisStatusEvent =
        AnalysisStatusEvent(jobId, 0, terminal, AnalysisStatusResponse(jobId, status, null, null, CREATED_AT, null))

    companion object {
        private const val JOB_ID = "11111111-1111-1111-1111-111111111111"
        private const val OTHER_JOB_ID = "22222222-2222-2222-2222-222222222222"
        private const val QUEUED = "QUEUED"
        private const val SUCCEEDED = "SUCCEEDED"
        private const val FAILED = "FAILED"
        private const val CREATED_AT = "2026-11-16T00:00:00Z"
        private const val TIMEOUT_MS = 1_000L
    }
}
