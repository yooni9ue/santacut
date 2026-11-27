package com.santacut.domain.roast.model

/**
 * 구간이 가진 표현 하나(roast/punch.json의 band_marks). 구간 이름('프리패스', '유턴')이나 그 구간에만 맞는 밈('체포하겠', '줴줴이야')이다.
 * [bands]에 들지 않은 구간의 판정 한 줄·결론 칭호·마지막 한 줄이 이 표현을 쓰면 떨어진다. 75~89점 덱에 90점 이상 구간 이름이,
 * 50~54점 덱에 석탄 구간 밈이 붙지 않게 한다.
 *
 * @property mark 따옴표 인용 밖에서 글자 그대로 찾는 표현
 * @property bands 이 표현을 쓸 수 있는 등급 이름([com.santacut.domain.analysis.model.GiftGrade.name])
 */
data class BandMark(val mark: String, val bands: List<String>)
