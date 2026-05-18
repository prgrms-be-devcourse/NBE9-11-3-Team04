package com.back.devc.global.entity

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity {

    @field:Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null
        protected set

    @field:Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null
        protected set

    @PrePersist
    protected fun prePersist() {
        val now = LocalDateTime.now()
        createdAt = now
        updatedAt = now
    }

    @PreUpdate
    protected fun preUpdate() {
        updatedAt = LocalDateTime.now()
    }
}