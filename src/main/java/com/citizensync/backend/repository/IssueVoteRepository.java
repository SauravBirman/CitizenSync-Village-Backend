package com.citizensync.backend.repository;

import com.citizensync.backend.entity.Issue;
import com.citizensync.backend.entity.IssueVote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IssueVoteRepository extends JpaRepository<IssueVote, Long> {
    Optional<IssueVote> findByIssueAndVoterEmailAndVoteCategory(Issue issue, String voterEmail, String voteCategory);
    long countByIssueAndVoteCategoryAndVoteType(Issue issue, String voteCategory, String voteType);
}
