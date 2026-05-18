package com.back.devc.domain.interaction.postlike.service;

import com.back.devc.domain.interaction.postLike.dto.PostLikeCommand;
import com.back.devc.domain.interaction.postLike.repository.PostLikeRepository;
import com.back.devc.domain.interaction.postLike.service.PostLikeService;
import com.back.devc.domain.member.member.entity.Member;
import com.back.devc.domain.member.member.repository.MemberRepository;
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import com.back.devc.domain.post.post.entity.Post;
import com.back.devc.domain.post.post.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostLikeServiceConcurrencyTest {

    @Autowired
    private PostLikeService postLikeService;

    @Autowired
    private PostLikeRepository postLikeRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("동시에 좋아요를 여러 번 요청해도 좋아요는 1개만 생성되고 likeCount는 1만 증가한다")
    void createLike_concurrently() throws Exception {
        // given
        Member member = memberRepository.save(createMember("like@test.com", "likeUser"));
        Category category = categoryRepository.save(createCategory("좋아요 테스트 카테고리"));
        Post post = postRepository.save(createPost(member, category));

        PostLikeCommand command = new PostLikeCommand(
                member.getUserId(),
                post.getPostId()
        );

        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    postLikeService.createLike(command);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        executorService.shutdown();

        // then
        long likeRowCount = postLikeRepository.countByPost_PostId(post.getPostId());
        int likeCount = postRepository.findLikeCountByPostId(post.getPostId());

        assertThat(likeRowCount).isEqualTo(1);
        assertThat(likeCount).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 좋아요 취소를 여러 번 요청해도 좋아요는 0개가 되고 likeCount는 0 미만으로 내려가지 않는다")
    void cancelLike_concurrently() throws Exception {
        // given
        Member member = memberRepository.save(createMember("cancel-like@test.com", "cancelLikeUser"));
        Category category = categoryRepository.save(createCategory("좋아요 취소 테스트 카테고리"));
        Post post = postRepository.save(createPost(member, category));

        PostLikeCommand command = new PostLikeCommand(
                member.getUserId(),
                post.getPostId()
        );

        postLikeService.createLike(command);

        int threadCount = 100;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        // when
        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    postLikeService.cancelLike(command);
                } catch (Exception ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean ready = readyLatch.await(10, TimeUnit.SECONDS);
        assertThat(ready).isTrue();

        startLatch.countDown();

        boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
        assertThat(finished).isTrue();

        executorService.shutdown();

        // then
        long likeRowCount = postLikeRepository.countByPost_PostId(post.getPostId());
        int likeCount = postRepository.findLikeCountByPostId(post.getPostId());

        assertThat(likeRowCount).isEqualTo(0);
        assertThat(likeCount).isEqualTo(0);
    }

    private Member createMember(String email, String nickname) {
        return Member.createLocalMember(
                email,
                "passwordHash",
                nickname
        );
    }

    private Category createCategory(String name) {
        return new Category(name);
    }

    private Post createPost(Member member, Category category) {
        return Post.create(
                member,
                category,
                "테스트 제목",
                "테스트 내용"
        );
    }
}