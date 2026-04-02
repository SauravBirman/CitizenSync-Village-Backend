package com.citizensync.backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String aadhar;
    private String phone;
    private String village;
    private String address;
}