package com.santacut.domain.roast.model

/** 작가 입력의 막 하나. [lines]는 본인 말풍선만 `줄번호|월-일 시|본문`으로 적은 것이다. */
data class RoastTranscriptAct(val act: Int, val months: String, val lines: List<String>)
