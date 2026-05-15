package com.back.devc

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
object DevCApplication {
    @JvmStatic
    fun main(args: Array<String>) {
        SpringApplication.run(DevCApplication::class.java, *args)
    }
}
