package com.santacut.domain.ranking.model

/**
 * 순위에 영향을 주는 변경(심사 성공, 지역 변경, 참가자 삭제)이 순위 다시 계산을 요청했다는 알림.
 * 받는 쪽은 트랜잭션이 커밋된 뒤에 곧바로 다시 계산을 예약한다. 요청 표시([RankingRefresh])는 이 알림과 상관없이 같은 트랜잭션에 남는다.
 */
object RankingRebuildRequested
