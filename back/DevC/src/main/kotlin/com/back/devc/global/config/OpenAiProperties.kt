package com.back.devc.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@ConfigurationProperties(prefix = "ai.openai")
data class OpenAiProperties(
    val apiKey: String = "",
    val model: String = "gpt-4o-mini",
    val baseUrl: String = "https://api.openai.com/v1/chat/completions",
)

@Configuration
class OpenAiRestClientConfig {

    @Bean
    fun openAiRestClient(): RestClient {
        return RestClient.create()
    }
}