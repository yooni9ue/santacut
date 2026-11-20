package com.santacut.domain.roast.model

/** 세 막 줄거리의 한 줄. [act]는 1~3이다. */
data class RoastStoryLine(val act: Int, val line: String, val source: RoastSource = RoastSource.AI)
