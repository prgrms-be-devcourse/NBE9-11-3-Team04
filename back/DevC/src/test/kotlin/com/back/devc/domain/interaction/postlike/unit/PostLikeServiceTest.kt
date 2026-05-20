package com.back.devc.domain.interaction.postlike.unit

import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.notification.service.NotificationService
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.interaction.postLike.dto.LikedPostsQuery
import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand
import com.back.devc.domain.interaction.postLike.entity.PostLike
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.interaction.postLike.service.PostLikeService
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.PostLikeErrorCode
import com.back.devc.global.response.successCode.PostLikeSuccessCode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.*
import org.mockito.BDDMockito.given
import org.mockito.BDDMockito.then
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.*

@DisplayName("PostLikeService 테스트")
@ExtendWith(MockitoExtension::class)
class PostLikeServiceTest {

    @InjectMocks
    private lateinit var postLikeService: PostLikeService

    @Mock
    private lateinit var postLikeRepository: PostLikeRepository

    @Mock
    private lateinit var bookmarkRepository: BookmarkRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var notificationService: NotificationService

    private val userId = 1L
    private val postId = 10L

    @Test
    @DisplayName("처음 좋아요하면 좋아요가 생성되고 카운트가 증가한다")
    fun createLikeSuccess() {
        // given
        val command = PostLikeCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(postLikeRepository.insertIgnore(userId, postId))
            .willReturn(1)
        given(postRepository.findLikeCountByPostId(postId))
            .willReturn(5)

        // when
        val response = postLikeService.createLike(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.liked).isTrue()
        assertThat(response.likeCount).isEqualTo(5L)
        assertThat(response.message)
            .isEqualTo(PostLikeSuccessCode.POST_LIKE_CREATED.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(postLikeRepository).should().insertIgnore(userId, postId)
        then(postRepository).should().increaseLikeCount(postId)
        then(notificationService).should()
            .createPostLikeNotification(postId, userId)
    }

    @Test
    @DisplayName("이미 좋아요한 게시글이면 카운트와 알림을 생성하지 않는다")
    fun createLikeAlreadyExists() {
        // given
        val command = PostLikeCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(postLikeRepository.insertIgnore(userId, postId))
            .willReturn(0)
        given(postRepository.findLikeCountByPostId(postId))
            .willReturn(3)

        // when
        val response = postLikeService.createLike(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.liked).isTrue()
        assertThat(response.likeCount).isEqualTo(3L)
        assertThat(response.message)
            .isEqualTo(PostLikeSuccessCode.POST_LIKE_ALREADY_EXISTS.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(postLikeRepository).should().insertIgnore(userId, postId)
        then(postRepository).should(never()).increaseLikeCount(anyLong())
        then(notificationService).should(never())
            .createPostLikeNotification(anyLong(), anyLong())
    }

    @Test
    @DisplayName("좋아요 생성 시 회원이 없으면 예외가 발생한다")
    fun createLikeMemberNotFound() {
        // given
        val command = PostLikeCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(false)

        // when & then
        assertThatThrownBy {
            postLikeService.createLike(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong())
        then(postLikeRepository).should(never()).insertIgnore(anyLong(), anyLong())
        then(postRepository).should(never()).increaseLikeCount(anyLong())

        verifyNoInteractions(notificationService)
    }

    @Test
    @DisplayName("좋아요 생성 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    fun createLikePostNotFound() {
        // given
        val command = PostLikeCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            postLikeService.createLike(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(postLikeRepository).should(never()).insertIgnore(anyLong(), anyLong())
        then(postRepository).should(never()).increaseLikeCount(anyLong())

        verifyNoInteractions(notificationService)
    }

    @Test
    @DisplayName("좋아요가 존재하면 삭제되고 카운트가 감소한다")
    fun cancelLikeSuccess() {
        // given
        val command = PostLikeCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(postLikeRepository.deleteByUserIdAndPostId(userId, postId))
            .willReturn(1)
        given(postRepository.findLikeCountByPostId(postId))
            .willReturn(2)

        // when
        val response = postLikeService.cancelLike(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.liked).isFalse()
        assertThat(response.likeCount).isEqualTo(2L)
        assertThat(response.message)
            .isEqualTo(PostLikeSuccessCode.POST_LIKE_CANCELED.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(postLikeRepository).should().deleteByUserIdAndPostId(userId, postId)
        then(postRepository).should().decreaseLikeCount(postId)
    }

    @Test
    @DisplayName("이미 취소된 상태면 카운트를 감소하지 않는다")
    fun cancelLikeAlreadyCanceled() {
        // given
        val command = PostLikeCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(postLikeRepository.deleteByUserIdAndPostId(userId, postId))
            .willReturn(0)
        given(postRepository.findLikeCountByPostId(postId))
            .willReturn(2)

        // when
        val response = postLikeService.cancelLike(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.liked).isFalse()
        assertThat(response.likeCount).isEqualTo(2L)
        assertThat(response.message)
            .isEqualTo(PostLikeSuccessCode.POST_LIKE_ALREADY_CANCELED.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(postLikeRepository).should().deleteByUserIdAndPostId(userId, postId)
        then(postRepository).should(never()).decreaseLikeCount(anyLong())
    }

    @Test
    @DisplayName("좋아요 취소 시 회원이 없으면 예외가 발생한다")
    fun cancelLikeMemberNotFound() {
        // given
        val command = PostLikeCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(false)

        // when & then
        assertThatThrownBy {
            postLikeService.cancelLike(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(PostLikeErrorCode.POST_LIKE_404_MEMBER_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong())
        then(postLikeRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong())
        then(postRepository).should(never()).decreaseLikeCount(anyLong())
    }

    @Test
    @DisplayName("좋아요 취소 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    fun cancelLikePostNotFound() {
        // given
        val command = PostLikeCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            postLikeService.cancelLike(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(PostLikeErrorCode.POST_LIKE_404_POST_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(postLikeRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong())
        then(postRepository).should(never()).decreaseLikeCount(anyLong())
    }

    @Test
    @DisplayName("내가 좋아요한 게시글 목록을 페이징으로 반환한다")
    fun getLikedPostsSuccess() {
        // given
        val member = createMemberWithId(userId, "작성자")
        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)

        val post = createPostWithId(
            postId = postId,
            member = member,
            title = "테스트 제목",
            likeCount = 7,
            commentCount = 2,
            viewCount = 100,
            createdAt = createdAt,
        )

        val postLike = PostLike.create(member, post)
        val pageable = PageRequest.of(0, 10)

        given(
            postLikeRepository.findPageWithPostMemberByUserId(
                userId,
                pageable,
            )
        ).willReturn(PageImpl(listOf(postLike), pageable, 1))

        given(
            bookmarkRepository.findBookmarkedPostIdsByUserIdAndPostIds(
                eq(userId),
                anyCollection(),
            )
        ).willReturn(listOf(postId))

        // when
        val responses = postLikeService.getLikedPosts(userId, pageable)

        // then
        assertThat(responses.content).hasSize(1)

        val response: LikedPostResponse = responses.content[0]

        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.title).isEqualTo("테스트 제목")
        assertThat(response.authorNickname).isEqualTo("작성자")
        assertThat(response.likeCount).isEqualTo(7L)
        assertThat(response.commentCount).isEqualTo(2L)
        assertThat(response.viewCount).isEqualTo(100L)
        assertThat(response.createdAt).isEqualTo(createdAt)
        assertThat(response.liked).isTrue()
        assertThat(response.bookmarked).isTrue()

        then(postLikeRepository).should()
            .findPageWithPostMemberByUserId(userId, pageable)

        then(bookmarkRepository).should()
            .findBookmarkedPostIdsByUserIdAndPostIds(eq(userId), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())

        then(bookmarkRepository).should(never())
            .existsByMember_UserIdAndPost_PostId(anyLong(), anyLong())
    }

    @Test
    @DisplayName("좋아요한 게시글 목록이 비어 있으면 빈 페이지를 반환한다")
    fun getLikedPostsEmpty() {
        // given
        val pageable = PageRequest.of(0, 10)

        given(
            postLikeRepository.findPageWithPostMemberByUserId(
                userId,
                pageable,
            )
        ).willReturn(PageImpl(emptyList(), pageable, 0))

        // when
        val responses = postLikeService.getLikedPosts(userId, pageable)

        // then
        assertThat(responses.content).isEmpty()

        then(postLikeRepository).should()
            .findPageWithPostMemberByUserId(userId, pageable)

        then(bookmarkRepository).should(never())
            .findBookmarkedPostIdsByUserIdAndPostIds(anyLong(), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())
    }

    @Test
    @DisplayName("내가 좋아요한 게시글 목록을 List로 반환한다")
    fun getLikedPostsListSuccess() {
        // given
        val member = createMemberWithId(userId, "작성자")
        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)

        val post = createPostWithId(
            postId = postId,
            member = member,
            title = "테스트 제목",
            likeCount = 7,
            commentCount = 2,
            viewCount = 100,
            createdAt = createdAt,
        )

        val postLike = PostLike.create(member, post)

        given(postLikeRepository.findAllWithPostMemberByUserId(userId))
            .willReturn(listOf(postLike))

        given(
            bookmarkRepository.findBookmarkedPostIdsByUserIdAndPostIds(
                eq(userId),
                anyCollection(),
            )
        ).willReturn(listOf(postId))

        // when
        val responses = postLikeService.getLikedPosts(LikedPostsQuery(userId))

        // then
        assertThat(responses).hasSize(1)

        val response = responses[0]

        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.title).isEqualTo("테스트 제목")
        assertThat(response.authorNickname).isEqualTo("작성자")
        assertThat(response.likeCount).isEqualTo(7L)
        assertThat(response.commentCount).isEqualTo(2L)
        assertThat(response.viewCount).isEqualTo(100L)
        assertThat(response.createdAt).isEqualTo(createdAt)
        assertThat(response.liked).isTrue()
        assertThat(response.bookmarked).isTrue()

        then(postLikeRepository).should()
            .findAllWithPostMemberByUserId(userId)

        then(bookmarkRepository).should()
            .findBookmarkedPostIdsByUserIdAndPostIds(eq(userId), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())

        then(bookmarkRepository).should(never())
            .existsByMember_UserIdAndPost_PostId(anyLong(), anyLong())
    }

    @Test
    @DisplayName("List 좋아요 목록이 비어 있으면 빈 리스트를 반환한다")
    fun getLikedPostsListEmpty() {
        // given
        given(postLikeRepository.findAllWithPostMemberByUserId(userId))
            .willReturn(emptyList())

        // when
        val responses = postLikeService.getLikedPosts(LikedPostsQuery(userId))

        // then
        assertThat(responses).isEmpty()

        then(postLikeRepository).should()
            .findAllWithPostMemberByUserId(userId)

        then(bookmarkRepository).should(never())
            .findBookmarkedPostIdsByUserIdAndPostIds(anyLong(), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())
    }

    @Test
    @DisplayName("사용자가 특정 게시글을 좋아요했는지 확인한다")
    fun isLikedByUserSuccess() {
        // given
        given(
            postLikeRepository.existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(
                userId,
                postId,
            )
        ).willReturn(true)

        // when
        val result = postLikeService.isLikedByUser(userId, postId)

        // then
        assertThat(result).isTrue()

        then(postLikeRepository).should()
            .existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(userId, postId)
    }

    @Test
    @DisplayName("사용자가 특정 게시글을 좋아요하지 않았으면 false를 반환한다")
    fun isLikedByUserFalse() {
        // given
        given(
            postLikeRepository.existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(
                userId,
                postId,
            )
        ).willReturn(false)

        // when
        val result = postLikeService.isLikedByUser(userId, postId)

        // then
        assertThat(result).isFalse()

        then(postLikeRepository).should()
            .existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(userId, postId)
    }

    private fun createMemberWithId(
        userId: Long,
        nickname: String,
    ): Member {
        val member = Member.createLocalMember(
            "test@test.com",
            "encodedPassword",
            nickname,
        )

        setField(member, "userId", userId)

        return member
    }

    private fun createPostWithId(
        postId: Long,
        member: Member,
        title: String,
        likeCount: Int,
        commentCount: Int,
        viewCount: Int,
        createdAt: LocalDateTime,
    ): Post {
        val category = mock(Category::class.java)

        val post = Post.create(
            member,
            category,
            title,
            "테스트 내용",
        )

        setField(post, "postId", postId)
        setField(post, "likeCount", likeCount)
        setField(post, "commentCount", commentCount)
        setField(post, "viewCount", viewCount)
        setField(post, "isDeleted", false)
        setField(post, "createdAt", createdAt)
        setField(post, "updatedAt", createdAt)

        return post
    }

    private fun setField(
        target: Any,
        fieldName: String,
        value: Any?,
    ) {
        try {
            val field: Field = target.javaClass.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(target, value)
        } catch (e: ReflectiveOperationException) {
            throw RuntimeException(e)
        }
    }
}