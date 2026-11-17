package com.santacut.domain.analysis.model

/**
 * 비용 확인을 통과했지만 아직 끝나지 않은 작업들의 어림 사용량. 여러 작업자 스레드가 함께 쓰므로 모든 연산을 잠근다.
 * 실제 사용량이 기록된 뒤에도 작업이 끝날 때까지 남겨 두므로, 그동안은 비용을 두 번 세어 한도를 보수적으로 본다.
 */
class ReservedUsages(private val values: MutableList<ModelUsage> = mutableListOf()) {
    @Synchronized
    fun total(): ModelUsage = values.fold(ModelUsage.NONE) { sum, usage -> sum + usage }

    @Synchronized
    fun add(usage: ModelUsage) {
        values.add(usage)
    }

    @Synchronized
    fun remove(usage: ModelUsage) {
        values.remove(usage)
    }
}
