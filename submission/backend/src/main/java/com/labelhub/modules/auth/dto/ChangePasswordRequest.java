package com.labelhub.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(min = 1) String oldPassword,
        @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
