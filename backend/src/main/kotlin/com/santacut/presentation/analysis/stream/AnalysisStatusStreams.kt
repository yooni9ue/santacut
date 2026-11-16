package com.santacut.presentation.analysis.stream

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 이 앱 인스턴스에 열려 있는 진행 상황 스트림. 참가자 한 명당 하나이며, 새로 열면 이전 것을 닫고 대신한다.
 * 서버가 한 대라 메모리에만 둔다. 끝난 스트림은 스스로 목록에서 빠진다.
 */
class AnalysisStatusStreams(
    private val values: MutableMap<UUID, AnalysisStatusStream> = ConcurrentHashMap(),
) {
    fun size(): Int = values.size

    /**
     * [stream]을 이 참가자의 스트림으로 먼저 등록한 뒤 [loadInitial]로 현재 상태를 읽어 보낸다.
     * 읽기 전에 등록하므로, 읽는 사이 작업이 끝나도 그 이벤트를 놓치지 않는다.
     * 이전 스트림은 읽기에 성공한 뒤에야 닫는다. 읽기에 실패하면(없는 작업, 남의 작업) 새 스트림만 닫고 이전 스트림을 되돌린다.
     *
     * @param verifyOpenable 지금 열린 스트림 수와 이 참가자 스트림을 대신하는지를 받아 상한을 확인한다
     */
    fun open(
        participantId: UUID,
        stream: AnalysisStatusStream,
        verifyOpenable: (openStreams: Int, replacing: Boolean) -> Unit,
        loadInitial: () -> AnalysisStatusEvent,
    ) {
        val previous = register(participantId, stream, verifyOpenable)
        val initial = try {
            loadInitial()
        } catch (exception: RuntimeException) {
            restore(participantId, stream, previous)
            throw exception
        }
        previous?.close()
        stream.sendInitial(initial)
    }

    /** 이 참가자가 연 스트림이 이 작업을 보고 있으면 새 상태를 보낸다. */
    fun publish(participantId: UUID, event: AnalysisStatusEvent) {
        values[participantId]?.send(event)
    }

    fun heartbeat() {
        values.values.forEach { stream -> stream.heartbeat() }
    }

    /** 상한 확인과 등록을 한 잠금 안에서 해, 동시에 들어온 요청이 함께 상한을 넘지 못하게 한다. 대신한 이전 스트림을 돌려준다. */
    @Synchronized
    private fun register(
        participantId: UUID,
        stream: AnalysisStatusStream,
        verifyOpenable: (openStreams: Int, replacing: Boolean) -> Unit,
    ): AnalysisStatusStream? {
        verifyOpenable(values.size, values.containsKey(participantId))
        stream.onFinish { values.remove(participantId, stream) }
        return values.put(participantId, stream)
    }

    /** 열지 못한 [stream]을 닫고, 이전 스트림이 아직 열려 있으면 그 자리에 되돌린다. */
    @Synchronized
    private fun restore(participantId: UUID, stream: AnalysisStatusStream, previous: AnalysisStatusStream?) {
        if (previous != null && previous.isOpen()) values.replace(participantId, stream, previous)
        stream.close()
    }
}
