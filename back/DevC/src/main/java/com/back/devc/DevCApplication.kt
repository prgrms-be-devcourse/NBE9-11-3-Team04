package com.back.devc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
open class DevCApplication

fun main(args: Array<String>) {
    runApplication<DevCApplication>(*args)
}
