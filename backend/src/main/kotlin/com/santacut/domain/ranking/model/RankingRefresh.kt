package com.santacut.domain.ranking.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

/**
 * 순위를 다시 계산해야 하는지 나타내는 표시. 행은 [ID] 하나뿐이다.
 * 순위에 영향을 주는 변경은 [requestedVersion]을 하나 올리기만 하고, 예약 작업이 모아서 한 번에 다시 계산한다.
 * 다시 계산을 시작할 때 읽은 판까지만 [rebuiltVersion]으로 표시하므로, 계산 도중 들어온 요청은 다음 계산이 맡는다.
 *
 * @property requestedVersion 다시 계산을 요청한 횟수
 * @property rebuiltVersion 다시 계산에 반영한 마지막 요청 판
 */
@Entity
@Table(name = "ranking_refresh")
class RankingRefresh(
    id: Int,
    requestedVersion: Long,
    rebuiltVersion: Long,
) {
    @Id
    @Column(name = "id")
    var id: Int = id
        protected set

    @Column(name = "requested_version", nullable = false)
    var requestedVersion: Long = requestedVersion
        protected set

    @Column(name = "rebuilt_version", nullable = false)
    var rebuiltVersion: Long = rebuiltVersion
        protected set

    /** 아직 순위에 반영하지 않은 요청이 있는지. */
    fun isDirty(): Boolean = requestedVersion > rebuiltVersion

    companion object {
        const val ID = 1
        private const val FIRST_REQUEST = 1L
        private const val NOTHING_REBUILT = 0L

        /** 표시 행이 없던 DB에서 처음 만드는 행. 앱이 뜨면 한 번 다시 계산하도록 요청된 상태로 둔다. */
        fun requested(): RankingRefresh = RankingRefresh(ID, FIRST_REQUEST, NOTHING_REBUILT)
    }
}
