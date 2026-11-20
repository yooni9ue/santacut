package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 작가 입력 안전장치(실제 rules.json). 고유명사·숫자·영문·링크는 일반명사로 가리고, 민감한 줄은 통째로 뺀다. */
class RoastRulesTest {
    private val rules = TestRoasts.catalog.rules

    @Test
    fun `돈·직장·건강·연애·가족 낱말이나 금액이 든 줄은 통째로 뺀다`() {
        assertNull(rules.scrub("내일 회식 ㄱ?"))
        assertNull(rules.scrub("정산 완료했어"))
        assertNull(rules.scrub("만원씩 보내"))
        assertNull(rules.scrub("12,000원 나왔다"))
        assertNull(rules.scrub("엄마가 부름"))
        assertNull(rules.scrub("병원 다녀옴"))
        assertNotNull(rules.scrub("내일 보드게임 ㄱ?"))
    }

    @Test
    fun `숫자열은 (숫자), 영문 낱말은 (영문)으로 바꾸고 영문 원래 낱말은 차단 목록에 넣는다`() {
        val scrubbed = assertNotNull(rules.scrub("7시에 starbucks 앞에서 보자"))

        assertEquals("(숫자)시에 (영문) 앞에서 보자", scrubbed.text)
        assertEquals(listOf("starbucks"), scrubbed.masked)
    }

    @Test
    fun `사전 고유명사는 낱말 앞 경계에서 바꾸고 사전 낱말로 시작하는 흔한 말은 그대로 둔다`() {
        val scrubbed = assertNotNull(rules.scrub("서울에서 펜타포트 가자 대만족"))

        assertEquals("(장소)에서 (행사) 가자 대만족", scrubbed.text)
        assertTrue(scrubbed.masked.containsAll(listOf("서울", "펜타포트")))
        assertEquals("성수기라 부산하네", assertNotNull(rules.scrub("성수기라 부산하네")).text)
    }

    @Test
    fun `사전 지명 뒤에 다른 말을 붙여 써도 가린다`() {
        assertEquals("(장소) (장소)여행 (장소)쪽", assertNotNull(rules.scrub("홍대입구 제주여행 강남쪽")).text)
        assertEquals("(장소)가자 (행사)갔다", assertNotNull(rules.scrub("서산가자 펜파포트갔다")).text)
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
    fun `장소 접미사 패턴은 두 글자 이상 줄기에만 붙이고 조사 도와 흔한 낱말은 건너뛴다`() {
        assertEquals("(장소)에서 보자", assertNotNull(rules.scrub("내포항에서 보자")).text)
        assertEquals("(장소) 어때", assertNotNull(rules.scrub("화담숲 어때")).text)
        assertEquals("(장소)에 가자", assertNotNull(rules.scrub("제주도에 가자")).text)
        assertEquals("(장소) 앞", assertNotNull(rules.scrub("망원역 앞")).text)
        assertEquals("(숫자) 까지 (장소) (숫자)동 앞", assertNotNull(rules.scrub("7 까지 은하마을한빛아파트 101동 앞")).text)
        assertEquals("(장소)이나 (장소)?", assertNotNull(rules.scrub("수원이나 안양?")).text)
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
