package com.back.devc.domain.post.aidiscussion.scheduler

import com.back.devc.domain.post.aidiscussion.service.AiDiscussionPostService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException

@Component
class AiDiscussionPostScheduler(
    private val aiDiscussionPostService: AiDiscussionPostService,
) {

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    fun createDailyDiscussionPost() {
        try {
            val aiDiscussionPost = aiDiscussionPostService.createPendingDiscussion()

            log.info(
                "AI 토론 주제 자동 생성 완료 - aiDiscussionPostId={}",
                aiDiscussionPost.id,
            )
        } catch (e: ResponseStatusException) {
            log.info(
                "AI 토론 주제 자동 생성 생략 - reason={}",
                e.reason,
            )
        } catch (e: Exception) {
            log.error(
                "AI 토론 주제 자동 생성 실패",
                e,
            )
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(AiDiscussionPostScheduler::class.java)
    }
}