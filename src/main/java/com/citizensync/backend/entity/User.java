package com.citizensync.backend.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String aadhar;
    private String phone;
    private Long stateId;
    private String state;
    private Long districtId;
    private String district;
    private Long subdistrictId;
    private String subdistrict;
    private Long panchayatId;
    private String panchayat;
    private Long villageId;
    private String village;
    private String selectedVillage;
    private String role;
    private String address;

    private String password;

    private boolean isVerified;

    private String otp;
    private LocalDateTime otpExpiry;
}