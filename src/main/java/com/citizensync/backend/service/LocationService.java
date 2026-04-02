package com.citizensync.backend.service;

import com.citizensync.backend.dto.LocationOption;
import com.citizensync.backend.entity.*;
import com.citizensync.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationStateRepository stateRepository;
    private final LocationDistrictRepository districtRepository;
    private final LocationSubdistrictRepository subdistrictRepository;
    private final LocationPanchayatRepository panchayatRepository;
    private final LocationVillageRepository villageRepository;

    public List<LocationOption> getStates() {
        return stateRepository.findAllByOrderByNameAsc().stream()
                .map(s -> new LocationOption(s.getId(), s.getCode(), s.getName()))
                .toList();
    }

    public List<LocationOption> getDistricts(Long stateId) {
        return districtRepository.findByStateIdOrderByNameAsc(stateId).stream()
                .map(d -> new LocationOption(d.getId(), d.getCode(), d.getName()))
                .toList();
    }

    public List<LocationOption> getSubdistricts(Long districtId) {
        return subdistrictRepository.findByDistrictIdOrderByNameAsc(districtId).stream()
                .map(sd -> new LocationOption(sd.getId(), sd.getCode(), sd.getName()))
                .toList();
    }

    public List<LocationOption> getPanchayats(Long subdistrictId) {
        return panchayatRepository.findBySubdistrictIdOrderByNameAsc(subdistrictId).stream()
                .map(p -> new LocationOption(p.getId(), p.getCode(), p.getName()))
                .toList();
    }

    public List<LocationOption> getVillages(Long panchayatId) {
        return villageRepository.findByPanchayatIdOrderByNameAsc(panchayatId).stream()
                .map(v -> new LocationOption(v.getId(), v.getCode(), v.getName()))
                .toList();
    }

    public LocationState getState(Long id) {
        return stateRepository.findById(id).orElseThrow(() -> new RuntimeException("State not found"));
    }

    public LocationDistrict getDistrict(Long id) {
        return districtRepository.findById(id).orElseThrow(() -> new RuntimeException("District not found"));
    }

    public LocationSubdistrict getSubdistrict(Long id) {
        return subdistrictRepository.findById(id).orElseThrow(() -> new RuntimeException("Subdistrict not found"));
    }

    public LocationPanchayat getPanchayat(Long id) {
        return panchayatRepository.findById(id).orElseThrow(() -> new RuntimeException("Panchayat not found"));
    }

    public LocationVillage getVillage(Long id) {
        return villageRepository.findById(id).orElseThrow(() -> new RuntimeException("Village not found"));
    }
}
