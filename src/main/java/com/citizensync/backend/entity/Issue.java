package com.citizensync.backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "issues")
@Data
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false)
    private String village;

    private Long stateId;
    private String state;
    private Long districtId;
    private String district;
    private Long subdistrictId;
    private String subdistrict;
    private Long panchayatId;
    private String panchayat;
    private Long villageId;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String priority;

    private String address;
    private Double lat;
    private Double lng;

    private String mediaFileName;
    private String mediaContentType;
    private Long mediaSizeBytes;

    @JsonIgnore
    private String mediaStoragePath;

    @Column(nullable = false)
    private boolean mediaEncrypted = false;

    @Column(nullable = false)
    private String reportedByEmail;

    private String reportedByName;
    private String reportedByAadhaar;

    @Column(nullable = false)
    private int urgentVotes = 0;

    @Column(nullable = false)
    private int notUrgentVotes = 0;

    private String escalatedTo;

    private LocalDateTime solvedAt;
    private Integer satisfiedVotes;
    private Integer unsatisfiedVotes;
    private LocalDateTime satisfactionEvaluationDue;

    @CreationTimestamp
    private LocalDateTime registeredAt;
}
