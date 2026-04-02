package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_votes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_issue_vote_user_type", columnNames = {"issue_id", "voterEmail", "voteCategory"})
})
@Data
public class IssueVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @Column(nullable = false)
    private String voterEmail;

    @Column(nullable = false)
    private String voteCategory;

    @Column(nullable = false)
    private String voteType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
