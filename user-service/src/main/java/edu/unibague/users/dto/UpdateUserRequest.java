package edu.unibague.users.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 80) String lastName,
        @Pattern(regexp = "^$|^[0-9+() -]{7,30}$", message = "Formato de teléfono inválido") String phone
) {}
