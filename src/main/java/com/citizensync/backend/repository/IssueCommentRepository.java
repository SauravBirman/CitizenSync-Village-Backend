package com.citizensync.backend.repository;

import com.citizensync.backend.entity.Issue;
import com.citizensync.backend.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {
    List<IssueComment> findByIssueOrderByTimestampAsc(Issue issue);
    long countByIssue(Issue issue);
}
