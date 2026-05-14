package com.back.devc.domain.interaction.notification.repository;

import com.back.devc.domain.interaction.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    boolean existsByUserIdAndActorUserIdAndPostIdAndType(Long userId, Long actorUserId, Long postId, String type);
}