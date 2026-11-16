package com.santacut.presentation.analysis.stream

import com.santacut.presentation.analysis.response.AnalysisStatusResponse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
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

        open(participant, first, event(QUEUED))
        open(participant, second, event(QUEUED))

        assertEquals(1, streams.size())
        assertFalse(first.isOpen())
        assertTrue(second.isOpen())
    }

    @Test
    fun `참가자마다 스트림을 따로 세어 앱 전체 수를 알려 준다`() {
        open(participant, stream(), event(QUEUED))
        open(UUID.randomUUID(), stream(), event(QUEUED))

        assertEquals(2, streams.size())
        assertTrue(streams.hasStreamOf(participant))
        assertFalse(streams.hasStreamOf(UUID.randomUUID()))
    }

    @Test
    fun `상한 확인에는 지금 열린 수와 이 참가자 스트림을 대신하는지를 넘긴다`() {
        open(participant, stream(), event(QUEUED))
        val checked = mutableListOf<Pair<Int, Boolean>>()

        streams.open(participant, stream(), { openStreams, replacing -> checked.add(openStreams to replacing) }) { event(QUEUED) }
        streams.open(UUID.randomUUID(), stream(), { openStreams, replacing -> checked.add(openStreams to replacing) }) { event(QUEUED) }

        assertEquals(listOf(1 to true, 1 to false), checked)
    }

    @Test
    fun `상한에 걸리면 등록하지 않고 현재 상태도 읽지 않는다`() {
        var loaded = false

        assertThrows<IllegalStateException> {
            streams.open(participant, stream(), { _, _ -> throw IllegalStateException("상한") }) {
                loaded = true
                event(QUEUED)
            }
        }

        assertFalse(loaded)
        assertEquals(0, streams.size())
    }

    @Test
    fun `현재 상태를 읽는 사이 작업이 끝나도 끝난 단계를 받고 닫힌다`() {
        val emitter = RecordingSseEmitter()
        val stream = AnalysisStatusStream(JOB_ID, emitter)

        open(participant, stream) {
            streams.publish(participant, event(SUCCEEDED, terminal = true))
            event(ANALYZING)
        }

        assertEquals(1, emitter.sent.size)
        assertTrue(emitter.sent.single().contains(SUCCEEDED))
        assertFalse(stream.isOpen())
        assertEquals(0, streams.size())
    }

    @Test
    fun `현재 상태를 읽지 못하면 스트림을 닫고 목록에서 뺀다`() {
        val stream = stream()

        assertThrows<IllegalArgumentException> {
            open(participant, stream) { throw IllegalArgumentException("없는 작업") }
        }

        assertFalse(stream.isOpen())
        assertEquals(0, streams.size())
    }

    @Test
    fun `끝난 단계를 보내면 스트림을 닫고 목록에서 뺀다`() {
        val stream = stream()
        open(participant, stream, event(QUEUED))

        streams.publish(participant, event(SUCCEEDED, terminal = true))

        assertFalse(stream.isOpen())
        assertEquals(0, streams.size())
    }

    @Test
    fun `이미 끝난 작업에 연결하면 끝난 단계를 보내자마자 닫는다`() {
        val stream = stream()

        open(participant, stream, event(FAILED, terminal = true))

        assertFalse(stream.isOpen())
        assertFalse(streams.hasStreamOf(participant))
    }

    @Test
    fun `다른 작업의 상태는 보내지 않고 스트림을 그대로 둔다`() {
        val stream = stream()
        open(participant, stream, event(QUEUED))

        streams.publish(participant, event(SUCCEEDED, terminal = true, jobId = OTHER_JOB_ID))

        assertTrue(stream.isOpen())
        assertEquals(1, streams.size())
    }

    @Test
    fun `직전에 보낸 단계와 같은 상태는 다시 보내지 않는다`() {
        val emitter = RecordingSseEmitter()
        open(participant, AnalysisStatusStream(JOB_ID, emitter), event(QUEUED))

        streams.publish(participant, event(QUEUED))
        streams.publish(participant, event(ANALYZING))

        assertEquals(2, emitter.sent.size)
    }

    @Test
    fun `열린 스트림마다 연결을 살려 두는 주석을 보낸다`() {
        val emitter = RecordingSseEmitter()
        open(participant, AnalysisStatusStream(JOB_ID, emitter), event(QUEUED))

        streams.heartbeat()

        assertTrue(emitter.sent.last().startsWith(":heartbeat"))
    }

    @Test
    fun `보내다 연결이 끊긴 스트림은 목록에서 빠진다`() {
        val stream = AnalysisStatusStream(JOB_ID, RecordingSseEmitter(failing = true))

        open(participant, stream, event(QUEUED))

        assertFalse(stream.isOpen())
        assertEquals(0, streams.size())
    }

    @Test
    fun `직접 닫은 스트림도 목록에서 빠진다`() {
        val stream = stream()
        open(participant, stream, event(QUEUED))

        stream.close()

        assertEquals(0, streams.size())
    }

    private fun open(participantId: UUID, stream: AnalysisStatusStream, initial: AnalysisStatusEvent) =
        open(participantId, stream) { initial }

    private fun open(participantId: UUID, stream: AnalysisStatusStream, loadInitial: () -> AnalysisStatusEvent) =
        streams.open(participantId, stream, { _, _ -> }, loadInitial)

    private fun stream(): AnalysisStatusStream = AnalysisStatusStream(JOB_ID, RecordingSseEmitter())

    private fun event(status: String, terminal: Boolean = false, jobId: String = JOB_ID): AnalysisStatusEvent =
        AnalysisStatusEvent(jobId, 0, terminal, AnalysisStatusResponse(jobId, status, null, null, CREATED_AT, null))

    companion object {
        private const val JOB_ID = "11111111-1111-1111-1111-111111111111"
        private const val OTHER_JOB_ID = "22222222-2222-2222-2222-222222222222"
        private const val QUEUED = "QUEUED"
        private const val ANALYZING = "ANALYZING"
        private const val SUCCEEDED = "SUCCEEDED"
        private const val FAILED = "FAILED"
        private const val CREATED_AT = "2026-11-16T00:00:00Z"
    }
}
