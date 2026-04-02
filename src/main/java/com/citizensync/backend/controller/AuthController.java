package com.citizensync.backend.controller;

import com.citizensync.backend.dto.LoginRequest;
import com.citizensync.backend.dto.OtpRequest;
import com.citizensync.backend.dto.RegisterRequest;
import com.citizensync.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest req) {
        authService.register(req);
        return "Registration initiated. OTP sent";
    }

    @PostMapping("/send-otp")
    public String sendOtp(@RequestBody RegisterRequest req) {
        authService.sendOtp(req);
        return "OTP sent";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestBody OtpRequest req) {
        authService.verifyOtp(req);
        return "Verified";
    }

    @PostMapping("/set-password")
    public String setPassword(@RequestParam String email,
                              @RequestParam String password) {
        authService.setPassword(email, password);
        return "Password set";
    }

    @PostMapping("/login")
    public String login(@RequestBody LoginRequest req) {
        return authService.login(req);
    }
}