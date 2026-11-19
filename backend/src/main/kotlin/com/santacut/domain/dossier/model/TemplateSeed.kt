package com.santacut.domain.dossier.model

/**
 * 템플릿을 고르는 결정적 시드. 결과(평가) ID에 쓰임새([of])를 이어 만들어, 같은 결과는 다시 열어도 같은 문구를 받는다.
 */
class TemplateSeed(private val value: String) {
    /** [purpose] 쓰임새의 시드. 같은 결과라도 쓰임새마다 다른 문구를 고르게 한다. */
    fun of(purpose: String): TemplateSeed = TemplateSeed(value + SEPARATOR + purpose)

    /** 0 이상 [size] 미만의 번호. */
    fun indexFor(size: Int): Int = Math.floorMod(value.hashCode(), size)

    companion object {
        private const val SEPARATOR = "|"
    }
}
