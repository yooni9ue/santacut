package com.santacut.infrastructure.global.config

import com.santacut.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import kotlin.test.assertEquals

/**
 * 테스트 컨텍스트에서는 예약 폴링이 꺼져 있다. 분석 작업자가 컨텍스트가 뜨자마자(fixedDelay 첫 실행은 지연이 없다)
 * 공유 `build/test.db`의 남은 작업을 잡아 다른 테스트와 경쟁하던 간헐 실패를 막는다. 운영 설정은 바꾸지 않는다.
 */
@IntegrationTest
class TestSchedulingDisabledTest @Autowired constructor(
    private val applicationContext: ApplicationContext,
) {
    @Test
    fun `통합 테스트 컨텍스트에는 예약 폴링 설정이 없다`() {
        assertEquals(0, applicationContext.getBeanNamesForType(SchedulerConfiguration::class.java).size)
    }
}
