package com.citizensync.backend.service;

import com.citizensync.backend.dto.AnnouncementRequest;
import com.citizensync.backend.entity.Announcement;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;

    @Transactional
    public Announcement postAnnouncement(AnnouncementRequest request, User currentUser) {
        if (!"Sarpanch".equalsIgnoreCase(currentUser.getRole())) {
            throw new RuntimeException("Only Sarpanch can post announcements");
        }

        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new RuntimeException("Title is required");
        }

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            throw new RuntimeException("Message is required");
        }

        Announcement announcement = new Announcement();
        announcement.setTitle(request.getTitle());
        announcement.setMessage(request.getMessage());
        announcement.setVillage((request.getVillage() == null || request.getVillage().isBlank())
                ? currentUser.getSelectedVillage()
                : request.getVillage());
        announcement.setPostedBy(currentUser.getName());

        return announcementRepository.save(announcement);
    }

    public List<Announcement> getAnnouncements(String village, User currentUser) {
        String resolvedVillage = village;
        if (resolvedVillage == null || resolvedVillage.isBlank()) {
            resolvedVillage = currentUser.getSelectedVillage();
        }

        if (resolvedVillage == null || resolvedVillage.isBlank()) {
            throw new RuntimeException("Village is required");
        }

        return announcementRepository.findByVillageOrderByTimestampDesc(resolvedVillage);
    }
}
