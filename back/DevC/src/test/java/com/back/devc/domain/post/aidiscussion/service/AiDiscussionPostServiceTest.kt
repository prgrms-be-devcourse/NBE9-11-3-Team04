package com.back.devc.domain.post.aidiscussion.service

import com.back.devc.domain.post.aidiscussion.dto.AiDiscussionGenerateResponse
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.aidiscussion.entity.AiDiscussionPost
import com.back.devc.domain.post.aidiscussion.repository.AiDiscussionPostRepository
import com.back.devc.domain.post.aidiscussion.type.AiDiscussionStatus
import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.dto.PostCreateResponse
import com.back.devc.domain.post.post.service.PostService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
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
        aiDiscussionPostRepository = mock(AiDiscussionPostRepository::class.java)
        postService = mock(PostService::class.java)
        aiDiscussionGeneratorService = mock(AiDiscussionGeneratorService::class.java)
        aiDiscussionPersistenceService = AiDiscussionPersistenceService(aiDiscussionPostRepository)
        categoryRepository = mock(CategoryRepository::class.java)
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
        `when`(aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING))
            .thenReturn(false)
        `when`(aiDiscussionPostRepository.save(any(AiDiscussionPost::class.java)))
            .thenAnswer { invocation ->
                val aiDiscussionPost = invocation.getArgument<AiDiscussionPost>(0)
                setId(aiDiscussionPost, 1L)
                aiDiscussionPost
            }
        `when`(aiDiscussionGeneratorService.generateDailyTopic())
            .thenReturn(
                AiDiscussionGenerateResponse(
                    title = "AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?",
                    content = "AI 토론 주제 테스트 본문입니다.",
                ),
            )

        val response = aiDiscussionPostService.createPendingDiscussion()

        assertThat(response.id).isEqualTo(1L)
        assertThat(response.status).isEqualTo(AiDiscussionStatus.PENDING)
        assertThat(response.approvedPostId).isNull()
        assertThat(response.rejectionReason).isNull()
        assertThat(response.title).isEqualTo("AI 시대에 주니어 개발자는 어떤 역량을 길러야 할까?")
        assertThat(response.content).isEqualTo("AI 토론 주제 테스트 본문입니다.")
        verify(aiDiscussionGeneratorService).generateDailyTopic()
        verify(aiDiscussionPostRepository, times(2)).existsByStatus(AiDiscussionStatus.PENDING)
        verify(aiDiscussionPostRepository).save(any(AiDiscussionPost::class.java))
    }

    @Test
    fun createPendingDiscussion_fail_whenPendingAlreadyExists() {
        `when`(aiDiscussionPostRepository.existsByStatus(AiDiscussionStatus.PENDING))
            .thenReturn(true)

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.createPendingDiscussion()
        }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        verify(aiDiscussionPostRepository).existsByStatus(AiDiscussionStatus.PENDING)
        verify(aiDiscussionPostRepository, never()).save(any(AiDiscussionPost::class.java))
        verifyNoInteractions(aiDiscussionGeneratorService)
    }

    @Test
    fun getPendingDiscussions_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        `when`(
            aiDiscussionPostRepository.findAllByStatusOrderByCreatedAtDesc(
                AiDiscussionStatus.PENDING,
            ),
        ).thenReturn(listOf(aiDiscussionPost))

        val responses = aiDiscussionPostService.getPendingDiscussions()

        assertThat(responses).hasSize(1)
        assertThat(responses[0].id).isEqualTo(1L)
        assertThat(responses[0].status).isEqualTo(AiDiscussionStatus.PENDING)
        verify(aiDiscussionPostRepository).findAllByStatusOrderByCreatedAtDesc(
            AiDiscussionStatus.PENDING,
        )
    }

    @Test
    fun getDiscussion_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val response = aiDiscussionPostService.getDiscussion(1L)

        assertThat(response.id).isEqualTo(1L)
        assertThat(response.status).isEqualTo(AiDiscussionStatus.PENDING)
        verify(aiDiscussionPostRepository).findById(1L)
    }

    @Test
    fun getDiscussion_fail_whenNotFound() {
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.empty())

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.getDiscussion(1L)
        }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(aiDiscussionPostRepository).findById(1L)
    }

    @Test
    fun approveDiscussion_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))
        val discussionCategory = Category("discussion")
        setCategoryId(discussionCategory, 3L)
        `when`(categoryRepository.findByName("discussion"))
            .thenReturn(discussionCategory)

        val expectedRequest = PostCreateRequest(
            title = aiDiscussionPost.title,
            content = aiDiscussionPost.content,
            categoryId = 3L,
        )
        `when`(postService.write(2L, expectedRequest))
            .thenReturn(PostCreateResponse(10L))

        val response = aiDiscussionPostService.approveDiscussion(
            aiDiscussionPostId = 1L,
            adminUserId = 2L,
        )

        assertThat(response.id).isEqualTo(1L)
        assertThat(response.status).isEqualTo(AiDiscussionStatus.APPROVED)
        assertThat(response.approvedPostId).isEqualTo(10L)
        assertThat(response.rejectionReason).isNull()

        verify(postService).write(2L, expectedRequest)
        verify(categoryRepository).findByName("discussion")
    }

    @Test
    fun approveDiscussion_fail_whenDiscussionCategoryNotFound() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))
        `when`(categoryRepository.findByName("discussion"))
            .thenReturn(null)

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.approveDiscussion(
                aiDiscussionPostId = 1L,
                adminUserId = 2L,
            )
        }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(categoryRepository).findByName("discussion")
        verifyNoInteractions(postService)
    }

    @Test
    fun approveDiscussion_fail_whenNotPending() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        aiDiscussionPost.reject("테스트 거절")
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.approveDiscussion(
                aiDiscussionPostId = 1L,
                adminUserId = 2L,
            )
        }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        verifyNoInteractions(postService)
    }

    @Test
    fun rejectDiscussion_success() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val response = aiDiscussionPostService.rejectDiscussion(
            aiDiscussionPostId = 1L,
            reason = "주제가 부적절합니다.",
        )

        assertThat(response.id).isEqualTo(1L)
        assertThat(response.status).isEqualTo(AiDiscussionStatus.REJECTED)
        assertThat(response.rejectionReason).isEqualTo("주제가 부적절합니다.")
        assertThat(response.approvedPostId).isNull()
        verify(aiDiscussionPostRepository).findById(1L)
    }

    @Test
    fun rejectDiscussion_fail_whenNotPending() {
        val aiDiscussionPost = createAiDiscussionPost(id = 1L)
        aiDiscussionPost.approve(10L)
        `when`(aiDiscussionPostRepository.findById(1L))
            .thenReturn(Optional.of(aiDiscussionPost))

        val exception = assertThrows<ResponseStatusException> {
            aiDiscussionPostService.rejectDiscussion(
                aiDiscussionPostId = 1L,
                reason = "주제가 부적절합니다.",
            )
        }

        assertThat(exception.statusCode).isEqualTo(HttpStatus.CONFLICT)
        verify(aiDiscussionPostRepository).findById(1L)
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