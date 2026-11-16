package com.santacut.presentation.analysis.response

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

    fun hasStreamOf(participantId: UUID): Boolean = values.containsKey(participantId)

    /** [stream]을 이 참가자의 스트림으로 등록하고 [initial] 상태를 바로 보낸다. 이전 스트림은 닫는다. */
    fun open(participantId: UUID, stream: AnalysisStatusStream, initial: AnalysisStatusEvent) {
        stream.onFinish { values.remove(participantId, stream) }
        values.put(participantId, stream)?.close()
        stream.send(initial)
    }

    /** 이 참가자가 연 스트림이 이 작업을 보고 있으면 새 상태를 보낸다. */
    fun publish(participantId: UUID, event: AnalysisStatusEvent) {
        values[participantId]?.send(event)
    }

    fun heartbeat() {
        values.values.forEach { it.heartbeat() }
    }
}
