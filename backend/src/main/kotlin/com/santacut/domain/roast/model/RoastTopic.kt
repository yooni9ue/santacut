package com.santacut.domain.roast.model

/** 작가 입력의 막별 화제 후보. 조서 주제 사전의 일반명사 표기다. */
data class RoastTopic(val act: Int, val months: String, val candidates: List<String>)
