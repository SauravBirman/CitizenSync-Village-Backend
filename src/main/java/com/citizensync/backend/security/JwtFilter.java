package com.citizensync.backend.security;

import com.citizensync.backend.entity.User;
import com.citizensync.backend.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import io.jsonwebtoken.ExpiredJwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class JwtFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtFilter.class);

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepo;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);
            try {
                String email = jwtUtil.extractEmail(token);

                User user = userRepo.findByEmail(email).orElse(null);

                if (user != null) {
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    user, null, new ArrayList<>());

                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (ExpiredJwtException ex) {
                logger.warn("JWT token expired for request: {}", request.getRequestURI());
                // Token expired - continue without authentication
            } catch (Exception ex) {
                logger.warn("Failed to parse JWT token: {}", ex.getMessage());
                // Invalid token - continue without authentication
            }
        }

        filterChain.doFilter(request, response);
    }
}