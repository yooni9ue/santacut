package com.santacut.domain.share.model

/**
 * 공유 링크 목록.
 */
class Shares(private val values: List<Share>) {
    /** 공유 링크가 붙잡고 있어 지우면 안 되는 스냅숏 ID. */
    fun snapshotIds(): Set<String> = values.mapTo(HashSet()) { it.snapshotId }
}
