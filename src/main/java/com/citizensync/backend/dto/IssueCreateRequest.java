package com.citizensync.backend.dto;

import lombok.Data;

@Data
public class IssueCreateRequest {
    private String description;
    private String village;
    private String status;
    private String priority;
    private String address;
    private Double lat;
    private Double lng;
}
