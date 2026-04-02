package com.citizensync.backend.controller;

import com.citizensync.backend.entity.Notification;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.service.CurrentUserService;
import com.citizensync.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;

    @GetMapping("/me")
    public List<Notification> getMyNotifications() {
        User currentUser = currentUserService.getCurrentUser();
        return notificationService.getMyNotifications(currentUser);
    }

    @PatchMapping("/{id}/read")
    public Notification markRead(@PathVariable("id") Long notificationId) {
        User currentUser = currentUserService.getCurrentUser();
        return notificationService.markRead(notificationId, currentUser);
    }
}
