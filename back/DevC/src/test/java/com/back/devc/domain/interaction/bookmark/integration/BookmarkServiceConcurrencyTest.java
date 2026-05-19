package com.back.devc.domain.interaction.bookmark.integration;

import com.back.devc.domain.interaction.bookmark.dto.BookmarkCreateCommand;
import com.back.devc.domain.interaction.bookmark.dto.BookmarkDeleteCommand;
import com.back.devc.domain.interaction.bookmark.repository.BookmarkRepository;
import com.back.devc.domain.interaction.bookmark.service.BookmarkService;
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
class BookmarkServiceConcurrencyTest {

    @Autowired
    private BookmarkService bookmarkService;

    @Autowired
    private BookmarkRepository bookmarkRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    @DisplayName("동시에 북마크를 여러 번 요청해도 북마크는 1개만 생성된다")
    void createBookmark_concurrently() throws Exception {
        // given
        Member member = memberRepository.save(
                createMember("bookmark@test.com", "bookmarkUser")
        );

        Category category = categoryRepository.save(
                createCategory("북마크 테스트 카테고리")
        );

        Post post = postRepository.save(
                createPost(member, category)
        );

        BookmarkCreateCommand command = new BookmarkCreateCommand(
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

                    bookmarkService.createBookmark(command);
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
        long bookmarkRowCount = bookmarkRepository.countByPost_PostId(post.getPostId());

        assertThat(bookmarkRowCount).isEqualTo(1);
    }

    @Test
    @DisplayName("동시에 북마크 취소를 여러 번 요청해도 북마크는 0개가 된다")
    void cancelBookmark_concurrently() throws Exception {
        // given
        Member member = memberRepository.save(
                createMember("cancel-bookmark@test.com", "cancelBookmarkUser")
        );

        Category category = categoryRepository.save(
                createCategory("북마크 취소 테스트 카테고리")
        );

        Post post = postRepository.save(
                createPost(member, category)
        );

        BookmarkCreateCommand createCommand = new BookmarkCreateCommand(
                member.getUserId(),
                post.getPostId()
        );

        BookmarkDeleteCommand deleteCommand = new BookmarkDeleteCommand(
                member.getUserId(),
                post.getPostId()
        );

        bookmarkService.createBookmark(createCommand);

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

                    bookmarkService.cancelBookmark(deleteCommand);
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
        long bookmarkRowCount = bookmarkRepository.countByPost_PostId(post.getPostId());

        assertThat(bookmarkRowCount).isEqualTo(0);
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