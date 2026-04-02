package com.citizensync.backend.controller;

import com.citizensync.backend.dto.ProfileRequest;
import com.citizensync.backend.dto.ProfileResponse;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.service.CurrentUserService;
import com.citizensync.backend.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    @PostMapping
    public ProfileResponse saveProfile(@RequestBody ProfileRequest request) {
        return profileService.saveOrUpdateProfile(request);
    }

    @GetMapping("/me")
    public ProfileResponse getMyProfile() {
        User currentUser = currentUserService.getCurrentUser();
        return profileService.getMyProfile(currentUser);
    }

    @GetMapping("/by-aadhaar/{aadhaar}")
    public ProfileResponse getByAadhaar(@PathVariable String aadhaar) {
        return profileService.getByAadhaar(aadhaar);
    }

    @GetMapping("/by-email")
    public ProfileResponse getByEmail(@RequestParam String email) {
        return profileService.getByEmail(email);
    }
}
