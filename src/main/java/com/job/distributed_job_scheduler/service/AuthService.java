package com.job.distributed_job_scheduler.service;

import com.job.distributed_job_scheduler.model.Role;
import com.job.distributed_job_scheduler.model.User;
import com.job.distributed_job_scheduler.model.dtos.LoginRequestDto;
import com.job.distributed_job_scheduler.model.dtos.RegisterRequestDto;
import com.job.distributed_job_scheduler.model.dtos.JwtResponseDto;
import com.job.distributed_job_scheduler.repository.RoleRepository;
import com.job.distributed_job_scheduler.repository.UserRepository;
import com.job.distributed_job_scheduler.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    /**
     * Xác thực người dùng và trả về JWT token
     */
    @Transactional
    public JwtResponseDto login(LoginRequestDto loginRequest) throws AuthenticationException {
        log.info("Login attempt for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        String jwt = tokenProvider.generateTokenFromAuthentication(authentication);

        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        log.info("Login successful for user: {}", loginRequest.getEmail());

        return JwtResponseDto.builder()
                .accessToken(jwt)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getExpirationTimeMs() / 1000) // Convert to seconds
                .email(user.getEmail())
                .role(user.getRole().getName())
                .build();
    }

    /**
     * Đăng ký người dùng mới
     */
    @Transactional
    public JwtResponseDto register(RegisterRequestDto registerRequest) {
        log.info("Register attempt for email: {}", registerRequest.getEmail());

        // Kiểm tra email đã tồn tại chưa
        if (userRepository.existsByEmail(registerRequest.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Lấy role, mặc định là DEVELOPER
        String roleName = registerRequest.getRoleName() != null ?
                registerRequest.getRoleName() : "DEVELOPER";

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new RuntimeException("Role not found: " + roleName));

        // Tạo user mới
        User newUser = User.builder()
                .email(registerRequest.getEmail())
                .passwordHash(passwordEncoder.encode(registerRequest.getPassword()))
                .fullName(registerRequest.getFullName())
                .role(role)
                .status("ACTIVE")
                .build();

        userRepository.save(newUser);
        log.info("User registered successfully: {}", registerRequest.getEmail());

        // Auto login sau khi register
        return login(new LoginRequestDto(registerRequest.getEmail(), registerRequest.getPassword()));
    }

    /**
     * Lấy thông tin user từ token
     */
    public String getUserFromToken(String token) {
        return tokenProvider.getEmailFromToken(token);
    }

    /**
     * Validate token
     */
    public boolean validateToken(String token) {
        return tokenProvider.validateToken(token);
    }
}

