package com.job.distributed_job_scheduler.controller;

import com.job.distributed_job_scheduler.model.dtos.ApiResponseDto;
import com.job.distributed_job_scheduler.model.dtos.JwtResponseDto;
import com.job.distributed_job_scheduler.model.dtos.LoginRequestDto;
import com.job.distributed_job_scheduler.model.dtos.RegisterRequestDto;
import com.job.distributed_job_scheduler.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/v1/auth/login
     * Đăng nhập và lấy JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponseDto<JwtResponseDto>> login(@RequestBody LoginRequestDto loginRequest) {
        log.info("Login request for email: {}", loginRequest.getEmail());

        try {
            JwtResponseDto response = authService.login(loginRequest);
            return ResponseEntity.ok(ApiResponseDto.success("Login successful", response));
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponseDto.error("AUTH_FAILED", "Invalid email or password"));
        }
    }

    /**
     * POST /api/v1/auth/register
     * Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<JwtResponseDto>> register(@RequestBody RegisterRequestDto registerRequest) {
        log.info("Register request for email: {}", registerRequest.getEmail());

        try {
            JwtResponseDto response = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponseDto.success("Registration successful", response));
        } catch (RuntimeException e) {
            log.error("Registration failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponseDto.error("REGISTER_FAILED", e.getMessage()));
        }
    }
}

