package com.santacut

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["spring.datasource.url=jdbc:sqlite:./build/test.db"])
class SantaCutApplicationTests {

	@Test
	fun contextLoads() {
	}

}
