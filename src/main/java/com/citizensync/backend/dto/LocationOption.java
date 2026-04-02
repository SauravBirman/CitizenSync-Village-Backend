package com.citizensync.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LocationOption {
    private Long id;
    private String code;
    private String name;
}
