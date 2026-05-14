package com.back.devc.domain.post.category.repository;

import com.back.devc.domain.post.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category,Long> {

}
