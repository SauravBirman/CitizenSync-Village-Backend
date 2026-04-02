package com.citizensync.backend.security;

import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    private final String SECRET = "supersecretkeysupersecretkeysupersecretkey123"; // min 32 chars

    private SecretKey getSignKey() {
        return Keys.hmacShaKeyFor(SECRET.getBytes());
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .setSubject(email)   // ✅ correct
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 2592000000L))  // 30 days
                .signWith(getSignKey())
                .compact();
    }

    public String extractEmail(String token) {

        Claims claims = Jwts.parserBuilder()
                .setSigningKey(getSignKey())
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.getSubject();
    }
}
