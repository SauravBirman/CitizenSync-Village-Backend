package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userEmail;

    private Long issueId;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private boolean read = false;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
