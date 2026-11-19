package com.santacut.domain.dossier.model

/**
 * 제목과 메모 문구 한 쌍(copy.json의 `hook`, `hook_fallback`).
 */
data class HeadlineMemoCopy(val headline: Templates, val memo: Templates)
