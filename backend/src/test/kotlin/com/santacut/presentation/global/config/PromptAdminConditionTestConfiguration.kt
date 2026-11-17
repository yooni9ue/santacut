package com.santacut.presentation.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional

/** [PromptAdminCondition]이 맞을 때만 빈 하나를 올리는 시험용 설정. 통합 테스트 컴포넌트 스캔에 걸리지 않게 @Configuration을 달지 않는다. */
class PromptAdminConditionTestConfiguration {
    @Bean(BEAN_NAME)
    @Conditional(PromptAdminCondition::class)
    fun promptAdminMarker(): String = BEAN_NAME

    companion object {
        const val BEAN_NAME = "promptAdminMarker"
    }
}
