package com.back.devc.global.initData;

// 테스트를 수행하기 위해서 일단 카테고리 임시 추가
import com.back.devc.domain.post.category.entity.Category;
import com.back.devc.domain.post.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CategoryInitData {

    private final CategoryRepository categoryRepository;

    @Bean
    public ApplicationRunner init() {
        return args -> {

            if (categoryRepository.count() > 0) return;

            List<String> names = List.of("tech", "job-market", "trend","free");

            for (String name : names) {
                Category c = new Category(name);
                categoryRepository.save(c);
            }
        };
    }
}