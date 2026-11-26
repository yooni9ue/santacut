package com.santacut.infrastructure.share.render

/**
 * 공개 공유 페이지에 그릴 리캡 카드 덱. 값은 표시 형식을 맞춘 문자열이고, HTML 이스케이프는 하기 전이다.
 *
 * @property gift 선물 스킨이면 true, 석탄 스킨이면 false
 * @property slides 카드 목록. 공유할 때 저장한 순서 그대로다.
 */
data class ShareDeck(
    val gift: Boolean,
    val slides: ShareSlides,
)
