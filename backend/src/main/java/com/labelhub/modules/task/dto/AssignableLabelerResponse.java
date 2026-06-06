package com.labelhub.modules.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Owner assignable labeler option")
public record AssignableLabelerResponse(
        @Schema(description = "Labeler user ID", example = "20")
        Long labelerId,
        @Schema(description = "Username", example = "labeler-a")
        String username,
        @Schema(description = "Email", example = "labeler-a@example.com")
        String email,
        @Schema(description = "Display name", example = "Labeler A")
        String displayName,
        @Schema(description = "Avatar URL", example = "https://cdn.example.com/a.png")
        String avatarUrl,
        @Schema(description = "Whether the account is enabled", example = "true")
        Boolean enabled,
        @Schema(description = "Whether login is enabled", example = "true")
        Boolean loginEnabled
) {
}
