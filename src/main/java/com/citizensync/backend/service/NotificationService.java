package com.citizensync.backend.service;

import com.citizensync.backend.entity.Notification;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getMyNotifications(User user) {
        return notificationRepository.findByUserEmailOrderByTimestampDesc(user.getEmail());
    }

    @Transactional
    public Notification markRead(Long notificationId, User user) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        if (!notification.getUserEmail().equals(user.getEmail())) {
            throw new RuntimeException("You cannot update this notification");
        }

        notification.setRead(true);
        return notificationRepository.save(notification);
    }

    public Notification createNotificationForEmail(String userEmail, Long issueId, String message) {
        Notification notification = new Notification();
        notification.setUserEmail(userEmail);
        notification.setIssueId(issueId);
        notification.setMessage(message);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }
}
