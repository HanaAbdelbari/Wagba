package com.wagba.users.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyEmailRequest(
        @NotBlank(message = "Code is required")
        @Pattern(regexp = "^\\d{6}$", message = "Code must be 6 digits")
        String code
) {
}