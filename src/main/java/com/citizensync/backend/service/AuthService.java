package com.citizensync.backend.service;

import com.citizensync.backend.dto.LoginRequest;
import com.citizensync.backend.dto.OtpRequest;
import com.citizensync.backend.dto.RegisterRequest;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.repository.UserRepository;
import com.citizensync.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

     public void register(RegisterRequest req) {

        if (userRepo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        String otp = String.valueOf(new Random().nextInt(900000) + 100000);

        User user = new User();
        user.setEmail(req.getEmail());
        user.setName(req.getName());
        user.setAadhar(req.getAadhar());
        user.setPhone(req.getPhone());
        user.setVillage(req.getVillage());
        user.setAddress(req.getAddress());
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(5));
        user.setVerified(false);

        userRepo.save(user);

        emailService.sendOtp(req.getEmail(), otp);
    }

    public void sendOtp(RegisterRequest req) {
        register(req);
    }

    public void verifyOtp(OtpRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!user.getOtp().equals(req.getOtp())) {
            throw new RuntimeException("Invalid OTP");
        }

        if (user.getOtpExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired");
        }

        user.setVerified(true);
        userRepo.save(user);
    }

    public void setPassword(String email, String password) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setPassword(passwordEncoder.encode(password));
        userRepo.save(user);
    }

    public String login(LoginRequest req) {
        User user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtUtil.generateToken(req.getEmail());
    }
}