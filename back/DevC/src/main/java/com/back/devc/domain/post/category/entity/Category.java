package com.back.devc.domain.post.category.entity;

import com.back.devc.domain.post.post.entity.Post;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long categoryId;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    //one to many 처리 완료
    @OneToMany(mappedBy = "category",fetch = FetchType.LAZY)
    private List<Post> posts = new ArrayList<>();

    // 생성자
    public Category(String name) {
        this.name = name;
    }

    // 이름 수정 (필요하면)
    public void update(String name) {
        this.name = name;
    }
}