package com.santacut.domain.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.time.Duration

/**
 * 앱 안의 AI 비용 가드 설정. 기록한 토큰 수에 단가를 곱해 하루·한 달 비용을 어림하고, 한도를 넘으면 작업을 잡지 않는다.
 *
 * @property dailyUsd 하루(한국 날짜) 비용 상한(달러). null이나 0이면 하루 한도를 두지 않는다(기본). Gemini 선불 크레딧이 실제 상한이다.
 * @property monthlyUsd 한 달(한국 달력) 비용 상한(달러)
 * @property inputUsdPerMillionTokens 입력 토큰 100만 개 단가(달러)
 * @property cachedInputUsdPerMillionTokens 암묵적 캐시에서 읽은 입력 토큰 100만 개 단가(달러). 3.5 Flash-Lite 공식 단가는 정가 입력의 10%다(0.03달러).
 * @property outputUsdPerMillionTokens 출력 토큰 100만 개 단가(달러)
 * @property pause 제공자가 호출을 막았거나 연속으로 실패했을 때 작업 잡기를 멈추는 시간
 * @property maxConsecutiveFailures 이만큼 연달아 실패하면 [pause]만큼 멈춘다
 */
@ConfigurationProperties(prefix = "santacut.ai.budget")
data class AiBudgetProperties(
    val dailyUsd: BigDecimal? = null,
    val monthlyUsd: BigDecimal = BigDecimal("9"),
    val inputUsdPerMillionTokens: BigDecimal = BigDecimal("0.3"),
    val cachedInputUsdPerMillionTokens: BigDecimal = BigDecimal("0.03"),
    val outputUsdPerMillionTokens: BigDecimal = BigDecimal("2.5"),
    val pause: Duration = Duration.ofMinutes(15),
    val maxConsecutiveFailures: Int = 3,
)
