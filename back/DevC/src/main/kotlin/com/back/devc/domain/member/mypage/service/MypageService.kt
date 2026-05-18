package com.back.devc.domain.member.mypage.service

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
import com.back.devc.domain.member.mypage.dto.MyProfileResponse
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.global.exception.errorCode.MypageErrorCode
import com.back.devc.global.response.PageResponse
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MypageService(
    private val memberRepository: MemberRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val postLikeRepository: PostLikeRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val postLikeService: PostLikeService,
    private val bookmarkService: BookmarkService,
) {

    private fun getMember(userId: Long): Member {
        return memberRepository.findById(userId)
            .orElseThrow {
                EntityNotFoundException(
                    MypageErrorCode.MYPAGE_404_MEMBER_NOT_FOUND.getCode()
                )
            }
    }

    fun getMyProfile(userId: Long): MyProfileResponse {
        val member = getMember(userId)

        return MyProfileResponse(
            userId = userId,
            email = member.email,
            nickname = member.nickname,
        )
    }

    fun getMyPosts(
        userId: Long,
        pageable: Pageable,
    ): PageResponse<MyPostResponse> {
        val member = getMember(userId)

        val posts = postRepository.findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(
            member,
            pageable,
        )

        val response = posts.map { post ->
            val postId = post.getPostId()

            val liked = postLikeRepository.existsByMember_UserIdAndPost_PostId(
                userId,
                postId,
            )

            val bookmarked = bookmarkRepository.existsByMember_UserIdAndPost_PostId(
                userId,
                postId,
            )

            MyPostResponse(
                postId = postId,
                title = post.getTitle(),
                likeCount = post.getLikeCount().toLong(),
                commentCount = post.getCommentCount().toLong(),
                viewCount = post.getViewCount().toLong(),
                createdAt = post.getCreatedAt(),
                liked = liked,
                bookmarked = bookmarked,
            )
        }

        return PageResponse.from(response)
    }

    fun getMyComments(
        userId: Long,
        pageable: Pageable,
    ): PageResponse<MyCommentResponse> {
        getMember(userId)

        val comments = commentRepository.findMyComments(userId, pageable)

        return PageResponse.from(comments)
    }

    fun getMyLikedPosts(
        userId: Long,
        pageable: Pageable,
    ): PageResponse<LikedPostResponse> {
        getMember(userId)

        return postLikeService.getLikedPosts(userId, pageable)
    }

    fun getMyBookmarkedPosts(
        userId: Long,
        pageable: Pageable,
    ): PageResponse<BookmarkedPostResponse> {
        getMember(userId)

        return bookmarkService.getBookmarkedPosts(userId, pageable)
    }

    @Transactional
    fun updateMyProfile(
        userId: Long,
        request: UpdateMyProfileRequest,
    ): MyProfileResponse {
        val member = getMember(userId)

        val newNickname = request.nickname.trim()

        if (member.nickname != newNickname &&
            memberRepository.existsByNickname(newNickname)
        ) {
            throw IllegalArgumentException(
                MypageErrorCode.MYPAGE_409_NICKNAME_ALREADY_EXISTS.getCode()
            )
        }

        member.updateNickname(newNickname)

        return MyProfileResponse(
            userId = userId,
            email = member.email,
            nickname = member.nickname,
        )
    }
}