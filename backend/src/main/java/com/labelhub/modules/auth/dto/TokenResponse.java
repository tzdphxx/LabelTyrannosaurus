package com.labelhub.modules.auth.dto;

public record TokenResponse(String accessToken, String refreshToken, Integer tokenVersion) {
}
