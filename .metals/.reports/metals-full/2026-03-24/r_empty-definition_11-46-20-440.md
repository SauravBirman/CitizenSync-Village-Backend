error id: file:///E:/HackV/backend/backend/src/main/java/com/citizensync/backend/controller/AuthController.java:com/citizensync/backend/dto/RegisterRequest#
file:///E:/HackV/backend/backend/src/main/java/com/citizensync/backend/controller/AuthController.java
empty definition using pc, found symbol in pc: com/citizensync/backend/dto/RegisterRequest#
empty definition using semanticdb
empty definition using fallback
non-local guesses:

offset: 180
uri: file:///E:/HackV/backend/backend/src/main/java/com/citizensync/backend/controller/AuthController.java
text:
```scala
package com.citizensync.backend.controller;

import com.citizensync.backend.dto.LoginRequest;
import com.citizensync.backend.dto.OtpRequest;
import com.citizensync.backend.dto.@@RegisterRequest;
import com.citizensync.backend.dto.UserDTO;
import com.citizensync.backend.entity.User;
import com.citizensync.backend.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
```


#### Short summary: 

empty definition using pc, found symbol in pc: com/citizensync/backend/dto/RegisterRequest#