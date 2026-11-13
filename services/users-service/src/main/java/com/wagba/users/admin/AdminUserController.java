package com.wagba.users.admin;

import com.wagba.users.admin.dto.AdminUserResponse;
import com.wagba.users.admin.dto.UpdateRoleRequest;
import com.wagba.users.admin.dto.UpdateStatusRequest;
import com.wagba.users.common.PageResponse;
import com.wagba.users.user.Role;
import com.wagba.users.user.UserStatus;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService service;

    @GetMapping
    public PageResponse<AdminUserResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(search, role, status, page, size);
    }

    @GetMapping("/{id}")
    public AdminUserResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    @PatchMapping("/{id}/status")
    public AdminUserResponse changeStatus(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @Valid @RequestBody UpdateStatusRequest request) {
        return service.changeStatus(UUID.fromString(jwt.getSubject()), id, request);
    }

    @PutMapping("/{id}/role")
    public AdminUserResponse changeRole(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                        @Valid @RequestBody UpdateRoleRequest request) {
        return service.changeRole(UUID.fromString(jwt.getSubject()), id, request);
    }
}