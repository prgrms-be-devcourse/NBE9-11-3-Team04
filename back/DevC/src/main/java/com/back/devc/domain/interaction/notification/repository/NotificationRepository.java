package com.back.devc.domain.interaction.notification.repository;

import com.back.devc.domain.interaction.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("""
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
            """)
    Page<Notification> findAvailableByUserIdOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            Pageable pageable
    );

    @Query("""
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
            """)
    Page<Notification> findAvailableByUserIdAndTypeInOrderByCreatedAtDesc(
            @Param("userId") Long userId,
            @Param("types") List<String> types,
            Pageable pageable
    );

    boolean existsByUserIdAndActorUserIdAndPostIdAndType(
            Long userId,
            Long actorUserId,
            Long postId,
            String type
    );
}