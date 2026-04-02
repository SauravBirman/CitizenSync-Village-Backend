package com.citizensync.backend.repository;

import com.citizensync.backend.entity.Announcement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    List<Announcement> findByVillageOrderByTimestampDesc(String village);
}
