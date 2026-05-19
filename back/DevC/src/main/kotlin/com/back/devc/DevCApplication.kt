package com.back.devc

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.boot.context.properties.ConfigurationPropertiesScan

@ConfigurationPropertiesScan
@EnableScheduling
@SpringBootApplication
@EnableJpaAuditing
class DevCApplication

fun main(args: Array<String>) {
    runApplication<DevCApplication>(*args)
}
