package com.back.devc.domain.interaction.bookmark.service

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand
import com.back.devc.domain.interaction.bookmark.dto.BookmarkResponse
import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse
import com.back.devc.domain.interaction.bookmark.entity.Bookmark
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.member.member.entity.Member
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.member.util.MemberDisplayUtil
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.errorCode.BookmarkErrorCode
import com.back.devc.global.response.PageResponse
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class BookmarkService(
    private val bookmarkRepository: BookmarkRepository,
    private val postLikeRepository: PostLikeRepository,
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
) {

    /**
     * 게시글 북마크 추가
     *
     * 동시성 처리 방식:
     * - exists 확인 후 save 하지 않음
     * - DB unique constraint + insert ignore 사용
     * - 이미 북마크된 상태면 그대로 성공 응답
     */
    @Transactional
    fun createBookmark(command: BookmarkCreateCommand): BookmarkResponse {
        val userId = command.memberId
        val postId = command.postId

        validateMemberExists(userId)
        validatePostExists(postId)

        bookmarkRepository.insertIgnore(userId, postId)

        return BookmarkResponse(
            postId = postId,
            bookmarked = true,
        )
    }

    /**
     * 게시글 북마크 취소
     *
     * 동시성 처리 방식:
     * - 북마크 엔티티 조회 후 delete 하지 않음
     * - delete query의 affected row 수를 기준으로 처리
     * - 이미 취소된 상태면 그대로 성공 응답
     */
    @Transactional
    fun cancelBookmark(command: BookmarkDeleteCommand): BookmarkResponse {
        val userId = command.memberId
        val postId = command.postId

        validateMemberExists(userId)
        validatePostExists(postId)

        bookmarkRepository.deleteByUserIdAndPostId(userId, postId)

        return BookmarkResponse(
            postId = postId,
            bookmarked = false,
        )
    }

    /**
     * 북마크 목록 조회 - 기존 List 방식
     */
    fun getBookmarkedPosts(userId: Long): List<BookmarkedPostResponse> {
        val member = findMemberById(userId)

        val bookmarks = bookmarkRepository.findAllByMemberAndPost_IsDeletedFalse(member)

        return bookmarks.map { bookmark ->
            toBookmarkedPostResponse(bookmark, userId)
        }
    }

    /**
     * 북마크 목록 조회 - 페이징 방식
     */
    fun getBookmarkedPosts(
        userId: Long,
        pageable: Pageable,
    ): PageResponse<BookmarkedPostResponse> {
        val member = findMemberById(userId)

        val bookmarks = bookmarkRepository.findAllByMemberAndPost_IsDeletedFalse(
            member,
            pageable,
        )

        val responses = bookmarks.map { bookmark ->
            toBookmarkedPostResponse(bookmark, userId)
        }

        return PageResponse.from(responses)
    }

    /**
     * 현재 로그인한 사용자가 특정 게시글을 북마크했는지 확인
     */
    fun isBookmarkedByUser(
        userId: Long,
        postId: Long,
    ): Boolean {
        return bookmarkRepository.existsByMember_UserIdAndPost_PostId(userId, postId)
    }

    private fun toBookmarkedPostResponse(
        bookmark: Bookmark,
        userId: Long,
    ): BookmarkedPostResponse {
        val post: Post = bookmark.post
        val postId = post.postId ?: throw IllegalStateException("Post ID cannot be null")

        val liked = postLikeRepository.existsByMember_UserIdAndPost_PostId(
            userId,
            postId,
        )

        return BookmarkedPostResponse(
            postId = postId,
            title = post.title,
            authorNickname = MemberDisplayUtil.getDisplayName(post.member),
            categoryId = post.category.getCategoryId(),
            likeCount = post.likeCount.toLong(),
            commentCount = post.commentCount.toLong(),
            viewCount = post.viewCount.toLong(),
            createdAt = post.createdAt,
            liked = liked,
            bookmarked = true,
        )
    }

    /**
     * 북마크 목록 조회처럼 Member 엔티티가 필요한 경우 사용
     */
    private fun findMemberById(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow {
                EntityNotFoundException(
                    BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.code
                )
            }
    }

    /**
     * 북마크 생성/취소에서는 엔티티 조회 대신 존재 여부만 검증한다.
     */
    private fun validateMemberExists(userId: Long) {
        if (!memberRepository.existsById(userId)) {
            throw EntityNotFoundException(
                BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND.code
            )
        }
    }

    /**
     * 북마크 생성/취소 대상 게시글 존재 여부 검증
     */
    private fun validatePostExists(postId: Long) {
        if (postRepository.findByPostIdAndIsDeletedFalse(postId).isEmpty) {
            throw EntityNotFoundException(
                BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND.code
            )
        }
    }
}