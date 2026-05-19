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
    private val restClient: RestClient,
) {

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
        val discussionContent = normalizeDiscussionContent(
            jsonNode.path("content").asText(),
        )

        if (title.isBlank() || discussionContent.isBlank()) {
            throw IllegalStateException("AI 응답에 title 또는 content가 없습니다.")
        }

        return AiDiscussionGenerateResponse(
            title = title,
            content = discussionContent,
        )
    }

    private fun normalizeDiscussionContent(content: String): String {
        return content
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace(Regex("\\s*토론\\s*포인트\\s*[:.]?\\s*"), "\n\n토론 포인트\n")
            .replace(Regex("\\s+(?=[1-3]\\.\\s)"), "\n")
            .lines()
            .map { line -> line.trim() }
            .filter { line -> line.isNotBlank() }
            .joinToString("\n")
            .replace(Regex("(토론 포인트)\\n"), "$1\n")
            .replace(Regex("(?<!\\n)토론 포인트"), "\n\n토론 포인트")
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    private fun createFallbackTopic(): AiDiscussionGenerateResponse {
        return AiDiscussionGenerateResponse(
            title = "AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?",
            content = """
                최근 AI 도구가 코드 작성, 리팩토링, 테스트 코드 작성까지 도와주는 환경이 빠르게 확산되고 있습니다.
            
                이런 변화 속에서 주니어 개발자는 단순 구현 능력만으로 충분한지, 아니면 문제 정의 능력과 코드 리뷰 역량, 설계 이해력을 더 강화해야 하는지에 대한 고민이 커지고 있습니다.
            
                여러분은 AI 시대에 주니어 개발자가 가장 집중해야 할 역량이 무엇이라고 생각하시나요?
            
                토론 포인트
                1. AI 도구를 잘 쓰는 능력도 개발 역량이라고 볼 수 있을까요?
                2. 기초 CS 지식의 중요성은 줄어들까요, 더 커질까요?
                3. 기업은 앞으로 주니어 개발자에게 무엇을 기대하게 될까요?
            """.trimIndent(),
        )
    }

    private fun createDailyTopicPrompt(): String {
        return """
        너는 개발자 커뮤니티 DevC의 토론 주제 작성자야.
        오늘 개발자들이 댓글로 의견을 나누고 싶어지는 흥미로운 개발 관련 토론 주제 1개를 생성해줘.

        작성 방식:
        - 글은 가능하면 일상에서 접할 수 있는 사례로 시작한다.
        - 사례는 영화, 드라마, 뉴스, IT 서비스, 앱, SNS 이슈, 회사 생활, 취업 준비, 팀 프로젝트, 학교 생활 중 하나를 활용한다.
        - 단, 실제 최신 영화/뉴스 제목을 확신할 수 없으면 특정 제목을 지어내지 말고 "최근 영화나 드라마에서"처럼 일반적으로 표현한다.
        - 도입부는 독자가 "나도 본 적 있다", "나도 겪어봤다"라고 느낄 수 있게 작성한다.
        - 도입부 다음에는 해당 사례를 개발, AI, 백엔드, 프론트엔드, 보안, 데이터, 협업, 테스트, 코드 품질 중 하나의 주제로 자연스럽게 연결한다.
        - 마지막에는 독자의 의견을 묻는 질문을 작성한다.
        - 그 다음 줄에 반드시 "토론 포인트"라는 소제목을 작성한다.
        - 토론 포인트는 반드시 1., 2., 3. 번호 목록으로 각각 다른 줄에 작성한다.
        - 문단 사이에는 빈 줄을 1줄 넣는다.
        - 제목은 50자 이하로 작성한다.
        - 본문은 700자 이하로 작성한다.
        - 정치, 종교, 혐오, 선정적 내용은 제외한다.
        - 특정 집단을 비난하거나 공격적인 표현은 사용하지 않는다.
        - 사실을 단정하기보다 의견을 묻는 방식으로 작성한다.
        - 응답은 title과 content를 포함한 JSON 형식으로만 작성한다.
        - content 값 안에는 줄바꿈을 \n으로 포함한다.
        - content에는 반드시 최소 5개의 줄바꿈이 포함되어야 한다.

        좋은 주제 예시:
        - 영화 속 AI 판결 시스템을 보며 생각해보는 개발자의 책임
        - 배달 앱 추천 알고리즘은 편리함일까, 조작일까?
        - 회사 메신저의 읽음 표시 기능은 협업에 도움이 될까?
        - AI 면접관이 지원자를 평가한다면 공정할까?
        - 유튜브 추천 알고리즘처럼 개발자 커뮤니티도 맞춤형 피드를 제공해야 할까?
        - 개발자가 만든 서비스의 알림은 어디까지 사용자를 방해해도 괜찮을까?

        응답 형식:
        {
          "title": "토론 제목",
          "content": "일상 사례 도입...\\n\\n개발 주제 연결...\\n\\n의견 질문...\\n\\n토론 포인트\\n1. ...\\n2. ...\\n3. ..."
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