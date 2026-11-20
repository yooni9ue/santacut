package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 작가 입력 안전장치(실제 rules.json). 사람 이름·사는 곳 이름·숫자·영문·링크는 일반명사로 가리고, 민감한 줄은 통째로 뺀다. 지명은 그대로 둔다. */
class RoastRulesTest {
    private val rules = TestRoasts.catalog.rules

    @Test
    fun `돈 낱말이나 금액이 든 줄은 통째로 뺀다`() {
        assertNull(rules.scrub("정산 완료했어"))
        assertNull(rules.scrub("만원씩 보내"))
        assertNull(rules.scrub("12,000원 나왔다"))
        assertNotNull(rules.scrub("내일 보드게임 ㄱ?"))
    }

    @Test
    fun `직장·건강·연애·가족 낱말이 든 줄은 빼지 않고 보낸다`() {
        listOf("내일 회식 ㄱ?", "엄마가 부름", "병원 다녀옴", "남편이 늦음", "와이프 허락", "응급실 감", "퇴근하고 봄", "노조 분위기 어떰")
            .forEach { text -> assertNotNull(rules.scrub(text), text) }
    }

    @Test
    fun `원 없는 금액, 줄 끝 돈과 빚이 든 줄도 통째로 뺀다`() {
        listOf("빚 갚아야 함", "3만 보내", "2천 남음", "돈좀", "그거 다 돈", "1억 모음").forEach { text -> assertNull(rules.scrub(text), text) }
    }

    @Test
    fun `민감 낱말과 꼴만 비슷한 말은 빼지 않는다`() {
        listOf("만두 빚어 먹자", "3시에 만나", "돈까스 먹자").forEach { text -> assertNotNull(rules.scrub(text), text) }
    }

    @Test
    fun `숫자열은 (숫자), 영문 낱말은 (영문)으로 바꾸고 영문 원래 낱말은 차단 목록에 넣는다`() {
        val scrubbed = assertNotNull(rules.scrub("7시에 starbucks 앞에서 보자"))

        assertEquals("(숫자)시에 (영문) 앞에서 보자", scrubbed.text)
        assertEquals(listOf("starbucks"), scrubbed.masked)
    }

    @Test
    fun `지명·가게·행사 이름은 가리지 않고 차단 목록에도 넣지 않는다`() {
        val text = "서울역에서 펜타포트 가자 홍대입구 제주도에 스벅 배낚시하자 편의점 부동산 비상구"

        val scrubbed = assertNotNull(rules.scrub(text))

        assertEquals(text, scrubbed.text)
        assertTrue(scrubbed.masked.isEmpty())
        assertTrue(rules.suspectsIn(text).isEmpty())
    }

    @Test
    fun `호칭 없이 부른 이름은 (이름)으로 바꾸고 줄기를 이름 목록에 넣는다`() {
        val scrubbed = assertNotNull(rules.scrub("동준아 민수야 지은이랑 성필이는 현규한테"))

        assertEquals("(이름)아 (이름)야 (이름)이랑 (이름)이는 (이름)한테", scrubbed.text)
        assertEquals(listOf("동준", "민수", "지은", "성필", "현규"), scrubbed.names)
    }

    @Test
    fun `부르는 말과 꼴이 같은 용언과 흔한 말은 이름으로 보지 않는다`() {
        val text = "괜찮아 먹어야 할거야 얘들아 친구야 누구한테 진짜야 놀아"

        val scrubbed = assertNotNull(rules.scrub(text))

        assertEquals(text, scrubbed.text)
        assertTrue(scrubbed.names.isEmpty())
    }

    @Test
    fun `호칭에서 떼어 낸 줄기도 이름 목록에 넣는다`() {
        assertEquals(listOf("현규"), assertNotNull(rules.scrub("현규형 모여")).names)
    }

    @Test
    fun `아파트·빌라 같은 사는 곳 이름은 주소에 가까워 (집)으로 가린다`() {
        val scrubbed = assertNotNull(rules.scrub("7 까지 은하마을한빛아파트 101동 앞"))

        assertEquals("(숫자) 까지 (집) (숫자)동 앞", scrubbed.text)
        assertEquals(listOf("은하마을한빛아파트"), scrubbed.masked)
        assertEquals("(집)에서 보자", assertNotNull(rules.scrub("해피빌라에서 보자")).text)
        assertEquals("오늘도 그래도 친구 역시 운동", assertNotNull(rules.scrub("오늘도 그래도 친구 역시 운동")).text)
    }

    @Test
    fun `호칭 패턴은 사람 부르는 말을 (이름)으로 바꾸고 원래 낱말을 차단 목록에 넣는다`() {
        val scrubbed = assertNotNull(rules.scrub("현규형 행님들 모여"))

        assertEquals("(이름) (이름)들 모여", scrubbed.text)
        assertEquals(listOf("현규형", "행님"), scrubbed.masked)
    }

    @Test
    fun `출력 숫자 검사는 허용 표현을 빼고 아라비아 숫자와 한글 수량을 찾는다`() {
        assertEquals("세 번", rules.numberIn("ㄱ?를 세 번이나 보냈다"))
        assertEquals("3", rules.numberIn("3월에 몰렸다"))
        assertEquals("절반", rules.numberIn("말풍선 절반이 물음표다"))
        assertNull(rules.numberIn("한 해 동안 한마디도 안 졌다"))
        assertNull(rules.numberIn("이것만 해도 충분하다"))
    }

    @Test
    fun `출력 숫자 검사는 한자어 수, 어림수, 붙여 쓴 수, 여러 번 같은 횟수 말도 찾는다`() {
        listOf(
            "삼십 분", "오 분", "이십 번", "백 번", "한두 번", "두세", "서너", "너댓", "대여섯", "열두 번", "스물한 번", "여러 번", "수차례", "두어 번",
        ).forEach { phrase -> assertNotNull(rules.numberIn("벌써 $phrase 넘게 보냈다"), phrase) }
    }

    @Test
    fun `출력 숫자 검사는 수가 아닌 흔한 말을 숫자로 보지 않는다`() {
        listOf(
            "생각한 시간이 길다", "이번엔 참았다", "구분이 안 된다", "이 분은 늘 바쁘다", "그대로 두어라", "만일 그렇다면", "천천히 보낸다", "만나서 놀자",
        ).forEach { text -> assertNull(rules.numberIn(text), text) }
    }

    @Test
    fun `비속어와 사람 부르는 입버릇, 긴 말은 인용 후보로 넘기지 않는다`() {
        assertTrue(rules.isQuotable("giyeok_q", "ㄱ?"))
        assertTrue(!rules.isQuotable("jojija", "조지자"))
        assertTrue(!rules.isQuotable("haengnimdeul", "행님들"))
        assertTrue(!rules.isQuotable("a_geurae_q", "아 그래?"))
    }
}
