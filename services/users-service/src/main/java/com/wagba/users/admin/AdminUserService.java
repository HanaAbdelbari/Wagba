package com.wagba.users.admin;

import com.wagba.users.admin.dto.AdminUserResponse;
import com.wagba.users.admin.dto.UpdateRoleRequest;
import com.wagba.users.admin.dto.UpdateStatusRequest;
import com.wagba.users.audit.AuditService;
import com.wagba.users.auth.RefreshTokenService;
import com.wagba.users.common.AdminOperationNotAllowedException;
import com.wagba.users.common.PageResponse;
import com.wagba.users.common.UserNotFoundException;
import com.wagba.users.user.Role;
import com.wagba.users.user.User;
import com.wagba.users.user.UserRepository;
import com.wagba.users.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenService refreshTokenService;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> search(String search, Role role, UserStatus status,
                                                  int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = userRepository.findAll(buildSpec(search, role, status), pageable)
                .map(AdminUserResponse::from);
        return PageResponse.from(result);
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        return AdminUserResponse.from(find(id));
    }

    @Transactional
    public AdminUserResponse changeStatus(UUID actorId, UUID targetId, UpdateStatusRequest request) {
        if (actorId.equals(targetId)) {
            throw new AdminOperationNotAllowedException("You cannot change your own status");
        }
        User target = find(targetId);
        UserStatus before = target.getStatus();
        UserStatus after = request.status();

        if (before == after) {
            return AdminUserResponse.from(target); // nothing to do
        }
        if (after == UserStatus.Suspended && target.getRole() == Role.SuperAdmin
                && userRepository.countByRoleAndStatus(Role.SuperAdmin, UserStatus.Active) <= 1) {
            throw new AdminOperationNotAllowedException("Cannot suspend the last active SuperAdmin");
        }

        target.setStatus(after);
        userRepository.saveAndFlush(target);

        if (after == UserStatus.Suspended) {
            refreshTokenService.revokeAllForUser(targetId);
        }

        auditService.record(actorId, targetId,
                after == UserStatus.Suspended ? "USER_SUSPENDED" : "USER_REACTIVATED",
                Map.of("status", Map.of("before", before.name(), "after", after.name())),
                request.reason());

        return AdminUserResponse.from(target);
    }

    @Transactional
    public AdminUserResponse changeRole(UUID actorId, UUID targetId, UpdateRoleRequest request) {
        if (actorId.equals(targetId)) {
            throw new AdminOperationNotAllowedException("You cannot change your own role");
        }
        User target = find(targetId);
        Role before = target.getRole();
        Role after = request.role();

        if (before == after) {
            return AdminUserResponse.from(target);
        }
        if (before == Role.SuperAdmin
                && userRepository.countByRoleAndStatus(Role.SuperAdmin, UserStatus.Active) <= 1) {
            throw new AdminOperationNotAllowedException("Cannot demote the last active SuperAdmin");
        }

        target.setRole(after);
        userRepository.saveAndFlush(target);

        // the role lives inside the access token, so force a fresh login to pick up the new one
        refreshTokenService.revokeAllForUser(targetId);

        auditService.record(actorId, targetId, "USER_ROLE_CHANGED",
                Map.of("role", Map.of("before", before.name(), "after", after.name())),
                request.reason());

        return AdminUserResponse.from(target);
    }

    private User find(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }

    private static Specification<User> buildSpec(String search, Role role, UserStatus status) {
        Specification<User> spec = (root, q, cb) -> cb.conjunction();

        if (role != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("email")), pattern, '\\'),
                    cb.like(cb.lower(root.get("fullName")), pattern, '\\')));
        }
        return spec;
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}