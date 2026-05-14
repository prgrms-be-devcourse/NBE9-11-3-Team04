package com.back.devc.domain.post.post.service;

import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.searchLog.dto.CreateSearchLogRequest;
import com.back.devc.domain.member.searchLog.service.SearchLogService;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.dto.*;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.domain.post.post.type.PostSearchType;
import com.back.devc.domain.post.post.type.PostSortType;
import com.back.devc.global.exception.ApiException;
import com.back.devc.global.exception.errorCode.CategoryErrorCode;
import com.back.devc.global.exception.errorCode.MemberErrorCode;
import com.back.devc.global.exception.errorCode.PostErrorCode;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;

@Service
@RequiredArgsConstructor
@Transactional
public class PostService {

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PostLikeRepository postLikeRepository;
    private final BookmarkRepository bookmarkRepository;
    private final SearchLogService searchLogService;
    private final CommentRepository commentRepository;

    public PostCreateResponse write(Long userId, PostCreateRequest request) {

        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new ApiException(MemberErrorCode.MEMBER_NOT_FOUND));;

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ApiException(CategoryErrorCode.CATEGORY_404_NOT_FOUND));

        Post post = Post.builder()
                .member(member)
                .category(category)
                .title(request.title())
                .content(request.content())
                .build();

        Post saved = postRepository.save(post);

        return PostCreateResponse.from(saved);
    }

    @Transactional
    public PostDetailResponse findDetailById(Long postId, Long loginUserId) {
        Post post = postRepository.findByPostIdAndIsDeletedFalse(postId)
                .orElseThrow(() -> new ApiException(PostErrorCode.POST_404_NOT_FOUND));

        post.increaseViewCount();

        boolean liked = loginUserId != null
                && postLikeRepository.existsByMember_UserIdAndPost_PostId(loginUserId, postId);

        boolean bookmarked = loginUserId != null
                && bookmarkRepository.existsByMember_UserIdAndPost_PostId(loginUserId, postId);

        int bookmarkCount = Math.toIntExact(bookmarkRepository.countByPost_PostId(postId));
        applyActualCommentCount(post);

        return PostDetailResponse.from(post, liked, bookmarked, bookmarkCount);
    }

    @Transactional
    public Page<PostListResponse> getPosts(
            Long loginUserId,
            Long categoryId,
            String keyword,
            PostSearchType searchType,
            PostSortType sort,
            int page,
            int size
    ) {
        Sort jpaSort = switch (sort) {
            case VIEWS -> Sort.by(
                    Sort.Order.desc("viewCount"),
                    Sort.Order.desc("createdAt")
            );
            case LIKES -> Sort.by(
                    Sort.Order.desc("likeCount"),
                    Sort.Order.desc("createdAt")
            );
            default -> Sort.by(Sort.Order.desc("createdAt"));
        };

        Pageable pageable = PageRequest.of(page, size, jpaSort);

        boolean hasKeyword = keyword != null && !keyword.isBlank();

        if (loginUserId != null && hasKeyword) {
            searchLogService.createSearchLog(
                    loginUserId,
                    new CreateSearchLogRequest(keyword.trim())
            );
        }

        Page<Post> result;

        if (hasKeyword) {
            String kw = keyword.trim();

            if (searchType == PostSearchType.TITLE) {
                result = (categoryId != null)
                        ? postRepository.findByCategoryCategoryIdAndTitleContainingAndIsDeletedFalse(categoryId, kw, pageable)
                        : postRepository.findByTitleContainingAndIsDeletedFalse(kw, pageable);
            } else if (searchType == PostSearchType.CONTENT) {
                result = (categoryId != null)
                        ? postRepository.findByCategoryCategoryIdAndContentContainingAndIsDeletedFalse(categoryId, kw, pageable)
                        : postRepository.findByContentContainingAndIsDeletedFalse(kw, pageable);
            } else {
                result = (categoryId != null)
                        ? postRepository.searchPosts(categoryId, kw, pageable)
                        : postRepository.searchByKeyword(kw, pageable);
            }
        } else {
            if (categoryId != null) {
                if (!categoryRepository.existsById(categoryId)) {
                    throw new ApiException(CategoryErrorCode.CATEGORY_404_NOT_FOUND);
                }
                result = postRepository.findByCategoryCategoryIdAndIsDeletedFalse(categoryId, pageable);
            } else {
                result = postRepository.findByIsDeletedFalse(pageable);
            }
        }

        return result.map(post -> {
            applyActualCommentCount(post);
            boolean liked = loginUserId != null
                    && postLikeRepository.existsByMember_UserIdAndPost_PostId(loginUserId, post.getPostId());

            boolean bookmarked = loginUserId != null
                    && bookmarkRepository.existsByMember_UserIdAndPost_PostId(loginUserId, post.getPostId());

            return PostListResponse.from(post, liked, bookmarked);
        });
    }

    private void applyActualCommentCount(Post post) {
        int actualCommentCount = Math.toIntExact(commentRepository.countByPostIdAndIsDeletedFalse(post.getPostId()));

        try {
            Field commentCountField = Post.class.getDeclaredField("commentCount");
            commentCountField.setAccessible(true);
            commentCountField.setInt(post, actualCommentCount);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Post commentCount 동기화에 실패했습니다.", e);
        }
    }

    @Transactional
    public void increaseViewCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(PostErrorCode.POST_404_NOT_FOUND));

        post.increaseViewCount();
    }

    @Transactional
    public void increaseCommentCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(PostErrorCode.POST_404_NOT_FOUND));

        post.increaseCommentCount();
    }

    @Transactional
    public void decreaseCommentCount(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(PostErrorCode.POST_404_NOT_FOUND));

        post.decreaseCommentCount();
    }

    public PostUpdateResponse update(Long memberId, Long postId, PostUpdateRequest request) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(PostErrorCode.POST_404_NOT_FOUND));

        if (!post.getMember().getUserId().equals(memberId)) {
            throw new ApiException(PostErrorCode.POST_403_FORBIDDEN);
        }

        if (post.isDeleted()) {
            throw new ApiException(PostErrorCode.POST_401_1_ALREADY_DELETED);
        }

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ApiException(CategoryErrorCode.CATEGORY_404_NOT_FOUND));

        post.update(
                request.title(),
                request.content(),
                category
        );

        return PostUpdateResponse.from(post);
    }

    public PostDeleteResponse delete(Long memberId, Long postId) {

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new ApiException(PostErrorCode.POST_404_NOT_FOUND));

        if (!post.getMember().getUserId().equals(memberId)) {
            throw new ApiException(PostErrorCode.POST_403_FORBIDDEN);
        }

        if (post.isDeleted()) {
            throw new ApiException(PostErrorCode.POST_401_1_ALREADY_DELETED);
        }

        bookmarkRepository.deleteByPost_PostId(postId);
        postLikeRepository.deleteByPost_PostId(postId);

        post.delete();

        return PostDeleteResponse.of(postId);
    }
}