package com.citizensync.backend.controller;

import com.citizensync.backend.dto.LocationOption;
import com.citizensync.backend.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/states")
    public List<LocationOption> getStates() {
        return locationService.getStates();
    }

    @GetMapping("/districts")
    public List<LocationOption> getDistricts(@RequestParam Long stateId) {
        return locationService.getDistricts(stateId);
    }

    @GetMapping("/subdistricts")
    public List<LocationOption> getSubdistricts(@RequestParam Long districtId) {
        return locationService.getSubdistricts(districtId);
    }

    @GetMapping("/panchayats")
    public List<LocationOption> getPanchayats(@RequestParam Long subdistrictId) {
        return locationService.getPanchayats(subdistrictId);
    }

    @GetMapping("/villages")
    public List<LocationOption> getVillages(@RequestParam Long panchayatId) {
        return locationService.getVillages(panchayatId);
    }
}
