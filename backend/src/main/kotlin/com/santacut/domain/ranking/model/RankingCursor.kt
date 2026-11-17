package com.santacut.domain.ranking.model

import com.santacut.domain.global.exception.ErrorCode
import com.santacut.domain.global.exception.SantaCutException
import java.time.Instant
import java.util.Base64

/**
 * 순위표 페이지 위치. 직전 페이지 마지막 줄의 배송 순서 다음부터 최대 [PAGE_SIZE]줄을 읽는다.
 * 어느 순위 계산([computedAt])의 페이지인지도 함께 담아, 페이지 사이에 순위가 다시 계산되면 줄이 겹치거나 빠지지 않게 거절한다.
 * 클라이언트에는 불투명한 문자열([value])로만 주고받는다.
 *
 * @property afterSequence 이 배송 순서보다 뒤의 줄만 읽는다. 첫 페이지는 0이다.
 * @property computedAt 이 cursor를 만든 순위 계산 시각. 첫 페이지는 null이다.
 */
class RankingCursor private constructor(val afterSequence: Int, private val computedAt: Instant?) {
    val pageSize: Int = PAGE_SIZE

    /** 다음 페이지가 있는지 알기 위해 한 줄 더 읽는다. */
    val fetchSize: Int = PAGE_SIZE + 1

    fun isFirstPage(): Boolean = computedAt == null

    /** 응답의 next_cursor로 내보내는 문자열. 순위 계산 시각(밀리초)과 배송 순서를 이어 Base64URL로 감싼다. */
    fun value(): String =
        ENCODER.encodeToString("${computedAt?.toEpochMilli() ?: NO_SNAPSHOT_MILLIS}$SEPARATOR$afterSequence".toByteArray())

    /** 이 cursor의 순위 계산이 지금 순위([latestComputedAt])와 다르면 [ErrorCode.RANKING_SNAPSHOT_CHANGED]다. */
    fun verifySnapshot(latestComputedAt: Instant) {
        val cursorComputedAt = computedAt ?: return
        if (cursorComputedAt.toEpochMilli() != latestComputedAt.toEpochMilli()) {
            throw SantaCutException(ErrorCode.RANKING_SNAPSHOT_CHANGED)
        }
    }

    companion object {
        private const val PAGE_SIZE = 50
        private const val FIRST_PAGE_SEQUENCE = 0
        private const val SEPARATOR = '.'
        private const val NO_SNAPSHOT_MILLIS = 0L
        private const val PART_COUNT = 2
        private val ENCODER = Base64.getUrlEncoder().withoutPadding()

        fun first(): RankingCursor = RankingCursor(FIRST_PAGE_SEQUENCE, null)

        fun after(deliverySequence: Int, computedAt: Instant): RankingCursor = RankingCursor(deliverySequence, computedAt)

        /** 요청의 cursor. 없으면 첫 페이지이고, 이전 응답이 준 값이 아니면 [ErrorCode.INVALID_CURSOR]다. */
        fun from(raw: String?): RankingCursor {
            if (raw == null) return first()
            val parts = decode(raw)?.split(SEPARATOR)?.takeIf { it.size == PART_COUNT }
                ?: throw SantaCutException(ErrorCode.INVALID_CURSOR)
            val computedAtMillis = parts[0].toLongOrNull()
            val sequence = parts[1].toIntOrNull()
            if (computedAtMillis == null || sequence == null || sequence <= FIRST_PAGE_SEQUENCE) {
                throw SantaCutException(ErrorCode.INVALID_CURSOR)
            }
            return after(sequence, Instant.ofEpochMilli(computedAtMillis))
        }

        private fun decode(raw: String): String? =
            runCatching { String(Base64.getUrlDecoder().decode(raw)) }.getOrNull()
    }
}
