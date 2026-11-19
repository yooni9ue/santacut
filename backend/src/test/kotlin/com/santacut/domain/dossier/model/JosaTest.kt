package com.santacut.domain.dossier.model

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class JosaTest {
    @Test
    fun `받침이 있으면 을·이·은·과·으로, 없으면 를·가·는·와·로를 고른다`() {
        assertEquals(listOf("을", "이", "은", "과", "으로"), Josa.entries.map { josa -> josa.after("밥") })
        assertEquals(listOf("를", "가", "는", "와", "로"), Josa.entries.map { josa -> josa.after("진짜") })
    }

    @Test
    fun `ㄹ 받침 뒤에는 으로 대신 로를 쓴다`() {
        assertEquals("로", Josa.DIRECTION.after("헐"))
        assertEquals("을", Josa.OBJECT.after("헐"))
    }

    @Test
    fun `숫자는 읽는 소리로 받침을 정한다`() {
        assertEquals("이", Josa.SUBJECT.after("3"))
        assertEquals("가", Josa.SUBJECT.after("2"))
        assertEquals("로", Josa.DIRECTION.after("7"))
        assertEquals("으로", Josa.DIRECTION.after("10"))
    }

    @Test
    fun `끝의 따옴표와 괄호는 건너뛰고 퍼센트는 받침 없이 본다`() {
        assertEquals("을", Josa.OBJECT.after("'밥'"))
        assertEquals("를", Josa.OBJECT.after("23%"))
        assertEquals("이", Josa.SUBJECT.after("1막(1~4월)"))
    }

    @Test
    fun `템플릿에 어느 꼴로 적어도 같은 조사로 본다`() {
        assertEquals(Josa.OBJECT, Josa.of("를"))
        assertEquals(Josa.DIRECTION, Josa.of("으로"))
        assertEquals(null, Josa.of("에"))
    }
}
