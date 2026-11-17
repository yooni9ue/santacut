package com.santacut

import java.util.UUID

/**
 * 통합 테스트가 함께 쓰는 참가자 등록 값. 모든 통합 테스트가 같은 DB를 쓰므로 닉네임은 매번 새로 만든다.
 */
object TestParticipants {
    /** seed.sql이 채우는 지역 중 하나(서울 종로구). */
    const val REGION_CODE = "KR-11-110"

    private const val NICKNAME_PREFIX = "t"
    private const val NICKNAME_RANDOM_LENGTH = 12

    /** 닉네임 규칙을 지키면서 다른 테스트와 겹치지 않는 닉네임. */
    fun uniqueNickname(): String =
        NICKNAME_PREFIX + UUID.randomUUID().toString().replace("-", "").take(NICKNAME_RANDOM_LENGTH)
}
