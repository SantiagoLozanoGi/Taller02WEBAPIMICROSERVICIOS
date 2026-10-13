package edu.unibague.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String tokenType,
        String accessToken,
        Instant expiresAt,
        UUID userId,
        String username,
        String role
) {}
