package com.back.devc.domain.post.post.service

import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository
import com.back.devc.domain.member.member.repository.MemberRepository
import com.back.devc.domain.member.searchLog.dto.CreateSearchLogRequest
import com.back.devc.domain.member.searchLog.service.SearchLogService
import com.back.devc.domain.post.category.repository.CategoryRepository
import com.back.devc.domain.post.comment.repository.CommentRepository
import com.back.devc.domain.post.post.dto.PostCreateRequest
import com.back.devc.domain.post.post.dto.PostCreateResponse
import com.back.devc.domain.post.post.dto.PostDeleteResponse
import com.back.devc.domain.post.post.dto.PostDetailResponse
import com.back.devc.domain.post.post.dto.PostListResponse
import com.back.devc.domain.post.post.dto.PostUpdateRequest
import com.back.devc.domain.post.post.dto.PostUpdateResponse
import com.back.devc.domain.post.post.entity.Post
import com.back.devc.domain.post.post.repository.PostRepository
import com.back.devc.domain.post.post.type.PostSearchType
import com.back.devc.domain.post.post.type.PostSortType
import com.back.devc.global.exception.ApiException
import com.back.devc.global.exception.errorCode.CategoryErrorCode
import com.back.devc.global.exception.errorCode.MemberErrorCode
import com.back.devc.global.exception.errorCode.PostErrorCode
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class PostService(
    private val postRepository: PostRepository,
    private val memberRepository: MemberRepository,
    private val categoryRepository: CategoryRepository,
    private val postLikeRepository: PostLikeRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val searchLogService: SearchLogService,
    private val commentRepository: CommentRepository,
) {

    @Transactional
    fun write(
        userId: Long,
        request: PostCreateRequest,
    ): PostCreateResponse {
        val member = memberRepository.findById(userId)
            .orElseThrow { ApiException(MemberErrorCode.MEMBER_NOT_FOUND) }

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ApiException(CategoryErrorCode.CATEGORY_404_NOT_FOUND) }

        val post = Post.Companion.create(
            member = member,
            category = category,
            title = request.title,
            content = request.content,
        )

        val savedPost = postRepository.save(post)


        return PostCreateResponse.Companion.from(savedPost)
    }

    @Transactional
    fun findDetailById(
        postId: Long,
        loginUserId: Long?,
    ): PostDetailResponse {
        val post = findActivePost(postId)

        post.increaseViewCount()

        val liked = loginUserId?.let {
            postLikeRepository.existsByMember_UserIdAndPost_PostId(it, postId)
        } ?: false

        val bookmarked = loginUserId?.let {
            bookmarkRepository.existsByMember_UserIdAndPost_PostId(it, postId)
        } ?: false

        val bookmarkCount = bookmarkRepository.countByPost_PostId(postId).toInt()

        syncCommentCount(post)

        return PostDetailResponse.Companion.from(
            post,
            liked,
            bookmarked,
            bookmarkCount,
        )
    }

    fun getPosts(
        loginUserId: Long?,
        categoryId: Long?,
        keyword: String?,
        searchType: PostSearchType?,
        sort: PostSortType,
        page: Int,
        size: Int,
    ): Page<PostListResponse> {
        val pageable: Pageable = PageRequest.of(
            page,
            size,
            createSort(sort),
        )

        val trimmedKeyword = keyword?.trim()?.takeIf { it.isNotBlank() }

        if (loginUserId != null && trimmedKeyword != null) {
            searchLogService.createSearchLog(
                loginUserId,
                CreateSearchLogRequest(trimmedKeyword),
            )
        }

        val posts = searchPosts(
            categoryId = categoryId,
            keyword = trimmedKeyword,
            searchType = searchType,
            pageable = pageable,
        )

        return posts.map { post ->

            val postId = requireNotNull(post.postId)

            val liked = loginUserId?.let {
                postLikeRepository.existsByMember_UserIdAndPost_PostId(it, postId)
            } ?: false

            val bookmarked = loginUserId?.let {
                bookmarkRepository.existsByMember_UserIdAndPost_PostId(it, postId)
            } ?: false

            PostListResponse.Companion.from(
                post,
                liked,
                bookmarked,
            )
        }
    }

    @Transactional
    fun update(
        memberId: Long,
        postId: Long,
        request: PostUpdateRequest,
    ): PostUpdateResponse {
        val post = findActivePost(postId)

        validatePostOwner(post, memberId)

        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ApiException(CategoryErrorCode.CATEGORY_404_NOT_FOUND) }

        post.update(
            request.title,
            request.content,
            category,
        )

        return PostUpdateResponse.Companion.from(post)
    }

    @Transactional
    fun delete(
        memberId: Long,
        postId: Long,
    ): PostDeleteResponse {
        val post = findActivePost(postId)

        validatePostOwner(post, memberId)

        bookmarkRepository.deleteByPost_PostId(postId)
        postLikeRepository.deleteByPost_PostId(postId)

        commentRepository.findByPostIdAndIsDeletedFalse(postId)
            .forEach { comment ->
                comment.softDelete()
            }

        post.delete()

        return PostDeleteResponse.Companion.of(postId)
    }

    @Transactional
    fun increaseViewCount(postId: Long) {
        val post = findActivePost(postId)
        post.increaseViewCount()
    }

    @Transactional
    fun increaseCommentCount(postId: Long) {
        val post = findActivePost(postId)
        post.increaseCommentCount()
    }

    @Transactional
    fun decreaseCommentCount(postId: Long) {
        val post = findActivePost(postId)
        post.decreaseCommentCount()
    }

    private fun searchPosts(
        categoryId: Long?,
        keyword: String?,
        searchType: PostSearchType?,
        pageable: Pageable,
    ): Page<Post> {
        if (keyword != null) {
            return when (searchType) {
                PostSearchType.TITLE -> {
                    if (categoryId != null) {
                        postRepository.findByCategoryCategoryIdAndTitleContainingAndIsDeletedFalse(
                            categoryId,
                            keyword,
                            pageable,
                        )
                    } else {
                        postRepository.findByTitleContainingAndIsDeletedFalse(
                            keyword,
                            pageable,
                        )
                    }
                }

                PostSearchType.CONTENT -> {
                    if (categoryId != null) {
                        postRepository.findByCategoryCategoryIdAndContentContainingAndIsDeletedFalse(
                            categoryId,
                            keyword,
                            pageable,
                        )
                    } else {
                        postRepository.findByContentContainingAndIsDeletedFalse(
                            keyword,
                            pageable,
                        )
                    }
                }

                else -> {
                    if (categoryId != null) {
                        postRepository.searchPosts(
                            categoryId,
                            keyword,
                            pageable,
                        )
                    } else {
                        postRepository.searchByKeyword(
                            keyword,
                            pageable,
                        )
                    }
                }
            }
        }

        if (categoryId != null) {
            if (!categoryRepository.existsById(categoryId)) {
                throw ApiException(CategoryErrorCode.CATEGORY_404_NOT_FOUND)
            }

            return postRepository.findByCategoryCategoryIdAndIsDeletedFalse(
                categoryId,
                pageable,
            )
        }

        return postRepository.findByIsDeletedFalse(pageable)
    }

    private fun createSort(sort: PostSortType): Sort {
        return when (sort) {
            PostSortType.VIEWS -> Sort.by(
                Sort.Order.desc("viewCount"),
                Sort.Order.desc("createdAt"),
            )

            PostSortType.LIKES -> Sort.by(
                Sort.Order.desc("likeCount"),
                Sort.Order.desc("createdAt"),
            )

            else -> Sort.by(
                Sort.Order.desc("createdAt"),
            )
        }
    }

    private fun findActivePost(postId: Long): Post {
        return postRepository.findByPostIdAndIsDeletedFalse(postId)
            .orElseThrow {
                ApiException(PostErrorCode.POST_404_NOT_FOUND)
            }
    }

    private fun validatePostOwner(
        post: Post,
        memberId: Long,
    ) {
        if (post.member.userId != memberId) {
            throw ApiException(PostErrorCode.POST_403_FORBIDDEN)
        }

        if (post.isDeleted) {
            throw ApiException(PostErrorCode.POST_401_1_ALREADY_DELETED)
        }
    }

    private fun syncCommentCount(post: Post) {
        val postId = requireNotNull(post.postId)

        val actualCommentCount = commentRepository
            .countByPostIdAndIsDeletedFalse(postId)
            .toInt()
    }
}