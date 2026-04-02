package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "issue_comments")
@Data
public class IssueComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private String userEmail;

    private String userName;
    private String userAadhaar;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
