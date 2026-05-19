package com.back.devc.domain.post.aidiscussion.unit

import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionGenerateResponse
import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.repository.AiDiscussionPostRepository
import com.back.devc.domain.post.aidiscussion.service.AiDiscussionGeneratorService
import com.back.devc.domain.post.aidiscussion.service.AiDiscussionPersistenceService
import com.back.devc.domain.post.aidiscussion.service.AiDiscussionPostService
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.dto.PostCreateResponse
import com.back.devc.domain.post.post.service.PostService
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import java.util.Optional

class AiDiscussionPostServiceTest {

    private lateinit var aiDiscussionPostRepository: AiDiscussionPostRepository
    private lateinit var postService: PostService
    private lateinit var aiDiscussionPostService: AiDiscussionPostService
    private lateinit var aiDiscussionGeneratorService: AiDiscussionGeneratorService
    private lateinit var aiDiscussionPersistenceService: AiDiscussionPersistenceService
    private lateinit var categoryRepository: CategoryRepository

    @BeforeEach
    fun setUp() {
        aiDiscussionPostRepository = Mockito.mock(AiDiscussionPostRepository::class.java)
        postService = Mockito.mock(PostService::class.java)
        aiDiscussionGeneratorService = Mockito.mock(AiDiscussionGeneratorService::class.java)
        aiDiscussionPersistenceService = AiDiscussionPersistenceService(aiDiscussionPostRepository)
        categoryRepository = Mockito.mock(CategoryRepository::class.java)
        aiDiscussionPostService = AiDiscussionPostService(
            aiDiscussionPostRepository = aiDiscussionPostRepository,
            postService = postService,
            aiDiscussionGeneratorService = aiDiscussionGeneratorService,
            aiDiscussionPersistenceService = aiDiscussionPersistenceService,
            categoryRepository = categoryRepository,
        )
    }

    @Test
    fun createPendingDiscussion_success() {
        Mockito.`when`(aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING))
            .thenReturn(false)
        Mockito.`when`(aiDiscussionPostRepository.save(ArgumentMatchers.any(AiDiscussionPost::class.java)))
            .thenAnswer { invocation ->
                val aiDiscussionPost = invocation.getArgument<AiDiscussionPost>(0)
                setId(aiDiscussionPost, 1L)
                aiDiscussionPost
            }
        Mockito.`when`(aiDiscussionGeneratorService.generateDailyTopic())
            .thenReturn(
                AiDiscussionGenerateResponse(
                    title = "AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?",
                    content = "AI 토론 주제 테스트 본문입니다.",
                ),
            )

        val response = aiDiscussionPostService.createPendingDiscussion()

        Assertions.assertThat(response.id).isEqualTo(1L)
        Assertions.assertThat(response.status).isEqualTo(AiDiscussionStatus.PENDING)
        Assertions.assertThat(response.approvedPostId).isNull()
        Assertions.assertThat(response.rejectionReason).isNull()
        Assertions.assertThat(response.title).isEqualTo("AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?")
        Assertions.assertThat(response.content).isEqualTo("AI 토론 주제 테스트 본문입니다.")
        Mockito.verify(aiDiscussionGeneratorService).generateDailyTopic()
        Mockito.verify(aiDiscussionPostRepository, Mockito.times(2)).existsByStatus(AiDiscussionStatus.PENDING)
        Mockito.verify(aiDiscussionPostRepository).save(ArgumentMatchers.any(AiDiscussionPost::class.java))
    }

    @Test
    fun createPendingDiscussion_fail_whenPendingAlreadyExists() {
        Mockito.`when`(aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING))
            .thenReturn(true)

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.createPendingDiscussion()
        }

        Assertions.assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        Mockito.verify(aiDiscussionPostRepository).existsByStatus(AiDiscussionStatus.PENDING)
        Mockito.verify(aiDiscussionPostRepository, Mockito.never())
            .save(ArgumentMatchers.any(AiDiscussionPost::class.java))
        Mockito.verifyNoInteractions(aiDiscussionGeneratorService)
    }

    @Test
    fun getPendingDiscussions_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        Mockito.`when`(
            aiDiscussionPostRepository.findAllByStatusOrderByCreatedAtDesc(
                AiDiscussionStatus.PENDING,
            ),
        ).thenReturn(listOf(aiDiscussionPost))

        val responses = aiDiscussionPostService.getPendingDiscussions()

        Assertions.assertThat(responses).hasSize(1)
        Assertions.assertThat(responses[0].id).isEqualTo(1L)
        Assertions.assertThat(responses[0].status).isEqualTo(AiDiscussionStatus.PENDING)
        Mockito.verify(aiDiscussionPostRepository).findAllByStatusOrderByCreatedAtDesc(
            AiDiscussionStatus.PENDING,
        )
    }

    @Test
    fun getDiscussion_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val response = aiDiscussionPostService.getDiscussion(1L)

        Assertions.assertThat(response.id).isEqualTo(1L)
        Assertions.assertThat(response.status).isEqualTo(AiDiscussionStatus.PENDING)
        Mockito.verify(aiDiscussionPostRepository).findById(1L)
    }

    @Test
    fun getDiscussion_fail_whenNotFound() {
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.empty())

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.getDiscussion(1L)
        }

        Assertions.assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        Mockito.verify(aiDiscussionPostRepository).findById(1L)
    }

    @Test
    fun approveDiscussion_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))
        val discussionCategory = Category("discussion")
        setCategoryId(discussionCategory, 3L)
        Mockito.`when`(categoryRepository.findByName("discussion"))
            .thenReturn(discussionCategory)

        val expectedRequest = PostCreateRequest(
            title = aiDiscussionPost.title,
            content = aiDiscussionPost.content,
            categoryId = 3L,
        )
        Mockito.`when`(postService.write(2L, expectedRequest))
            .thenReturn(PostCreateResponse(10L))

        val response = aiDiscussionPostService.approveDiscussion(
            aiDiscussionPostId = 1L,
            adminUserId = 2L,
        )

        Assertions.assertThat(response.id).isEqualTo(1L)
        Assertions.assertThat(response.status).isEqualTo(AiDiscussionStatus.APPROVED)
        Assertions.assertThat(response.approvedPostId).isEqualTo(10L)
        Assertions.assertThat(response.rejectionReason).isNull()

        Mockito.verify(postService).write(2L, expectedRequest)
        Mockito.verify(categoryRepository).findByName("discussion")
    }

    @Test
    fun approveDiscussion_fail_whenDiscussionCategoryNotFound() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))
        Mockito.`when`(categoryRepository.findByName("discussion"))
            .thenReturn(null)

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.approveDiscussion(
                aiDiscussionPostId = 1L,
                adminUserId = 2L,
            )
        }

        Assertions.assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        Mockito.verify(categoryRepository).findByName("discussion")
        Mockito.verifyNoInteractions(postService)
    }

    @Test
    fun approveDiscussion_fail_whenNotPending() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        aiDiscussionPost.reject("테스트 거절")
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.approveDiscussion(
                aiDiscussionPostId = 1L,
                adminUserId = 2L,
            )
        }

        Assertions.assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        Mockito.verifyNoInteractions(postService)
    }

    @Test
    fun rejectDiscussion_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val response = aiDiscussionPostService.rejectDiscussion(
            aiDiscussionPostId = 1L,
            reason = "주제가 부적절합니다.",
        )

        Assertions.assertThat(response.id).isEqualTo(1L)
        Assertions.assertThat(response.status).isEqualTo(AiDiscussionStatus.REJECTED)
        Assertions.assertThat(response.rejectionReason).isEqualTo("주제가 부적절합니다.")
        Assertions.assertThat(response.approvedPostId).isNull()
        Mockito.verify(aiDiscussionPostRepository).findById(1L)
    }

    @Test
    fun rejectDiscussion_fail_whenNotPending() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        aiDiscussionPost.approve(10L)
        Mockito.`when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.rejectDiscussion(
                aiDiscussionPostId = 1L,
                reason = "주제가 부적절합니다.",
            )
        }

        Assertions.assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        Mockito.verify(aiDiscussionPostRepository).findById(1L)
    }

    private fun createAiDiscussionPost(id: Long): AiDiscussionPost {
        val aiDiscussionPost = AiDiscussionPost.create(
            title = "AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?",
            content = "AI 토론 주제 테스트 본문입니다.",
        )
        setId(aiDiscussionPost, id)
        return aiDiscussionPost
    }

    private fun setId(
        aiDiscussionPost: AiDiscussionPost,
        id: Long,
    ) {
        val field = AiDiscussionPost::class.java.getDeclaredField("id")
        field.isAccessible = true
        field.set(aiDiscussionPost, id)
    }

    private fun setCategoryId(
        category: Category,
        id: Long,
    ) {
        val field = Category::class.java.getDeclaredField("categoryId")
        field.isAccessible = true
        field.set(category, id)
    }
}