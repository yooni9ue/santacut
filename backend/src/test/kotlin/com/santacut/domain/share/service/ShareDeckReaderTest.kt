package com.santacut.domain.share.service

import com.santacut.TestDossiers
import com.santacut.infrastructure.share.render.ShareCard
import com.santacut.infrastructure.share.render.ShareDeck
import com.santacut.infrastructure.share.render.SharePageRenderer
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 저장 판마다 공유 리캡 덱 읽기. 이미 저장한 1판 덱은 옛 카드 종류 그대로, 2판 덱은 새 카드 종류로 그린다. */
class ShareDeckReaderTest {
    private val mapper = TestDossiers.mapper
    private val renderer = SharePageRenderer()

    @Test
    fun `저장한 1판 덱은 쪽지·줄거리·유형·항목 막대 카드까지 모두 그린다`() {
        val html = render(read(V1_JSON))

        assertTrue(html.contains("공유 당시 리캡 카드 <strong>6장</strong>"), html)
        listOf("판정", "팩폭", "산타 쪽지", "올해 줄거리", "산타 유형", "항목 점수", "단톡방 자판기", "근데 다정함", "올해 고마웠어", "봄의 시작", "약속 요정")
            .forEach { fragment -> assertTrue(html.contains(fragment), fragment) }
    }

    @Test
    fun `2판 덱은 새 카드 종류와 상단 순위 바를 그린다`() {
        val html = render(read(V2_JSON))

        assertTrue(html.contains("공유 당시 리캡 카드 <strong>3장</strong>"), html)
        listOf("판정", "선물 받는 이유 1", "올해의 결론", "<span class=\"card-rank\">전국 3등 · 상위 50%</span>", "“내일 몇 시에 볼까”", "#산타컷 #걸쳐서통과")
            .forEach { fragment -> assertTrue(html.contains(fragment), fragment) }
    }

    @Test
    fun `회귀 - 참가자가 적어 상위 비율이 없는 2판 덱은 상단 바에 순위만 그린다`() {
        val html = render(read(V2_JSON.replace(",\"topPercent\":50", "")))

        assertTrue(html.contains("<span class=\"card-rank\">전국 3등</span>"), html)
        assertFalse(html.contains("· 상위"), html)
    }

    @Test
    fun `모르는 판은 예외 없이 null이라 요약 화면으로 보인다`() {
        assertNull(ShareDeckReader.read("{\"version\":99,\"skin\":\"GIFT\",\"cards\":[]}", mapper))
        assertNull(ShareDeckReader.read("{\"skin\":\"GIFT\"}", mapper))
    }

    @Test
    fun `덱 안의 모르는 종류 카드는 그 카드만 건너뛰고 나머지를 그린다`() {
        val deck = read(V1_JSON.replace("\"kind\":\"LETTER\"", "\"kind\":\"SOMETHING_NEW\""))

        assertTrue(render(deck).contains("공유 당시 리캡 카드 <strong>5장</strong>"))
    }

    private fun read(json: String): ShareDeck = assertNotNull(ShareDeckReader.read(json, mapper))

    private fun render(deck: ShareDeck): String = renderer.renderDeck(CARD, deck, PUBLIC_ORIGIN)

    companion object {
        private const val PUBLIC_ORIGIN = "https://santacut.example"
        private val CARD = ShareCard(
            "T".repeat(43), "산타도와줘", "73.73", "3위", "선물 배송 출발", "2026년 12월 25일", "한마디",
            "2026년 12월 1일 10:02", "미리보기", "2026년 12월 2일 12:30",
        )

        /** 공유 브랜치에서 저장하던 1판 JSON 꼴(합성 예시). */
        private val V1_JSON = """
            {"version":1,"skin":"GIFT","cards":[
            {"index":0,"kind":"VERDICT","scoreText":"73.73","title":"단톡방 자판기","verdict":"판결 한 줄","stamp":"겨우 실었다","grade":"굴뚝에 걸쳐서 통과","gradeCode":"SCRAPED_THROUGH","cut":65,"cutDistance":"+8.73","passed":true,"rank":3,"rankingPending":false},
            {"index":1,"kind":"ROAST","scoreText":"73.73","chipLabel":"ㅋ 최대","chipValue":"12개","roast":"ㅋ 폭격기","flip":"근데 다정함"},
            {"index":2,"kind":"LETTER","scoreText":"73.73","letter":"올해 고마웠어"},
            {"index":3,"kind":"STORY","scoreText":"73.73","acts":[{"act":1,"months":"1~4월","line":"봄의 시작"}]},
            {"index":4,"kind":"ARCHETYPE","scoreText":"73.73","archetypeCode":"WORKSHOP_ELF","archetypeName":"약속 요정"},
            {"index":5,"kind":"DIMENSIONS","scoreText":"73.73","bars":[{"label":"공감","score":71.67}]}
            ]}
        """.trimIndent()

        /** 2판 JSON(합성 예시). */
        private val V2_JSON = """
            {"version":2,"skin":"GIFT","cards":[
            {"index":0,"kind":"VERDICT","scoreText":"73.73","rank":3,"topPercent":50,"verdict":"착한 아이","passed":true,"grade":"굴뚝에 걸쳐서 통과","cutDistance":"+8.73","verdictLine":"판정 한 줄."},
            {"index":1,"kind":"REASON","scoreText":"73.73","rank":3,"topPercent":50,"ordinal":1,"quote":"내일 몇 시에 볼까","chipText":"'ㄱ?' 발사 횟수 19번","label":"약속발사상","line":"판은 늘 여기서 열렸다.","dimTag":"협력 +"},
            {"index":2,"kind":"CONCLUSION","scoreText":"73.73","rank":3,"topPercent":50,"verdict":"착한 아이","passed":true,"grade":"굴뚝에 걸쳐서 통과","title":"약속 발사대","bandTag":"걸쳐서통과","finalLine":"마지막 한 줄.","friendCall":"우리 방에서 나보다 착한 애 나와"}
            ]}
        """.trimIndent()
    }
}
