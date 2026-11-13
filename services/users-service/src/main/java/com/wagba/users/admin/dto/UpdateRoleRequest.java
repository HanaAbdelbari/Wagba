package com.wagba.users.admin.dto;

import com.wagba.users.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateRoleRequest(
        @NotNull(message = "Role is required") Role role,
        @NotBlank(message = "Reason is required") @Size(max = 500) String reason
) {
}