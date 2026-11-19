package com.santacut.domain.dossier.model

/**
 * 저장하기 전의 조서 집계. 집계와 JSON 직렬화는 CPU만 쓰므로 쓰기 트랜잭션 밖에서 미리 만들어 둔다.
 *
 * @property statsJson 집계값([DossierStats]) JSON. 원문, 이름, 링크, 전화번호는 담지 않는다.
 * @property statsVersion 집계 규칙 판([DossierStats.VERSION])
 */
data class DossierDraft(val statsJson: String, val tone: DossierTone, val statsVersion: Int)
