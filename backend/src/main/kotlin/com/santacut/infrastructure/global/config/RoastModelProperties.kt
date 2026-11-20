package com.santacut.infrastructure.global.config

import org.springframework.ai.google.genai.common.GoogleGenAiThinkingLevel
import org.springframework.boot.context.properties.ConfigurationProperties
import java.math.BigDecimal
import java.time.Duration

/**
 * 「팩폭 산타」 작가 호출에 쓰는 Gemini 모델 설정. 채점 모델 설정([ScoreModelProperties])과 따로 둔다.
 *
 * @property model 작가 모델 ID. 비어 있으면 작가를 부르지 않고 서버 대체 문장만 쓴다.
 * @property temperature 생성 온도. 같은 사람에게도 매번 다른 문장이 나오게 높게 둔다.
 * @property maxOutputTokens 호출 한 번의 출력 토큰 상한. 생각 토큰도 이 한도를 함께 쓰므로 넉넉히 둔다(잘림 방지).
 * @property thinkingLevel 생각 수준(MINIMAL, LOW, MEDIUM, HIGH). 비면 모델 기본값이다.
 * @property callTimeout 호출 하나의 시간 상한. 실제 HTTP 상한은 채점과 같은 클라이언트 설정이고, 작업 리스는 둘 중 긴 값으로 잡는다.
 * @property maxRewrites 검증에 떨어진 필드를 다시 쓰게 하는 최대 횟수. 호출은 많아야 1 + 이 값이다.
 * @property inputUsdPerMillionTokens 작가 모델 입력 토큰 100만 개 단가(달러). 비용 가드가 작가 호출을 채점과 따로 이 단가로 센다.
 * @property cachedInputUsdPerMillionTokens 작가 모델의 암묵적 캐시 읽기 입력 토큰 100만 개 단가(달러)
 * @property outputUsdPerMillionTokens 작가 모델 출력(답 본문) 토큰 100만 개 단가(달러)
 * @property thinkingUsdPerMillionTokens 작가 모델 생각 토큰 100만 개 단가(달러). Gemini는 생각 토큰을 출력 단가로 청구한다.
 */
@ConfigurationProperties(prefix = "santacut.ai.roast")
data class RoastModelProperties(
    val model: String = DEFAULT_MODEL,
    val temperature: Double = DEFAULT_TEMPERATURE,
    val maxOutputTokens: Int = DEFAULT_MAX_OUTPUT_TOKENS,
    val thinkingLevel: String = DEFAULT_THINKING_LEVEL,
    val callTimeout: Duration = DEFAULT_CALL_TIMEOUT,
    val maxRewrites: Int = DEFAULT_MAX_REWRITES,
    val inputUsdPerMillionTokens: BigDecimal = DEFAULT_INPUT_USD,
    val cachedInputUsdPerMillionTokens: BigDecimal = DEFAULT_CACHED_INPUT_USD,
    val outputUsdPerMillionTokens: BigDecimal = DEFAULT_OUTPUT_USD,
    val thinkingUsdPerMillionTokens: BigDecimal = DEFAULT_OUTPUT_USD,
) {
    init {
        require(maxRewrites >= 0) { "santacut.ai.roast.max-rewrites는 0 이상이어야 한다: $maxRewrites" }
        require(thinkingLevel.isBlank() || thinkingLevel in THINKING_LEVELS) {
            "santacut.ai.roast.thinking-level은 비우거나 $THINKING_LEVELS 중 하나여야 한다: $thinkingLevel"
        }
    }

    /** 작업 하나가 작가를 부르는 최대 횟수. */
    fun maxCalls(): Int = 1 + maxRewrites

    companion object {
        private const val DEFAULT_MODEL = "gemini-3.5-flash"
        private const val DEFAULT_TEMPERATURE = 1.0
        private const val DEFAULT_MAX_OUTPUT_TOKENS = 8192
        private const val DEFAULT_THINKING_LEVEL = "LOW"
        private const val DEFAULT_MAX_REWRITES = 2
        private val DEFAULT_CALL_TIMEOUT: Duration = Duration.ofSeconds(60)

        /** Gemini 3.5 Flash 표준 등급 정가(100만 토큰당 입력 1.5달러, 캐시 읽기 0.15달러, 출력·생각 9달러). 2026-11-20 확인. */
        private val DEFAULT_INPUT_USD = BigDecimal("1.5")
        private val DEFAULT_CACHED_INPUT_USD = BigDecimal("0.15")
        private val DEFAULT_OUTPUT_USD = BigDecimal("9")

        /** 클라이언트가 받는 생각 수준 이름. 앱이 뜰 때 틀린 값을 막아 호출마다 예외로 대체 문장이 되지 않게 한다. */
        private val THINKING_LEVELS: Set<String> = GoogleGenAiThinkingLevel.entries.map(GoogleGenAiThinkingLevel::name).toSet()
    }
}
