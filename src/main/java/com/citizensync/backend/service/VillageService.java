package com.citizensync.backend.service;

import com.citizensync.backend.repository.LocationVillageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VillageService {

    private final LocationVillageRepository villageRepository;

    public List<String> getVillages() {
        List<String> villages = villageRepository.findAll().stream()
                .map(v -> v.getName())
                .distinct()
                .sorted()
                .toList();

        if (!villages.isEmpty()) {
            return villages;
        }

        return List.of("Village A", "Village B", "Village C", "Village D");
    }
}
