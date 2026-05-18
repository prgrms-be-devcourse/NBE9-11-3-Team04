package com.back.devc.global.initData

import com.back.devc.domain.post.category.entity.Category
import com.back.devc.domain.post.category.repository.CategoryRepository
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component

@Component
class CategoryInitData(
    private val categoryRepository: CategoryRepository
) {

    @Bean
    fun init(): ApplicationRunner {
        return ApplicationRunner {
            if (categoryRepository.count() > 0) {
                return@ApplicationRunner
            }

            val names = listOf("tech", "job-market", "trend", "free")

            for (name in names) {
                val category = Category(name)
                categoryRepository.save(category)
            }
        }
    }
}