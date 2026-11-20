package com.santacut.domain.dossier.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

/** 표면형 세기. 두 글자 이상은 부분 문자열, 한 글자는 낱말 경계를 본다. */
class TextFormsTest {
    @Test
    fun `한 글자 음절 표면형은 뒤나 앞에 ㅋ·ㅎ·ㅠ·ㅜ가 붙어도 낱말로 센다`() {
        assertEquals(1, TextForms(listOf("헐")).countIn("헐ㅋㅋ"))
        assertEquals(1, TextForms(listOf("응")).countIn("응ㅎㅎ"))
        assertEquals(1, TextForms(listOf("왜")).countIn("왜ㅠㅠ"))
        assertEquals(1, TextForms(listOf("헐")).countIn("ㅋㅋ헐"))
    }

    @Test
    fun `한 글자 표면형은 다른 글자에 붙으면 세지 않고 자모 표면형은 ㅋ가 붙어도 세지 않는다`() {
        assertEquals(0, TextForms(listOf("헐")).countIn("헐랭 대헐"))
        assertEquals(0, TextForms(listOf("ㅇ")).countIn("ㅇㅋ"))
        assertEquals(2, TextForms(listOf("ㅇ")).countIn("ㅇ ㅇ"))
    }

    @Test
    fun `두 글자 이상은 부분 문자열로 겹치지 않게 센다`() {
        assertEquals(2, TextForms(listOf("ㅋㅋ")).countIn("ㅋㅋㅋㅋㅋ"))
        assertEquals(2, TextForms(listOf("진짜", "찐짜")).countIn("진짜? 찐짜!"))
    }

    @Test
    fun `낱말 경계로 세면 두 글자 이상도 앞뒤에 글자가 붙은 자리는 세지 않는다`() {
        val forms = TextForms(listOf("ㄱ?"))
        assertEquals(1, forms.countIn("내일 ㄱ?", wholeWord = true))
        assertEquals(1, forms.countIn("ㄱ??", wholeWord = true))
        assertEquals(0, forms.countIn("ㄱㄱ?", wholeWord = true))
        assertEquals(1, forms.countIn("ㄱㄱ?"))
    }
}
