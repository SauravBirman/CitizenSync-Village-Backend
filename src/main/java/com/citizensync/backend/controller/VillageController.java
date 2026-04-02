package com.citizensync.backend.controller;

import com.citizensync.backend.service.VillageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/villages")
@RequiredArgsConstructor
public class VillageController {

    private final VillageService villageService;

    @GetMapping
    public List<String> getVillages() {
        return villageService.getVillages();
    }
}
