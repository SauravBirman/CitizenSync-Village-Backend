package com.citizensync.backend.dto;

import lombok.Data;

@Data
public class ProfileRequest {
    private String aadhaar;
    private String role;
    private Long stateId;
    private Long districtId;
    private Long subdistrictId;
    private Long panchayatId;
    private Long villageId;
    private String selectedVillage;
    private String name;
    private String mobile;
    private String email;
    private String address;
}
