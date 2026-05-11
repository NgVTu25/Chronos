package com.job.distributed_job_scheduler.model.dtos;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequestDto {
    private String email;
    private String password;
    private String fullName;
    private String roleName; // ADMIN, DEVELOPER, VIEWER
}

