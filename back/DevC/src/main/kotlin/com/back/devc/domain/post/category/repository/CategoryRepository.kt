package com.back.devc.domain.post.category.repository

import com.back.devc.domain.post.category.entity.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long>