package com.back.devc.domain.interaction.bookmark.unit

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse
import com.back.devc.domain.interaction.bookmark.entity.Bookmark
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.bookmark.service.BookmarkService
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.BookmarkErrorCode
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
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.*

@DisplayName("BookmarkService 테스트")
@ExtendWith(MockitoExtension::class)
class BookmarkServiceTest {

    @InjectMocks
    private lateinit var bookmarkService: BookmarkService

    @Mock
    private lateinit var bookmarkRepository: BookmarkRepository

    @Mock
    private lateinit var postLikeRepository: PostLikeRepository

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var postRepository: PostRepository

    private val userId = 1L
    private val postId = 10L

    @Test
    @DisplayName("북마크 추가 성공")
    fun createBookmarkSuccess() {
        // given
        val command = BookmarkCreateCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(bookmarkRepository.insertIgnore(userId, postId))
            .willReturn(1)

        // when
        val response = bookmarkService.createBookmark(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.bookmarked).isTrue()

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(bookmarkRepository).should().insertIgnore(userId, postId)
    }

    @Test
    @DisplayName("이미 북마크된 게시글이어도 성공 응답을 반환한다")
    fun createBookmarkAlreadyExists() {
        // given
        val command = BookmarkCreateCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(bookmarkRepository.insertIgnore(userId, postId))
            .willReturn(0)

        // when
        val response = bookmarkService.createBookmark(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.bookmarked).isTrue()

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(bookmarkRepository).should().insertIgnore(userId, postId)
    }

    @Test
    @DisplayName("북마크 추가 시 회원이 없으면 예외가 발생한다")
    fun createBookmarkMemberNotFound() {
        // given
        val command = BookmarkCreateCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(false)

        // when & then
        assertThatThrownBy {
            bookmarkService.createBookmark(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong())
        then(bookmarkRepository).should(never()).insertIgnore(anyLong(), anyLong())
    }

    @Test
    @DisplayName("북마크 추가 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    fun createBookmarkPostNotFound() {
        // given
        val command = BookmarkCreateCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            bookmarkService.createBookmark(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(bookmarkRepository).should(never()).insertIgnore(anyLong(), anyLong())
    }

    @Test
    @DisplayName("북마크 취소 성공")
    fun cancelBookmarkSuccess() {
        // given
        val command = BookmarkDeleteCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(bookmarkRepository.deleteByUserIdAndPostId(userId, postId))
            .willReturn(1)

        // when
        val response = bookmarkService.cancelBookmark(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.bookmarked).isFalse()

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(bookmarkRepository).should().deleteByUserIdAndPostId(userId, postId)
    }

    @Test
    @DisplayName("이미 취소된 북마크여도 성공 응답을 반환한다")
    fun cancelBookmarkAlreadyCanceled() {
        // given
        val command = BookmarkDeleteCommand(userId, postId)
        val post = mock(Post::class.java)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.of(post))
        given(bookmarkRepository.deleteByUserIdAndPostId(userId, postId))
            .willReturn(0)

        // when
        val response = bookmarkService.cancelBookmark(command)

        // then
        assertThat(response.postId).isEqualTo(postId)
        assertThat(response.bookmarked).isFalse()

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(bookmarkRepository).should().deleteByUserIdAndPostId(userId, postId)
    }

    @Test
    @DisplayName("북마크 취소 시 회원이 없으면 예외가 발생한다")
    fun cancelBookmarkMemberNotFound() {
        // given
        val command = BookmarkDeleteCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(false)

        // when & then
        assertThatThrownBy {
            bookmarkService.cancelBookmark(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should(never()).findByPostIdAndIsDeletedFalse(anyLong())
        then(bookmarkRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong())
    }

    @Test
    @DisplayName("북마크 취소 시 게시글이 없거나 삭제된 게시글이면 예외가 발생한다")
    fun cancelBookmarkPostNotFound() {
        // given
        val command = BookmarkDeleteCommand(userId, postId)

        given(memberRepository.existsById(userId)).willReturn(true)
        given(postRepository.findByPostIdAndIsDeletedFalse(postId))
            .willReturn(Optional.empty())

        // when & then
        assertThatThrownBy {
            bookmarkService.cancelBookmark(command)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.message)

        then(memberRepository).should().existsById(userId)
        then(postRepository).should().findByPostIdAndIsDeletedFalse(postId)
        then(bookmarkRepository).should(never()).deleteByUserIdAndPostId(anyLong(), anyLong())
    }

    @Test
    @DisplayName("내가 북마크한 게시글 목록을 페이징으로 반환한다")
    fun getBookmarkedPostsSuccess() {
        // given
        val member = createMemberWithId(userId, "작성자")

        val category = mock(Category::class.java)
        given(category.categoryId).willReturn(100L)

        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)

        val post = createPostWithId(
            postId = postId,
            member = member,
            category = category,
            title = "테스트 제목",
            likeCount = 7,
            commentCount = 2,
            viewCount = 100,
            createdAt = createdAt,
        )

        val bookmark = Bookmark.create(member, post)
        val pageable = PageRequest.of(0, 10)

        given(
            bookmarkRepository.findPageWithPostMemberCategoryByUserId(
                userId,
                pageable,
            )
        ).willReturn(PageImpl(listOf(bookmark), pageable, 1))

        given(
            postLikeRepository.findLikedPostIdsByUserIdAndPostIds(
                eq(userId),
                anyCollection(),
            )
        ).willReturn(listOf(postId))

        // when
        val response = bookmarkService.getBookmarkedPosts(userId, pageable)

        // then
        assertThat(response.content).hasSize(1)

        val bookmarkedPost: BookmarkedPostResponse = response.content[0]

        assertThat(bookmarkedPost.postId).isEqualTo(postId)
        assertThat(bookmarkedPost.title).isEqualTo("테스트 제목")
        assertThat(bookmarkedPost.authorNickname).isEqualTo("작성자")
        assertThat(bookmarkedPost.categoryId).isEqualTo(100L)
        assertThat(bookmarkedPost.likeCount).isEqualTo(7L)
        assertThat(bookmarkedPost.commentCount).isEqualTo(2L)
        assertThat(bookmarkedPost.viewCount).isEqualTo(100L)
        assertThat(bookmarkedPost.createdAt).isEqualTo(createdAt)
        assertThat(bookmarkedPost.liked).isTrue()
        assertThat(bookmarkedPost.bookmarked).isTrue()

        then(bookmarkRepository).should()
            .findPageWithPostMemberCategoryByUserId(userId, pageable)

        then(postLikeRepository).should()
            .findLikedPostIdsByUserIdAndPostIds(eq(userId), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())

        then(postLikeRepository).should(never())
            .existsByMember_UserIdAndPost_PostId(anyLong(), anyLong())
    }

    @Test
    @DisplayName("북마크 목록이 비어 있으면 빈 페이지를 반환한다")
    fun getBookmarkedPostsEmpty() {
        // given
        val pageable = PageRequest.of(0, 10)

        given(
            bookmarkRepository.findPageWithPostMemberCategoryByUserId(
                userId,
                pageable,
            )
        ).willReturn(PageImpl(emptyList(), pageable, 0))

        // when
        val response = bookmarkService.getBookmarkedPosts(userId, pageable)

        // then
        assertThat(response.content).isEmpty()

        then(bookmarkRepository).should()
            .findPageWithPostMemberCategoryByUserId(userId, pageable)

        then(postLikeRepository).should(never())
            .findLikedPostIdsByUserIdAndPostIds(anyLong(), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())
    }

    @Test
    @DisplayName("내가 북마크한 게시글 목록을 List로 반환한다")
    fun getBookmarkedPostsListSuccess() {
        // given
        val member = createMemberWithId(userId, "작성자")

        val category = mock(Category::class.java)
        given(category.categoryId).willReturn(100L)

        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)

        val post = createPostWithId(
            postId = postId,
            member = member,
            category = category,
            title = "테스트 제목",
            likeCount = 7,
            commentCount = 2,
            viewCount = 100,
            createdAt = createdAt,
        )

        val bookmark = Bookmark.create(member, post)

        given(bookmarkRepository.findAllWithPostMemberCategoryByUserId(userId))
            .willReturn(listOf(bookmark))

        given(
            postLikeRepository.findLikedPostIdsByUserIdAndPostIds(
                eq(userId),
                anyCollection(),
            )
        ).willReturn(listOf(postId))

        // when
        val response = bookmarkService.getBookmarkedPosts(userId)

        // then
        assertThat(response).hasSize(1)

        val bookmarkedPost = response[0]

        assertThat(bookmarkedPost.postId).isEqualTo(postId)
        assertThat(bookmarkedPost.title).isEqualTo("테스트 제목")
        assertThat(bookmarkedPost.authorNickname).isEqualTo("작성자")
        assertThat(bookmarkedPost.categoryId).isEqualTo(100L)
        assertThat(bookmarkedPost.likeCount).isEqualTo(7L)
        assertThat(bookmarkedPost.commentCount).isEqualTo(2L)
        assertThat(bookmarkedPost.viewCount).isEqualTo(100L)
        assertThat(bookmarkedPost.createdAt).isEqualTo(createdAt)
        assertThat(bookmarkedPost.liked).isTrue()
        assertThat(bookmarkedPost.bookmarked).isTrue()

        then(bookmarkRepository).should()
            .findAllWithPostMemberCategoryByUserId(userId)

        then(postLikeRepository).should()
            .findLikedPostIdsByUserIdAndPostIds(eq(userId), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())

        then(postLikeRepository).should(never())
            .existsByMember_UserIdAndPost_PostId(anyLong(), anyLong())
    }

    @Test
    @DisplayName("List 북마크 목록이 비어 있으면 빈 리스트를 반환한다")
    fun getBookmarkedPostsListEmpty() {
        // given
        given(bookmarkRepository.findAllWithPostMemberCategoryByUserId(userId))
            .willReturn(emptyList())

        // when
        val response = bookmarkService.getBookmarkedPosts(userId)

        // then
        assertThat(response).isEmpty()

        then(bookmarkRepository).should()
            .findAllWithPostMemberCategoryByUserId(userId)

        then(postLikeRepository).should(never())
            .findLikedPostIdsByUserIdAndPostIds(anyLong(), anyCollection())

        then(memberRepository).should(never()).findById(anyLong())
    }

    @Test
    @DisplayName("사용자가 특정 게시글을 북마크했는지 확인한다")
    fun isBookmarkedByUserSuccess() {
        // given
        given(
            bookmarkRepository.existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(
                userId,
                postId,
            )
        ).willReturn(true)

        // when
        val result = bookmarkService.isBookmarkedByUser(userId, postId)

        // then
        assertThat(result).isTrue()

        then(bookmarkRepository).should()
            .existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(userId, postId)
    }

    @Test
    @DisplayName("사용자가 특정 게시글을 북마크하지 않았으면 false를 반환한다")
    fun isBookmarkedByUserFalse() {
        // given
        given(
            bookmarkRepository.existsByMember_UserIdAndPost_PostIdAndPost_IsDeletedFalse(
                userId,
                postId,
            )
        ).willReturn(false)

        // when
        val result = bookmarkService.isBookmarkedByUser(userId, postId)

        // then
        assertThat(result).isFalse()

        then(bookmarkRepository).should()
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
        category: Category,
        title: String,
        likeCount: Int,
        commentCount: Int,
        viewCount: Int,
        createdAt: LocalDateTime,
    ): Post {
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