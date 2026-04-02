package com.citizensync.backend.repository;

import com.citizensync.backend.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserEmailOrderByTimestampDesc(String userEmail);
}
