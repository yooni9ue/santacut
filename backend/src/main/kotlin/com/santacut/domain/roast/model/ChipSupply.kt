package com.santacut.domain.roast.model

/**
 * 근거 카드에 쓸 수 있는 칩 수를 방향별로 센 값. 슬롯을 나눌 때([RoastSlots.limitedTo]) 종류마다 방향이 맞는 칩부터 먼저 배정하고,
 * 어느 쪽으로도 쓰는 칩([ChipDirection.EITHER])은 모자란 종류에 나눠 준다. 중립 칩은 세지 않는다.
 *
 * @property good 이유·살아남은 말에만 쓰는 좋은 쪽 칩 수
 * @property bad 깎인 말에만 쓰는 나쁜 쪽 칩 수(고마워 칩 포함)
 * @property either 어느 종류에나 쓰는 칩 수
 */
data class ChipSupply(val good: Int, val bad: Int, val either: Int) {
    companion object {
        val NONE = ChipSupply(0, 0, 0)

        /** 방향을 가리지 않는 칩 [count]개. 방향 없는 옛 칩·테스트에서 쓴다. */
        fun either(count: Int): ChipSupply = ChipSupply(0, 0, count)
    }
}
