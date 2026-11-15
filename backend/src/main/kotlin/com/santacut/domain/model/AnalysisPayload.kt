package com.santacut.domain.model

/**
 * 작업에 저장하는 분석 요청 JSON.
 */
class AnalysisPayload(val json: String) {
    /** 같은 키로 다른 본문이 왔는지 가리는 요청 해시. */
    fun hash(): String = Sha256.hex(json)
}
