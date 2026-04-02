package com.citizensync.backend.dto;

import lombok.Data;

@Data
public class AnnouncementRequest {
    private String village;
    private String title;
    private String message;
}
