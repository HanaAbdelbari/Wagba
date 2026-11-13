package com.wagba.users.admin.dto;

import com.wagba.users.user.UserStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateStatusRequest(
        @NotNull(message = "Status is required") UserStatus status,
        @NotBlank(message = "Reason is required") @Size(max = 500) String reason
) {
}