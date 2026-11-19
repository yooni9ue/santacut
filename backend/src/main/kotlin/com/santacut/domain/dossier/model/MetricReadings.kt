package com.santacut.domain.dossier.model

/**
 * 한 사람에게서 읽을 수 있는 극값 후보 지표 목록. metrics.json 순서다.
 */
class MetricReadings(private val values: List<MetricReading>) {
    /**
     * 백분위가 50에서 가장 먼 순으로 [limit]개를 고른다. 같은 계열은 하나만, 거리가 같으면 metrics.json에서 앞선 지표다.
     */
    fun extremes(limit: Int): MetricReadings = MetricReadings(
        values.sortedByDescending(MetricReading::distance).distinctBy(MetricReading::family).take(limit),
    )

    /** [index]번째(0부터) 지표. 없으면 null이다. */
    fun at(index: Int): MetricReading? = values.getOrNull(index)

    fun find(key: String): MetricReading? = values.find { reading -> reading.key == key }

    /** 놀림 문구를 쓸 수 있는 지표의 방향 key(높으면 key, 낮으면 key_low). 죄목 후보로 쓴다. */
    fun mockableDirectionKeys(): List<String> = values.filter(MetricReading::isMockable).map(MetricReading::directionKey)

    /** 모든 지표의 방향 key. */
    fun directionKeys(): List<String> = values.map(MetricReading::directionKey)

    /** 지표마다 극값 장을 하나씩 만든다. 장 번호는 1부터다. */
    fun toScenes(context: SceneContext): List<DossierScene> =
        values.mapIndexed { index, reading -> reading.scene(index + 1, context) }
}
