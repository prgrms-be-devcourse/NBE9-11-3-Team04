package com.back.devc.domain.interaction.notification.repository

import com.back.devc.domain.interaction.notification.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository : JpaRepository<Notification, Long> {
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Notification>

    @Query(
        """
    SELECT n
    FROM Notification n
    WHERE n.userId = :userId
      AND (
          n.postId IS NULL
          OR EXISTS (
              SELECT p.postId
              FROM Post p
              WHERE p.postId = n.postId
                AND p.isDeleted = false
          )
      )
    ORDER BY n.createdAt DESC
    """
    )
    fun findAvailableByUserIdOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        pageable: Pageable,
    ): Page<Notification>

    @Query(
        """
    SELECT n
    FROM Notification n
    WHERE n.userId = :userId
      AND n.type IN :types
      AND (
          n.postId IS NULL
          OR EXISTS (
              SELECT p.postId
              FROM Post p
              WHERE p.postId = n.postId
                AND p.isDeleted = false
          )
      )
    ORDER BY n.createdAt DESC
    """
    )
    fun findAvailableByUserIdAndTypeInOrderByCreatedAtDesc(
        @Param("userId") userId: Long,
        @Param("types") types: List<String>,
        pageable: Pageable,
    ): Page<Notification>

    fun existsByUserIdAndActorUserIdAndPostIdAndType(
        userId: Long,
        actorUserId: Long,
        postId: Long,
        type: String,
    ): Boolean
}