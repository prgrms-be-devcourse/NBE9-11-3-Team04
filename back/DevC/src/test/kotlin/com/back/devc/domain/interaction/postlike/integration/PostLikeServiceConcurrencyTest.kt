package com.back.devc.domain.interaction.postlike.integration

import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.interaction.postLike.service.PostLikeService
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostLikeServiceConcurrencyTest @Autowired constructor(
    private val postLikeService: PostLikeService,
    private val postLikeRepository: PostLikeRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val categoryRepository: CategoryRepository,
) {

    @Test
    @DisplayName("동시에 좋아요를 여러 번 요청해도 좋아요는 1개만 생성되고 likeCount는 1만 증가한다")
    fun createLikeConcurrently() {
        // given
        val member = memberRepository.save(
            createMember(
                email = "like@test.com",
                nickname = "likeUser",
            )
        )

        val category = categoryRepository.save(
            createCategory("좋아요 테스트 카테고리")
        )

        val post = postRepository.save(
            createPost(
                member = member,
                category = category,
            )
        )

        val postId = post.postId
            ?: throw IllegalStateException("저장된 게시글 ID가 없습니다.")

        val command = PostLikeCommand(
            userId = member.userId
                ?: throw IllegalStateException("저장된 회원 ID가 없습니다."),
            postId = postId,
        )

        val threadCount = 100

        val executorService = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        // when
        repeat(threadCount) {
            executorService.submit {
                try {
                    readyLatch.countDown()
                    startLatch.await()

                    postLikeService.createLike(command)
                } catch (_: Exception) {
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        val ready = readyLatch.await(10, TimeUnit.SECONDS)
        assertThat(ready).isTrue()

        startLatch.countDown()

        val finished = doneLatch.await(10, TimeUnit.SECONDS)
        assertThat(finished).isTrue()

        executorService.shutdown()

        // then
        val likeRowCount = postLikeRepository.countByPost_PostId(postId)
        val likeCount = postRepository.findLikeCountByPostId(postId)

        assertThat(likeRowCount).isEqualTo(1)
        assertThat(likeCount).isEqualTo(1)
    }

    @Test
    @DisplayName("동시에 좋아요 취소를 여러 번 요청해도 좋아요는 0개가 되고 likeCount는 0 미만으로 내려가지 않는다")
    fun cancelLikeConcurrently() {
        // given
        val member = memberRepository.save(
            createMember(
                email = "cancel-like@test.com",
                nickname = "cancelLikeUser",
            )
        )

        val category = categoryRepository.save(
            createCategory("좋아요 취소 테스트 카테고리")
        )

        val post = postRepository.save(
            createPost(
                member = member,
                category = category,
            )
        )

        val command = PostLikeCommand(
            userId = member.userId
                ?: throw IllegalStateException("저장된 회원 ID가 없습니다."),
            postId = post.postId
                ?: throw IllegalStateException("저장된 게시글 ID가 없습니다."),
        )

        postLikeService.createLike(command)

        val threadCount = 100

        val executorService = Executors.newFixedThreadPool(threadCount)
        val readyLatch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)

        // when
        repeat(threadCount) {
            executorService.submit {
                try {
                    readyLatch.countDown()
                    startLatch.await()

                    postLikeService.cancelLike(command)
                } catch (_: Exception) {
                } finally {
                    doneLatch.countDown()
                }
            }
        }

        val ready = readyLatch.await(10, TimeUnit.SECONDS)
        assertThat(ready).isTrue()

        startLatch.countDown()

        val finished = doneLatch.await(10, TimeUnit.SECONDS)
        assertThat(finished).isTrue()

        executorService.shutdown()

        // then
        val likeRowCount = postLikeRepository.countByPost_PostId(command.postId)
        val likeCount = postRepository.findLikeCountByPostId(command.postId)

        assertThat(likeRowCount).isEqualTo(0)
        assertThat(likeCount).isEqualTo(0)
    }

    private fun createMember(
        email: String,
        nickname: String,
    ): Member {
        return Member.createLocalMember(
            email,
            "passwordHash",
            nickname,
        )
    }

    private fun createCategory(name: String): Category {
        return Category(name)
    }

    private fun createPost(
        member: Member,
        category: Category,
    ): Post {
        return Post.create(
            member,
            category,
            "테스트 제목",
            "테스트 내용",
        )
    }
}