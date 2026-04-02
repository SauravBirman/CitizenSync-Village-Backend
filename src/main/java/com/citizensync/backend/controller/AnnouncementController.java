package com.citizensync.backend.controller;

import com.citizensync.backend.dto.AnnouncementRequest;
import com.citizensync.backend.entity.Announcement;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.service.AnnouncementService;
import com.citizensync.backend.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public Announcement postAnnouncement(@RequestBody AnnouncementRequest request) {
        User currentUser = currentUserService.getCurrentUser();
        return announcementService.postAnnouncement(request, currentUser);
    }

    @GetMapping
    public List<Announcement> getAnnouncements(@RequestParam(required = false) String village) {
        User currentUser = currentUserService.getCurrentUser();
        return announcementService.getAnnouncements(village, currentUser);
    }
}
