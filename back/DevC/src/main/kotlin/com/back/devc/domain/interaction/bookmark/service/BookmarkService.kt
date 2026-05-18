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
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.BookmarkErrorCode
import com.back.devc.global.response.PageResponse
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

    // 게시글 북마크 추가
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

    // 게시글 북마크 취소
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

    // 북마크 목록 조회 - List
    fun getBookmarkedPosts(userId: Long): List<BookmarkedPostResponse> {
        val member = findMemberById(userId)
        val bookmarks = bookmarkRepository.findAllByMemberAndPost_IsDeletedFalse(member)

        return bookmarks.map { bookmark ->
            toBookmarkedPostResponse(bookmark, userId)
        }
    }

    // 북마크 목록 조회 - Paging
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

    // 현재 사용자의 북마크 여부 확인
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
        val postId = post.postId
            ?: throw ApiException(BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND)

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

    private fun findMemberById(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow {
                ApiException(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND)
            }
    }

    private fun validateMemberExists(userId: Long) {
        if (!memberRepository.existsById(userId)) {
            throw ApiException(BookmarkErrorCode.BOOKMARK_404_MEMBER_NOT_FOUND)
        }
    }

    private fun validatePostExists(postId: Long) {
        if (postRepository.findByPostIdAndIsDeletedFalse(postId).isEmpty) {
            throw ApiException(BookmarkErrorCode.BOOKMARK_404_POST_NOT_FOUND)
        }
    }
}