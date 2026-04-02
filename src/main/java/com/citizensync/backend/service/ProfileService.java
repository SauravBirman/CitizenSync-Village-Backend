package com.citizensync.backend.service;

import com.citizensync.backend.dto.ProfileRequest;
import com.citizensync.backend.dto.ProfileResponse;
import com.citizensync.backend.entity.LocationDistrict;
import com.citizensync.backend.entity.LocationPanchayat;
import com.citizensync.backend.entity.LocationState;
import com.citizensync.backend.entity.LocationSubdistrict;
import com.citizensync.backend.entity.LocationVillage;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final LocationService locationService;

    @Transactional
    public ProfileResponse saveOrUpdateProfile(ProfileRequest request) {
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            throw new RuntimeException("Email is required");
        }

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        validateRoleRules(request, user);

        user.setName(request.getName());
        user.setAadhar(request.getAadhaar());
        user.setPhone(request.getMobile());
        user.setAddress(request.getAddress());
        user.setRole(request.getRole());

        applyLocationSelection(request, user);

        User saved = userRepository.save(user);
        return ProfileResponse.fromUser(saved);
    }

    public ProfileResponse getMyProfile(User currentUser) {
        User user = userRepository.findByEmail(currentUser.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ProfileResponse.fromUser(user);
    }

    public ProfileResponse getByAadhaar(String aadhaar) {
        User user = userRepository.findByAadhar(aadhaar)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return ProfileResponse.fromUser(user);
    }

    public ProfileResponse getByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Profile not found"));
        return ProfileResponse.fromUser(user);
    }

    private void validateRoleRules(ProfileRequest request, User user) {
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new RuntimeException("Role is required");
        }

        if (request.getAadhaar() == null || !request.getAadhaar().matches("^\\d{12}$")) {
            throw new RuntimeException("Aadhaar must be a 12-digit number");
        }

        if (request.getStateId() == null || request.getDistrictId() == null || request.getSubdistrictId() == null) {
            throw new RuntimeException("State, district and subdistrict are required");
        }

        if (request.getRole().equalsIgnoreCase("Sarpanch")) {
            if (request.getPanchayatId() == null) {
                throw new RuntimeException("Panchayat is required for Sarpanch registration");
            }
            boolean panchayatHasSarpanch = userRepository.existsByRoleAndPanchayatId("Sarpanch", request.getPanchayatId());
            if (panchayatHasSarpanch && !"Sarpanch".equalsIgnoreCase(user.getRole())) {
                throw new RuntimeException("This panchayat already has a Sarpanch");
            }
        }

        if (request.getRole().equalsIgnoreCase("Villager")) {
            if (request.getPanchayatId() == null || request.getVillageId() == null) {
                throw new RuntimeException("Panchayat and village are required for Villager registration");
            }
        }

        if (request.getRole().equalsIgnoreCase("Tehsil Officer")) {
            boolean hasTehsilOfficerForSubdistrict = userRepository.existsByRoleAndSubdistrictId("Tehsil Officer", request.getSubdistrictId());
            if (hasTehsilOfficerForSubdistrict && !"Tehsil Officer".equalsIgnoreCase(user.getRole())) {
                throw new RuntimeException("A Tehsil Officer already exists for this subdistrict");
            }
        }
    }

    private void applyLocationSelection(ProfileRequest request, User user) {
        LocationState state = locationService.getState(request.getStateId());
        LocationDistrict district = locationService.getDistrict(request.getDistrictId());
        LocationSubdistrict subdistrict = locationService.getSubdistrict(request.getSubdistrictId());

        if (!district.getState().getId().equals(state.getId())) {
            throw new RuntimeException("District does not belong to selected state");
        }

        if (!subdistrict.getDistrict().getId().equals(district.getId())) {
            throw new RuntimeException("Subdistrict does not belong to selected district");
        }

        user.setStateId(state.getId());
        user.setState(state.getName());
        user.setDistrictId(district.getId());
        user.setDistrict(district.getName());
        user.setSubdistrictId(subdistrict.getId());
        user.setSubdistrict(subdistrict.getName());

        user.setPanchayatId(null);
        user.setPanchayat(null);
        user.setVillageId(null);
        user.setVillage(null);
        user.setSelectedVillage(subdistrict.getName());

        if ("Sarpanch".equalsIgnoreCase(request.getRole()) || "Villager".equalsIgnoreCase(request.getRole())) {
            LocationPanchayat panchayat = locationService.getPanchayat(request.getPanchayatId());
            if (!panchayat.getSubdistrict().getId().equals(subdistrict.getId())) {
                throw new RuntimeException("Panchayat does not belong to selected subdistrict");
            }

            user.setPanchayatId(panchayat.getId());
            user.setPanchayat(panchayat.getName());
            user.setSelectedVillage(panchayat.getName());
        }

        if ("Villager".equalsIgnoreCase(request.getRole())) {
            LocationVillage village = locationService.getVillage(request.getVillageId());
            if (!village.getPanchayat().getId().equals(user.getPanchayatId())) {
                throw new RuntimeException("Village does not belong to selected panchayat");
            }

            user.setVillageId(village.getId());
            user.setVillage(village.getName());
            user.setSelectedVillage(village.getName());
        }
    }
}
