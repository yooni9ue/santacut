package com.santacut

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SantaCutApplication

fun main(args: Array<String>) {
	runApplication<SantaCutApplication>(*args)
}
