package com.back.devc.domain.post.aidiscussion.service

import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionGenerateResponse
import com.back.devc.global.config.OpenAiProperties
import tools.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient

@Service
class AiDiscussionGeneratorService(
    private val openAiProperties: OpenAiProperties,
    private val objectMapper: ObjectMapper,
) {
    private val restClient: RestClient = RestClient.create()

    fun generateDailyTopic(): AiDiscussionGenerateResponse {
        val prompt = createDailyTopicPrompt()

        log.info(
            "AI 토론 주제 생성 요청 준비 - model={}, baseUrl={}, apiKeyConfigured={}, promptLength={}",
            openAiProperties.model,
            openAiProperties.baseUrl,
            openAiProperties.apiKey.isNotBlank(),
            prompt.length,
        )

        if (openAiProperties.apiKey.isBlank()) {
            log.warn("OPENAI_API_KEY가 설정되지 않아 기본 AI 토론 주제를 사용합니다.")
            return createFallbackTopic()
        }

        return runCatching {
            val response = restClient
                .post()
                .uri(openAiProperties.baseUrl)
                .headers { headers -> headers.setBearerAuth(openAiProperties.apiKey) }
                .body(createOpenAiRequest(prompt))
                .retrieve()
                .body(OpenAiChatCompletionResponse::class.java)

            val content = response
                ?.choices
                ?.firstOrNull()
                ?.message
                ?.content
                ?: throw IllegalStateException("AI 응답 본문이 비어 있습니다.")

            parseGeneratedTopic(content)
        }.getOrElse { exception ->
            log.error("AI 토론 주제 생성 실패 - 기본 주제를 사용합니다.", exception)
            createFallbackTopic()
        }
    }

    private fun createOpenAiRequest(prompt: String): Map<String, Any> {
        return mapOf(
            "model" to openAiProperties.model,
            "messages" to listOf(
                mapOf(
                    "role" to "system",
                    "content" to "너는 개발자 커뮤니티의 토론 주제 작성자야. 반드시 JSON 형식으로만 응답해.",
                ),
                mapOf(
                    "role" to "user",
                    "content" to prompt,
                ),
            ),
            "response_format" to mapOf("type" to "json_object"),
        )
    }

    private fun parseGeneratedTopic(content: String): AiDiscussionGenerateResponse {
        val jsonNode = objectMapper.readTree(content)
        val title = jsonNode.path("title").asText().trim()
        val discussionContent = jsonNode.path("content").asText().trim()

        if (title.isBlank() || discussionContent.isBlank()) {
            throw IllegalStateException("AI 응답에 title 또는 content가 없습니다.")
        }

        return AiDiscussionGenerateResponse(
            title = title,
            content = discussionContent,
        )
    }

    private fun createFallbackTopic(): AiDiscussionGenerateResponse {
        return AiDiscussionGenerateResponse(
            title = "AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?",
            content = """
                최근 AI 도구가 코드 작성, 리팩토링, 테스트 코드 작성까지 도와주는 환경이 빠르게 확산되고 있습니다.

                이런 상황에서 주니어 개발자는 단순 구현 능력보다 문제 정의 능력, 코드 리뷰 능력, 설계 이해력이 더 중요해질 수 있다는 의견이 있습니다.

                여러분은 AI 시대에 주니어 개발자가 가장 집중해야 할 역량이 무엇이라고 생각하시나요?

                토론 포인트
                1. AI 도구를 잘 쓰는 능력도 개발 역량이라고 볼 수 있을까?
                2. 기초 CS 지식의 중요성은 줄어들까, 더 커질까?
                3. 기업은 앞으로 주니어 개발자에게 무엇을 기대하게 될까?
            """.trimIndent(),
        )
    }

    private fun createDailyTopicPrompt(): String {
        return """
            너는 개발자 커뮤니티 DevC의 토론 주제 작성자야.
            오늘 개발자들이 댓글로 의견을 나눌 수 있는 흥미로운 개발 관련 토론 주제 1개를 생성해줘.

            조건:
            - 주제는 백엔드, 프론트엔드, AI, 취업, 협업, 코드 품질, 테스트, 성능 개선 중 하나와 관련 있어야 한다.
            - 제목은 50자 이하로 작성한다.
            - 본문은 500자 이하로 작성한다.
            - 본문 마지막에는 토론 포인트 3개를 포함한다.
            - 정치, 종교, 혐오, 선정적 내용은 제외한다.
            - 특정 집단을 비난하거나 공격적인 표현은 사용하지 않는다.
            - 사실을 단정하기보다 의견을 묻는 방식으로 작성한다.
            - 응답은 title과 content를 포함한 JSON 형식으로 작성한다.

            응답 형식:
            {
              "title": "토론 제목",
              "content": "토론 본문"
            }
        """.trimIndent()
    }

    private data class OpenAiChatCompletionResponse(
        val choices: List<OpenAiChoice> = emptyList(),
    )

    private data class OpenAiChoice(
        val message: OpenAiMessage? = null,
    )

    private data class OpenAiMessage(
        val content: String? = null,
    )

    companion object {
        private val log = LoggerFactory.getLogger(AiDiscussionGeneratorService::class.java)
    }
}