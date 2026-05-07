package com.hospital.controller;

import com.hospital.dto.LoginRequest;
import com.hospital.dto.LoginResponse;
import com.hospital.entity.User;
import com.hospital.repository.UserRepository;
import com.hospital.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication controller – shared by all departments.
 *
 * <p>POST /api/auth/login → returns a JWT bearer token.
 * Include the token as {@code Authorization: Bearer <token>} on all
 * subsequent requests to any department endpoint.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final UserDetailsService    userDetailsService;
    private final JwtService            jwtService;
    private final UserRepository        userRepository;

    public AuthController(AuthenticationManager authManager,
                          UserDetailsService userDetailsService,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.authManager = authManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    /**
     * Login endpoint.
     *
     * <p>Example request body:</p>
     * <pre>
     * { "email": "sarah.chen@hospital.com", "password": "Doctor@123" }
     * </pre>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {

        // Authenticate credentials (throws BadCredentialsException on failure)
        authManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        // Load user and generate token
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow();
        String token = jwtService.generateToken(userDetails, user.getRole().name());

        return ResponseEntity.ok(new LoginResponse(
            token,
            user.getEmail(),
            user.getRole().name(),
            user.getFullName()
        ));
    }
}
