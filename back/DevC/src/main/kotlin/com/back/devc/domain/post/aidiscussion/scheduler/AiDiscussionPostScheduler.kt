package com.back.devc.domain.post.aidiscussion.scheduler

import com.back.devc.domain.post.aidiscussion.service.AiDiscussionPostService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
@ConditionalOnProperty(
    prefix = "ai.discussion.scheduler",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true,
)
class AiDiscussionPostScheduler(
    private val aiDiscussionPostService: AiDiscussionPostService,
) {

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun createDailyDiscussionPost() {
        runCatching {
            aiDiscussionPostService.createPendingDiscussion()
            log.info("AI 토론 주제 자동 생성 완료")
        }.onFailure { exception ->
            if (exception is ResponseStatusException) {
                log.info(
                    "AI 토론 주제 자동 생성 생략 - reason={}",
                    exception.reason,
                )
                return
            }

            log.error("AI 토론 주제 자동 생성 실패", exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AiDiscussionPostScheduler::class.java)
    }
}