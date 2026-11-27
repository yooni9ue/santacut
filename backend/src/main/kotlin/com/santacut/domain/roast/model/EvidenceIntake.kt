package com.santacut.domain.roast.model

/**
 * 응답 근거 카드를 받아 둔 카드에 더한 결과. 검증은 통과했지만 이미 받은 카드와 겹치거나 슬롯이 차서 받지 않은 카드의 사유를 함께 쥔다.
 * 사유는 다시 쓰기 요청에 실어 작가가 왜 그 카드가 빠졌는지 알게 한다.
 *
 * @property kept 더한 뒤의 받아 둔 카드
 * @property reasons 받지 않은 카드의 사유("evidence[1] 이미 받은 카드와 겹쳐 받지 않았다: ..." 꼴)
 */
class EvidenceIntake(val kept: EvidenceDrafts, val reasons: List<String>)
