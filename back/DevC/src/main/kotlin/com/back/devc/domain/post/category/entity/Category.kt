package com.back.devc.domain.post.category.entity

import com.back.devc.domain.post.post.entity.Post
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany

@Entity
class Category(

    @Column(nullable = false, unique = true, length = 50)
    private var name: String
) {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val categoryId: Long? = null

    @OneToMany(mappedBy = "category", fetch = FetchType.LAZY)
    private val posts: MutableList<Post> = mutableListOf()

    // JPA 기본 생성자용 (필수)
    protected constructor() : this(name = "")

    fun update(name: String) {
        this.name = name
    }

    fun getName(): String = name

    fun getPosts(): List<Post> = posts.toList()
}