package com.santacut.domain.dossier.model

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * 입버릇 장부 문구(copy.json의 `ledger`).
 *
 * @property memoByCategory 1위 말의 분류별 요정 메모
 * @property bleep 삐 처리 횟수 줄. 화면 전용이라 카드에 싣지 않는다.
 */
data class LedgerCopy(
    val headline: Templates,
    @JsonProperty("memo_by_category") val memoByCategory: KeyedTemplates,
    val bleep: Templates,
)
