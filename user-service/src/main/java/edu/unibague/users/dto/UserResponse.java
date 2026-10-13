package edu.unibague.users.dto;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String username,
        String email,
        String firstName,
        String lastName,
        String phone,
        Instant createdAt,
        Instant updatedAt
) {}
