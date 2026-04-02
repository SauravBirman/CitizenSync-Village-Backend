package com.citizensync.backend.dto;

import com.citizensync.backend.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private Long id;
    private String aadhaar;
    private String role;
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
    private String name;
    private String mobile;
    private String email;
    private String address;

    public static ProfileResponse fromUser(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .aadhaar(user.getAadhar())
                .role(user.getRole())
                .stateId(user.getStateId())
                .state(user.getState())
                .districtId(user.getDistrictId())
                .district(user.getDistrict())
                .subdistrictId(user.getSubdistrictId())
                .subdistrict(user.getSubdistrict())
                .panchayatId(user.getPanchayatId())
                .panchayat(user.getPanchayat())
                .villageId(user.getVillageId())
                .village(user.getVillage())
                .selectedVillage(user.getSelectedVillage())
                .name(user.getName())
                .mobile(user.getPhone())
                .email(user.getEmail())
                .address(user.getAddress())
                .build();
    }
}
