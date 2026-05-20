package com.back.devc.domain.member.mypage.unit

import com.back.devc.toRepositoryResult

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.bookmark.service.BookmarkService
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.interaction.postLike.service.PostLikeService
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.mypage.dto.MyCommentResponse
import com.back.devc.domain.member.mypage.dto.MyPostResponse
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest
import com.back.devc.domain.member.mypage.service.MypageService
import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.MypageErrorCode
import com.back.devc.global.response.PageResponse
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.lang.reflect.Field
import java.time.LocalDateTime
import java.util.*

@DisplayName("MypageService 테스트")
@ExtendWith(MockitoExtension::class)
class MypageServiceTest {

    @InjectMocks
    private lateinit var mypageService: MypageService

    @Mock
    private lateinit var memberRepository: MemberRepository

    @Mock
    private lateinit var postRepository: PostRepository

    @Mock
    private lateinit var commentRepository: CommentRepository

    @Mock
    private lateinit var postLikeRepository: PostLikeRepository

    @Mock
    private lateinit var bookmarkRepository: BookmarkRepository

    @Mock
    private lateinit var postLikeService: PostLikeService

    @Mock
    private lateinit var bookmarkService: BookmarkService

    private val userId = 1L
    private val postId = 10L

    @Test
    @DisplayName("내 프로필 조회 성공")
    fun getMyProfileSuccess() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "기존닉네임")

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        // when
        val response = mypageService.getMyProfile(userId)

        // then
        assertThat(response.userId).isEqualTo(userId)
        assertThat(response.email).isEqualTo("test@test.com")
        assertThat(response.nickname).isEqualTo("기존닉네임")

        then(memberRepository).should().findById(userId)
    }

    @Test
    @DisplayName("내 프로필 조회 시 회원이 없으면 예외가 발생한다")
    fun getMyProfileMemberNotFound() {
        // given
        whenever(memberRepository.findById(userId))
            .thenReturn(null.toRepositoryResult())

        // when & then
        assertThatThrownBy {
            mypageService.getMyProfile(userId)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(MypageErrorCode.MYPAGE_404_MEMBER_NOT_FOUND.message)
    }

    @Test
    @DisplayName("내 게시글 목록 조회 성공")
    fun getMyPostsSuccess() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "작성자")
        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)

        val post = createPostWithId(
            postId = postId,
            member = member,
            title = "내 게시글",
            likeCount = 5,
            commentCount = 2,
            viewCount = 100,
            createdAt = createdAt,
        )

        val pageable = PageRequest.of(0, 10)

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(postRepository.findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(member, pageable))
            .thenReturn(PageImpl(listOf(post), pageable, 1))

        whenever(postLikeRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
            .thenReturn(true)

        whenever(bookmarkRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
            .thenReturn(false)

        // when
        val response: PageResponse<MyPostResponse> = mypageService.getMyPosts(userId, pageable)

        // then
        assertThat(response.content).hasSize(1)

        val myPost = response.content[0]

        assertThat(myPost.postId).isEqualTo(postId)
        assertThat(myPost.title).isEqualTo("내 게시글")
        assertThat(myPost.likeCount).isEqualTo(5L)
        assertThat(myPost.commentCount).isEqualTo(2L)
        assertThat(myPost.viewCount).isEqualTo(100L)
        assertThat(myPost.createdAt).isEqualTo(createdAt)
        assertThat(myPost.liked).isTrue()
        assertThat(myPost.bookmarked).isFalse()

        then(postRepository).should()
            .findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(member, pageable)

        then(postLikeRepository).should()
            .existsByMember_UserIdAndPost_PostId(userId, postId)

        then(bookmarkRepository).should()
            .existsByMember_UserIdAndPost_PostId(userId, postId)
    }

    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    fun getMyCommentsSuccess() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "작성자")
        val pageable = PageRequest.of(0, 10)
        val createdAt = LocalDateTime.of(2026, 5, 15, 10, 0)

        val comment = MyCommentResponse(
            commentId = 100L,
            postId = postId,
            postTitle = "게시글 제목",
            content = "댓글 내용",
            createdAt = createdAt,
        )

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(commentRepository.findMyComments(userId, pageable))
            .thenReturn(PageImpl(listOf(comment), pageable, 1))

        // when
        val response = mypageService.getMyComments(userId, pageable)

        // then
        assertThat(response.content).hasSize(1)

        val myComment = response.content[0]

        assertThat(myComment.commentId).isEqualTo(100L)
        assertThat(myComment.postId).isEqualTo(postId)
        assertThat(myComment.postTitle).isEqualTo("게시글 제목")
        assertThat(myComment.content).isEqualTo("댓글 내용")
        assertThat(myComment.createdAt).isEqualTo(createdAt)

        then(commentRepository).should().findMyComments(userId, pageable)
    }

    @Test
    @DisplayName("내 좋아요 게시글 목록 조회는 PostLikeService에 위임한다")
    fun getMyLikedPostsSuccess() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "작성자")
        val pageable = PageRequest.of(0, 10)

        val likedPost = LikedPostResponse(
            postId = postId,
            title = "좋아요한 게시글",
            authorNickname = "작성자",
            likeCount = 5L,
            commentCount = 2L,
            viewCount = 100L,
            createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
            liked = true,
            bookmarked = false,
        )

        val pageResponse = PageResponse.from(
            PageImpl(listOf(likedPost), pageable, 1)
        )

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(postLikeService.getLikedPosts(userId, pageable))
            .thenReturn(pageResponse)

        // when
        val response = mypageService.getMyLikedPosts(userId, pageable)

        // then
        assertThat(response.content).hasSize(1)
        assertThat(response.content[0].postId).isEqualTo(postId)
        assertThat(response.content[0].liked).isTrue()

        then(postLikeService).should().getLikedPosts(userId, pageable)
    }

    @Test
    @DisplayName("내 북마크 게시글 목록 조회는 BookmarkService에 위임한다")
    fun getMyBookmarkedPostsSuccess() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "작성자")
        val pageable = PageRequest.of(0, 10)

        val bookmarkedPost = BookmarkedPostResponse(
            postId = postId,
            title = "북마크한 게시글",
            authorNickname = "작성자",
            categoryId = 100L,
            likeCount = 5L,
            commentCount = 2L,
            viewCount = 100L,
            createdAt = LocalDateTime.of(2026, 5, 15, 10, 0),
            liked = false,
            bookmarked = true,
        )

        val pageResponse = PageResponse.from(
            PageImpl(listOf(bookmarkedPost), pageable, 1)
        )

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(bookmarkService.getBookmarkedPosts(userId, pageable))
            .thenReturn(pageResponse)

        // when
        val response = mypageService.getMyBookmarkedPosts(userId, pageable)

        // then
        assertThat(response.content).hasSize(1)
        assertThat(response.content[0].postId).isEqualTo(postId)
        assertThat(response.content[0].bookmarked).isTrue()

        then(bookmarkService).should().getBookmarkedPosts(userId, pageable)
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    fun updateMyProfileSuccess() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "기존닉네임")
        val request = UpdateMyProfileRequest("변경닉네임")

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(memberRepository.existsByNickname("변경닉네임"))
            .thenReturn(false)

        // when
        val response = mypageService.updateMyProfile(userId, request)

        // then
        assertThat(response.userId).isEqualTo(userId)
        assertThat(response.email).isEqualTo("test@test.com")
        assertThat(response.nickname).isEqualTo("변경닉네임")
        assertThat(member.nickname).isEqualTo("변경닉네임")

        then(memberRepository).should()
            .existsByNickname("변경닉네임")
    }

    @Test
    @DisplayName("내 프로필 수정 시 앞뒤 공백을 제거하고 닉네임을 변경한다")
    fun updateMyProfileTrimNickname() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "기존닉네임")
        val request = UpdateMyProfileRequest("  변경닉네임  ")

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(memberRepository.existsByNickname("변경닉네임"))
            .thenReturn(false)

        // when
        val response = mypageService.updateMyProfile(userId, request)

        // then
        assertThat(response.nickname).isEqualTo("변경닉네임")
        assertThat(member.nickname).isEqualTo("변경닉네임")
    }

    @Test
    @DisplayName("기존 닉네임과 동일하면 중복 검사를 하지 않고 성공한다")
    fun updateMyProfileSameNickname() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "기존닉네임")
        val request = UpdateMyProfileRequest("기존닉네임")

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        // when
        val response = mypageService.updateMyProfile(userId, request)

        // then
        assertThat(response.nickname).isEqualTo("기존닉네임")

        then(memberRepository)
            .should(never())
            .existsByNickname(any())
    }

    @Test
    @DisplayName("내 프로필 수정 시 이미 사용 중인 닉네임이면 예외가 발생한다")
    fun updateMyProfileDuplicateNickname() {
        // given
        val member = createMemberWithId(userId, "test@test.com", "기존닉네임")
        val request = UpdateMyProfileRequest("중복닉네임")

        whenever(memberRepository.findById(userId))
            .thenReturn(member.toRepositoryResult())

        whenever(memberRepository.existsByNickname("중복닉네임"))
            .thenReturn(true)

        // when & then
        assertThatThrownBy {
            mypageService.updateMyProfile(userId, request)
        }
            .isInstanceOf(ApiException::class.java)
            .hasMessage(MypageErrorCode.MYPAGE_409_NICKNAME_ALREADY_EXISTS.message)

        assertThat(member.nickname).isEqualTo("기존닉네임")
    }

    private fun createMemberWithId(
        userId: Long,
        email: String,
        nickname: String,
    ): Member {
        val member = Member.createLocalMember(
            email,
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
        val category = mock<Category>()

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