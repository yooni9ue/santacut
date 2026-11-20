package com.santacut.domain.roast.model

import com.santacut.TestRoasts
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 작가 입력 안전장치(실제 rules.json). 사람 이름·사는 곳 이름·숫자·영문·링크는 일반명사로 가리고, 지명과 줄 내용은 그대로 둔다. */
class RoastRulesTest {
    private val rules = TestRoasts.catalog.rules

    @Test
    fun `돈·직장·건강·가족 이야기도 줄째 빼지 않고 숫자만 가려 보낸다`() {
        assertEquals("정산 완료했어", rules.scrub("정산 완료했어").text)
        assertEquals("엄마가 부름 병원 다녀옴 회식 ㄱ?", rules.scrub("엄마가 부름 병원 다녀옴 회식 ㄱ?").text)
        assertEquals("(숫자)원 나왔다 (숫자)만 보내", rules.scrub("12,000원 나왔다 3만 보내").text)
    }

    @Test
    fun `숫자열은 (숫자), 영문 낱말은 (영문)으로 바꾸고 영문 원래 낱말은 차단 목록에 넣는다`() {
        val scrubbed = rules.scrub("7시에 starbucks 앞에서 보자")

        assertEquals("(숫자)시에 (영문) 앞에서 보자", scrubbed.text)
        assertEquals(listOf("starbucks"), scrubbed.masked)
    }

    @Test
    fun `지명·가게·행사 이름은 가리지 않고 차단 목록에도 넣지 않는다`() {
        val text = "서울역에서 펜타포트 가자 홍대입구 제주도에 스벅 배낚시하자 편의점 부동산 비상구"

        val scrubbed = rules.scrub(text)

        assertEquals(text, scrubbed.text)
        assertTrue(scrubbed.masked.isEmpty())
        assertTrue(rules.suspectsIn(text).isEmpty())
    }

    @Test
    fun `호칭 없이 부른 이름은 (이름)으로 바꾸고 줄기를 이름 목록에 넣는다`() {
        val scrubbed = rules.scrub("동준아 민수야 지은이랑 성필이는 현규한테")

        assertEquals("(이름)아 (이름)야 (이름)이랑 (이름)이는 (이름)한테", scrubbed.text)
        assertEquals(listOf("동준", "민수", "현규"), scrubbed.names)
        assertEquals(listOf("동준", "민수", "현규"), scrubbed.masked)
    }

    @Test
    fun `부르는 말과 꼴이 같은 용언과 흔한 말은 이름으로 보지 않는다`() {
        val text = "괜찮아 먹어야 할거야 얘들아 친구야 누구한테 진짜야 놀아"

        val scrubbed = rules.scrub(text)

        assertEquals(text, scrubbed.text)
        assertTrue(scrubbed.names.isEmpty())
    }

    @Test
    fun `호칭 접미사로 끝나는 흔한 말은 이름으로 보지 않는다`() {
        val text = "곰인형 아저씨 손글씨 주인님"

        assertEquals(text, rules.scrub(text).text)
        assertTrue(rules.suspectsIn(text).isEmpty())
    }

    @Test
    fun `서술격 조사와 흔한 말은 부르는 말이나 조사가 붙어도 이름으로 보지 않는다`() {
        val text = "무슨 소리야 그게 말이야 문제야 얘기야 별로야 집이야 치킨이랑 가족이랑 엄마한테 회사한테 움직이는"

        val scrubbed = rules.scrub(text)

        assertEquals(text, scrubbed.text)
        assertTrue(scrubbed.names.isEmpty())
    }

    @Test
    fun `호칭에서 떼어 낸 줄기도 이름 목록에 넣는다`() {
        assertEquals(listOf("현규"), rules.scrub("현규형 모여").names)
    }

    @Test
    fun `아파트·빌라 같은 사는 곳 이름은 주소에 가까워 (집)으로 가린다`() {
        val scrubbed = rules.scrub("7 까지 은하마을한빛아파트 101동 앞")

        assertEquals("(숫자) 까지 (집) (숫자)동 앞", scrubbed.text)
        assertEquals(listOf("은하마을한빛아파트"), scrubbed.masked)
        assertEquals("(집)에서 보자", rules.scrub("해피빌라에서 보자").text)
        assertEquals("오늘도 그래도 친구 역시 운동", rules.scrub("오늘도 그래도 친구 역시 운동").text)
    }

    @Test
    fun `호칭 패턴은 사람 부르는 말을 (이름)으로 바꾸고 원래 낱말을 차단 목록에 넣는다`() {
        val scrubbed = rules.scrub("현규형 행님들 모여")

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
            "점수 만회했다", "십분 이해한다", "벚꽃이 만개했다", "만년 막내다",
        ).forEach { text -> assertNull(rules.numberIn(text), text) }
    }

    @Test
    fun `출력 숫자 검사는 셈말(한 포대, 세 줄, 하나, 첫째)과 띄어 쓴 한 마디도 찾는다`() {
        listOf(
            "석탄 한 포대 보낸다", "올해를 세 줄로 줄였다", "물음표 하나로 버텼다", "하나도 안 놓쳤다", "둘이서 다 정했다", "첫째로 묻는다",
            "첫 번째 말풍선", "첫마디가 물음표다", "한 마디도 안 졌다", "두 잔 마셨다", "한판 붙자", "몇 바퀴 돌았다",
        ).forEach { text -> assertNotNull(rules.numberIn(text), text) }
    }

    @Test
    fun `붙여 쓴 한마디는 짧은 말이라는 한 낱말이라 셈말로 보지 않고, 셈말과 꼴만 같은 말도 숫자로 보지 않는다`() {
        listOf(
            "한마디만 하고 빠진다", "한마디도 안 졌다", "하나같이 물음표다", "첫눈 오는 날", "첫 장면부터 웃겼다", "네 판단이 맞았다", "둘러보고 왔다",
            "셋업 끝났다", "한데 모아 보낸다", "하루 늦었다",
        ).forEach { text -> assertNull(rules.numberIn(text), text) }
    }

    @Test
    fun `하나 뒤 용언, 하나로마트, 네(너의) 뒤 낱말, 한층·한시도·여러분은 셈말로 보지 않는다`() {
        listOf(
            "뭘 하나 봤더니", "일을 하나 싶었다", "뭘 하나 했더니", "이 방은 네 판이다", "하나로마트에서 장 봤다", "네 장부에 적었다", "네 자판이 바쁘다",
            "네 분위기가 좋다", "네 차례다", "한 장면도 안 놓쳤다", "한층 더 빨라졌다", "한시도 안 쉰다", "여러분 모여라", "네 명단을 봤다", "네 시간표다",
        ).forEach { text -> assertNull(rules.numberIn(text), text) }
    }

    @Test
    fun `단위 명사 켤레·박스·봉지·상자·대·그릇과 넷째·다섯째·하나둘·하나하나·일 년 치도 셈말이다`() {
        listOf(
            "양말 두 켤레", "귤 한 박스", "과자 세 봉지", "한 상자 보낸다", "한 대 맞았다", "국밥 한 그릇", "넷째로 묻는다", "다섯째 날", "하나둘씩 모였다",
            "하나하나 셌다", "일 년 치 말풍선", "물음표 하나 보냈다", "세 번이나", "한 번쯤", "두 번보다", "한 달 내내",
        ).forEach { text -> assertNotNull(rules.numberIn(text), text) }
    }

    @Test
    fun `말투 검사는 옛말투 어미, 훈화 낱말, 유행어, 이모지, 느낌표, ㅋㅋ를 찾는다`() {
        listOf(
            "먼저 하거라" to "하거라", "물어보아라" to "보아라", "다 읽었구나." to "구나", "잘 읽었습니다" to "습니다", "성미가 급하다" to "성미",
            "근사한 버릇이다" to "근사한", "새날을 연다" to "새날", "추진력 갑" to "갑", "답장 갑이다" to "갑이다", "레전드다" to "레전드", "찐이다" to "찐",
            "무지성 출발" to "무지성", "빠르다!" to "!", "ㅋㅋ 친다" to "ㅋㅋ", "선물이다\u2744" to "\u2744",
        ).forEach { (text, found) -> assertEquals(found, rules.toneIn(text, false), text) }
    }

    @Test
    fun `말투 검사는 꼴만 같은 흔한 말을 잡지 않고, 쪽지는 맨 끝 거라만 허용한다`() {
        listOf("갑자기 조용해졌다", "지갑을 열었다", "반갑더라", "찐빵을 샀다", "도구나 장비", "더구나 물음표다", "ㅋ 하나는 친다").forEach { text ->
            assertNull(rules.toneIn(text, false), text)
        }
        assertNull(rules.toneIn("내년에도 먼저 말을 꺼내거라.", true))
        assertNull(rules.toneIn("썰매는 내가 몰마.", true))
        assertEquals("하거라", rules.toneIn("먼저 하거라. 내년에 보자.", true))
        assertEquals("하거라", rules.toneIn("내년에도 먼저 하거라.", false))
    }

    @Test
    fun `옛 명령형(거라, 오너라, 두어라, 보아라)과 ㅂ니다체 어미는 문장 끝에서 잡고 봐라·해라 같은 반말 명령은 둔다`() {
        listOf("오거라.", "말해 보거라.", "그대로 두어라.", "다 주어라", "썰매에 오너라.", "두고 갑니다.", "문 앞에 둡니다", "이제 보아라.", "먼저 하여라.")
            .forEach { text -> assertNotNull(rules.toneIn(text, false), text) }
        listOf("그만 봐라.", "빨리 해라.", "많이 먹어라.", "좀 참아라.", "그대로 둬라.", "갈 거라 믿는다.", "몇 시에 갑니까 묻는다", "올해 선물 갑니다만 아니다")
            .forEach { text -> assertNull(rules.toneIn(text, false), text) }
        assertNull(rules.toneIn("내년에도 먼저 오거라.", true))
        assertEquals("두어라", rules.toneIn("그대로 두어라. 내년에 보자.", true))
    }

    @Test
    fun `구나는 문장 끝 어미만 잡고 누구나·축구나·야구나·아구나 같은 낱말은 두지 않는다`() {
        listOf("누구나 아는 버릇이다", "축구나 보자고 졸랐다", "야구나 농구나", "아구나 먹자고").forEach { text -> assertNull(rules.toneIn(text, false), text) }
        listOf("다 읽었구나.", "그랬구나", "빠르구나, 손이").forEach { text -> assertEquals("구나", rules.toneIn(text, false), text) }
    }

    @Test
    fun `갑은 조사가 붙어도 잡되 담배 한 갑은 두고, 찐득·성미산은 두며 별·알람 이모지도 잡는다`() {
        listOf("추진력 갑이라", "답장 갑인 사람", "속도는 갑은 갑", "갑을 넘는다").forEach { text -> assertNotNull(rules.toneIn(text, false), text) }
        listOf("담배 한 갑 샀다", "찐득한 손", "찐덕찐덕하다", "성미산 산책").forEach { text -> assertNull(rules.toneIn(text, false), text) }
        assertEquals("\u2B50", rules.toneIn("선물이다\u2B50", false))
        assertEquals("\u23F0", rules.toneIn("알람\u23F0 맞췄다", false))
    }

    @Test
    fun `뒤집기는 근데와 한 칸 띄움으로 시작해야 한다`() {
        assertTrue(rules.startsFlipRightly("근데 그 물음표 없었으면 이 방은 진작 눈 덮였다."))
        assertFalse(rules.startsFlipRightly("그 물음표 없었으면 이 방은 진작 눈 덮였다."))
        assertFalse(rules.startsFlipRightly("근데그 물음표 없었으면 이 방은 진작 눈 덮였다."))
    }

    @Test
    fun `비속어와 사람 부르는 입버릇, 긴 말은 인용 후보로 넘기지 않는다`() {
        assertTrue(rules.isQuotable("giyeok_q", "ㄱ?"))
        assertTrue(!rules.isQuotable("jojija", "조지자"))
        assertTrue(!rules.isQuotable("haengnimdeul", "행님들"))
        assertTrue(!rules.isQuotable("a_geurae_q", "아 그래?"))
    }
}
