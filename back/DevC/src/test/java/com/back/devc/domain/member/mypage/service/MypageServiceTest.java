package com.back.devc.domain.member.mypage.service;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkedPostResponse;
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.bookmark.service.BookmarkService;
import com.back.devc.domain.interaction.postLike.dto.LikedPostResponse;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.interaction.postLike.service.PostLikeService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.member.mypage.dto.MyCommentResponse;
import com.back.devc.domain.member.mypage.dto.MyPostResponse;
import com.back.devc.domain.member.mypage.dto.MyProfileResponse;
import com.back.devc.domain.member.mypage.dto.UpdateMyProfileRequest;
import com.back.devc.domain.post.comment.repository.CommentRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import com.back.devc.global.exception.errorcode.MypageErrorCode;
import com.back.devc.global.response.PageResponse;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@DisplayName("MypageService 테스트")
@ExtendWith(MockitoExtension.class)
class MypageServiceTest {

    @InjectMocks
    private MypageService mypageService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PostLikeService postLikeService;

    @Mock
    private BookmarkService bookmarkService;

    private final Long userId = 1L;
    private final Long postId = 10L;

    @Test
    @DisplayName("내 프로필 조회 성공")
    void getMyProfile_success() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "기존닉네임");

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));

        // when
        MyProfileResponse response = mypageService.getMyProfile(userId);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.nickname()).isEqualTo("기존닉네임");

        then(memberRepository).should().findById(userId);
    }

    @Test
    @DisplayName("내 프로필 조회 시 회원이 없으면 예외가 발생한다")
    void getMyProfile_memberNotFound() {
        // given
        given(memberRepository.findById(userId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> mypageService.getMyProfile(userId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage(MypageErrorCode.MYPAGE_404_MEMBER_NOT_FOUND.getCode());
    }

    @Test
    @DisplayName("내 게시글 목록 조회 성공")
    void getMyPosts_success() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "작성자");
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        Post post = createPostWithId(
                postId,
                member,
                "내 게시글",
                5,
                2,
                100,
                createdAt
        );

        Pageable pageable = PageRequest.of(0, 10);

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(postRepository.findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(member, pageable))
                .willReturn(new PageImpl<>(List.of(post), pageable, 1));
        given(postLikeRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
                .willReturn(true);
        given(bookmarkRepository.existsByMember_UserIdAndPost_PostId(userId, postId))
                .willReturn(false);

        // when
        PageResponse<MyPostResponse> response = mypageService.getMyPosts(userId, pageable);

        // then
        assertThat(response.content()).hasSize(1);

        MyPostResponse myPost = response.content().get(0);

        assertThat(myPost.postId()).isEqualTo(postId);
        assertThat(myPost.title()).isEqualTo("내 게시글");
        assertThat(myPost.likeCount()).isEqualTo(5);
        assertThat(myPost.commentCount()).isEqualTo(2);
        assertThat(myPost.viewCount()).isEqualTo(100);
        assertThat(myPost.createdAt()).isEqualTo(createdAt);
        assertThat(myPost.liked()).isTrue();
        assertThat(myPost.bookmarked()).isFalse();

        then(postRepository).should()
                .findAllByMemberAndIsDeletedFalseOrderByCreatedAtDesc(member, pageable);
        then(postLikeRepository).should()
                .existsByMember_UserIdAndPost_PostId(userId, postId);
        then(bookmarkRepository).should()
                .existsByMember_UserIdAndPost_PostId(userId, postId);
    }

    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    void getMyComments_success() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "작성자");
        Pageable pageable = PageRequest.of(0, 10);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 15, 10, 0);

        MyCommentResponse comment = new MyCommentResponse(
                100L,
                postId,
                "게시글 제목",
                "댓글 내용",
                createdAt
        );

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(commentRepository.findMyComments(userId, pageable))
                .willReturn(new PageImpl<>(List.of(comment), pageable, 1));

        // when
        PageResponse<MyCommentResponse> response = mypageService.getMyComments(userId, pageable);

        // then
        assertThat(response.content()).hasSize(1);

        MyCommentResponse myComment = response.content().get(0);

        assertThat(myComment.commentId()).isEqualTo(100L);
        assertThat(myComment.postId()).isEqualTo(postId);
        assertThat(myComment.postTitle()).isEqualTo("게시글 제목");
        assertThat(myComment.content()).isEqualTo("댓글 내용");
        assertThat(myComment.createdAt()).isEqualTo(createdAt);

        then(commentRepository).should().findMyComments(userId, pageable);
    }

    @Test
    @DisplayName("내 좋아요 게시글 목록 조회는 PostLikeService에 위임한다")
    void getMyLikedPosts_success() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "작성자");
        Pageable pageable = PageRequest.of(0, 10);

        LikedPostResponse likedPost = new LikedPostResponse(
                postId,
                "좋아요한 게시글",
                "작성자",
                5,
                2,
                100,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                true,
                false
        );

        PageResponse<LikedPostResponse> pageResponse =
                PageResponse.from(new PageImpl<>(List.of(likedPost), pageable, 1));

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(postLikeService.getLikedPosts(userId, pageable))
                .willReturn(pageResponse);

        // when
        PageResponse<LikedPostResponse> response = mypageService.getMyLikedPosts(userId, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).postId()).isEqualTo(postId);
        assertThat(response.content().get(0).liked()).isTrue();

        then(postLikeService).should().getLikedPosts(userId, pageable);
    }

    @Test
    @DisplayName("내 북마크 게시글 목록 조회는 BookmarkService에 위임한다")
    void getMyBookmarkedPosts_success() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "작성자");
        Pageable pageable = PageRequest.of(0, 10);

        BookmarkedPostResponse bookmarkedPost = new BookmarkedPostResponse(
                postId,
                "북마크한 게시글",
                "작성자",
                100L,
                5,
                2,
                100,
                LocalDateTime.of(2026, 5, 15, 10, 0),
                false,
                true
        );

        PageResponse<BookmarkedPostResponse> pageResponse =
                PageResponse.from(new PageImpl<>(List.of(bookmarkedPost), pageable, 1));

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(bookmarkService.getBookmarkedPosts(userId, pageable))
                .willReturn(pageResponse);

        // when
        PageResponse<BookmarkedPostResponse> response =
                mypageService.getMyBookmarkedPosts(userId, pageable);

        // then
        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).postId()).isEqualTo(postId);
        assertThat(response.content().get(0).bookmarked()).isTrue();

        then(bookmarkService).should().getBookmarkedPosts(userId, pageable);
    }

    @Test
    @DisplayName("내 프로필 수정 성공")
    void updateMyProfile_success() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "기존닉네임");
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("변경닉네임");

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("변경닉네임"))
                .willReturn(false);

        // when
        MyProfileResponse response = mypageService.updateMyProfile(userId, request);

        // then
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@test.com");
        assertThat(response.nickname()).isEqualTo("변경닉네임");
        assertThat(member.getNickname()).isEqualTo("변경닉네임");

        then(memberRepository).should().existsByNickname("변경닉네임");
    }

    @Test
    @DisplayName("내 프로필 수정 시 앞뒤 공백을 제거하고 닉네임을 변경한다")
    void updateMyProfile_trimNickname() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "기존닉네임");
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("  변경닉네임  ");

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("변경닉네임"))
                .willReturn(false);

        // when
        MyProfileResponse response = mypageService.updateMyProfile(userId, request);

        // then
        assertThat(response.nickname()).isEqualTo("변경닉네임");
        assertThat(member.getNickname()).isEqualTo("변경닉네임");
    }

    @Test
    @DisplayName("기존 닉네임과 동일하면 중복 검사를 하지 않고 성공한다")
    void updateMyProfile_sameNickname() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "기존닉네임");
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("기존닉네임");

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));

        // when
        MyProfileResponse response = mypageService.updateMyProfile(userId, request);

        // then
        assertThat(response.nickname()).isEqualTo("기존닉네임");

        then(memberRepository).should(never()).existsByNickname(any(String.class));
    }

    @Test
    @DisplayName("내 프로필 수정 시 이미 사용 중인 닉네임이면 예외가 발생한다")
    void updateMyProfile_duplicateNickname() {
        // given
        Member member = createMemberWithId(userId, "test@test.com", "기존닉네임");
        UpdateMyProfileRequest request = new UpdateMyProfileRequest("중복닉네임");

        given(memberRepository.findById(userId))
                .willReturn(Optional.of(member));
        given(memberRepository.existsByNickname("중복닉네임"))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> mypageService.updateMyProfile(userId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(MypageErrorCode.MYPAGE_409_NICKNAME_ALREADY_EXISTS.getCode());

        assertThat(member.getNickname()).isEqualTo("기존닉네임");
    }

    private Member createMemberWithId(Long userId, String email, String nickname) {
        Member member = Member.createLocalMember(
                email,
                "encodedPassword",
                nickname
        );

        setField(member, "userId", userId);

        return member;
    }

    private Post createPostWithId(
            Long postId,
            Member member,
            String title,
            int likeCount,
            int commentCount,
            int viewCount,
            LocalDateTime createdAt
    ) {
        Post post = Post.builder()
                .member(member)
                .title(title)
                .content("테스트 내용")
                .likeCount(likeCount)
                .commentCount(commentCount)
                .viewCount(viewCount)
                .isDeleted(false)
                .createdAt(createdAt)
                .build();

        setField(post, "postId", postId);

        return post;
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}