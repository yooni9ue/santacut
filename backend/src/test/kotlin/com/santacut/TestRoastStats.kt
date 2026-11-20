package com.santacut

import com.santacut.domain.dossier.model.BusiestDay
import com.santacut.domain.dossier.model.DossierStats
import com.santacut.domain.dossier.model.LaughRun
import com.santacut.domain.dossier.model.LaughStats
import com.santacut.domain.dossier.model.MessageStyle
import com.santacut.domain.dossier.model.PhraseTallies
import com.santacut.domain.dossier.model.PhraseTally
import com.santacut.domain.dossier.model.ReplyStats
import com.santacut.domain.dossier.model.VolumeStats

/**
 * 사실 고르기 테스트가 쓰는 합성 조서 집계. 실제 사전으로 센 작은 집계의 틀에 값만 바꿔 끼운다.
 */
object TestRoastStats {
    /** 한 해에 말풍선 약 2.5만 개, 활동 330일, 최대 연타 40, 물음표 25%, 심야 1%, 'ㄱㄱ'을 자주 쓰는 단톡방 고인물(운영자 같은 사람). */
    val heavyUser: DossierStats by lazy {
        val base = TestDossiers.tally(TestDossiers.selves(List(BASE_MESSAGES) { "ㄱㄱ 바로 가자?" }), TestRoasts.dossier).stats
        base.copy(
            volume = VolumeStats(25_000, 23_500, 230_000, 330, BusiestDay(400, 7)),
            laugh = LaughStats(20_000, 85.0, LaughRun(70, 8, 23), 500, 2.1),
            emoji = base.emoji.copy(density = 1.5),
            clock = base.clock.copy(
                hourly = HOURLY, weekday = WEEKDAY, peakHour = 22, nightRatio = 0.01, busiestMonth = 7, quietestMonth = 2, monthRatio = 3.0,
            ),
            reply = ReplyStats(false, null),
            style = MessageStyle(9.0, 0.5, 2.6, 40, 0.25, 0.03, 900),
            words = base.words.copy(phrasesFrequent = HEAVY_PHRASES, phrasesDistinct = PhraseTallies(emptyList())),
        )
    }

    private const val BASE_MESSAGES = 300

    /** 0~4시 합 250(1%), 22시가 가장 많다. 합 25,000. */
    private val HOURLY = listOf(
        120, 60, 35, 20, 15, 20, 120, 350, 700, 1000, 1100, 1200,
        1500, 1300, 1150, 1100, 1150, 1300, 1500, 1650, 1800, 2100, 3100, 2610,
    )

    /** 월~일. 주말 비율 약 24%(보통). 합 25,000. */
    private val WEEKDAY = listOf(3900, 3800, 3700, 3800, 3900, 3000, 2900)

    private val HEAVY_PHRASES = PhraseTallies(
        listOf(
            PhraseTally("gogo", "ㄱㄱ", "agree", 2400),
            PhraseTally("ok", "ㅇㅋ", "agree", 1200),
            PhraseTally("heol", "헐", "reaction", 900),
            PhraseTally("ro", "ㄹㅇ", "agree", 700),
            PhraseTally("giyeok_q", "ㄱ?", "question", 300),
        ),
    )
}
