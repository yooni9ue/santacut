package com.santacut.domain.roast.model

/**
 * 작가가 써도 되는 유행어 하나(roast/slang.json의 allowed). 작가 입력에는 표기와 뜻만 나가고(예문은 그대로 새지 않게 사람용으로만 둔다), 서버는 [stem]으로 센다.
 *
 * @property stem 활용형까지 잡으려고 세는 줄기(엔믹스하다 → 엔믹스)
 */
data class SlangWord(
    val word: String,
    val stem: String,
    val meaning: String,
    val example: String,
)
