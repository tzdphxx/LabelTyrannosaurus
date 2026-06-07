package com.labelhub.modules.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @Size(max = 64) String displayName,
        @Email @Size(max = 255) String email
) {
}
