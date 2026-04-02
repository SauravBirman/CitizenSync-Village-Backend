package com.citizensync.backend.repository;

import com.citizensync.backend.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findByVillageOrderByRegisteredAtDesc(String village);
    List<Issue> findByVillageAndStatusOrderByRegisteredAtDesc(String village, String status);
    List<Issue> findByStatusOrderByRegisteredAtDesc(String status);
}
