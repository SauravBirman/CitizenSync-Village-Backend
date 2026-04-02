package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "announcements")
@Data
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String village;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 4000)
    private String message;

    @Column(nullable = false)
    private String postedBy;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
