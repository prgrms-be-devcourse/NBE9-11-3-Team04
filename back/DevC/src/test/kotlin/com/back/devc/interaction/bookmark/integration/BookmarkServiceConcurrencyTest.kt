package com.back.devc.domain.interaction.bookmark.integration

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.bookmark.service.BookmarkService
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
class BookmarkServiceConcurrencyTest @Autowired constructor(
    private val bookmarkService: BookmarkService,
    private val bookmarkRepository: BookmarkRepository,
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val categoryRepository: CategoryRepository,
) {

    @Test
    @DisplayName("동시에 북마크를 여러 번 요청해도 북마크는 1개만 생성된다")
    fun createBookmarkConcurrently() {
        // given
        val member = memberRepository.save(
            createMember(
                email = "bookmark@test.com",
                nickname = "bookmarkUser",
            )
        )

        val category = categoryRepository.save(
            createCategory("북마크 테스트 카테고리")
        )

        val post = postRepository.save(
            createPost(
                member = member,
                category = category,
            )
        )

        val command = BookmarkCreateCommand(
            memberId = member.userId
                ?: throw IllegalStateException("저장된 회원 ID가 없습니다."),
            postId = post.postId
                ?: throw IllegalStateException("저장된 게시글 ID가 없습니다."),
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

                    bookmarkService.createBookmark(command)
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
        val bookmarkRowCount = bookmarkRepository.countByPost_PostId(
            post.postId ?: throw IllegalStateException("저장된 게시글 ID가 없습니다.")
        )

        assertThat(bookmarkRowCount).isEqualTo(1)
    }

    @Test
    @DisplayName("동시에 북마크 취소를 여러 번 요청해도 북마크는 0개가 된다")
    fun cancelBookmarkConcurrently() {
        // given
        val member = memberRepository.save(
            createMember(
                email = "cancel-bookmark@test.com",
                nickname = "cancelBookmarkUser",
            )
        )

        val category = categoryRepository.save(
            createCategory("북마크 취소 테스트 카테고리")
        )

        val post = postRepository.save(
            createPost(
                member = member,
                category = category,
            )
        )

        val memberId = member.userId
            ?: throw IllegalStateException("저장된 회원 ID가 없습니다.")

        val postId = post.postId
            ?: throw IllegalStateException("저장된 게시글 ID가 없습니다.")

        val createCommand = BookmarkCreateCommand(
            memberId = memberId,
            postId = postId,
        )

        val deleteCommand = BookmarkDeleteCommand(
            memberId = memberId,
            postId = postId,
        )

        bookmarkService.createBookmark(createCommand)

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

                    bookmarkService.cancelBookmark(deleteCommand)
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
        val bookmarkRowCount = bookmarkRepository.countByPost_PostId(postId)

        assertThat(bookmarkRowCount).isEqualTo(0)
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