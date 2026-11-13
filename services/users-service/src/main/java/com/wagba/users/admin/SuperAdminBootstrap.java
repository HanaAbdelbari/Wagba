package com.wagba.users.admin;

import com.wagba.users.audit.AuditService;
import com.wagba.users.user.Role;
import com.wagba.users.user.User;
import com.wagba.users.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SuperAdminBootstrap implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${wagba.bootstrap.admin-email:}")
    private String email;

    @Value("${wagba.bootstrap.admin-password:}")
    private String password;

    @Value("${wagba.bootstrap.admin-name:Super Admin}")
    private String name;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRole(Role.SuperAdmin)) {
            return;
        }
        if (email.isBlank() || password.isBlank()) {
            log.warn("No SuperAdmin exists. Set WAGBA_ADMIN_EMAIL and WAGBA_ADMIN_PASSWORD to create one.");
            return;
        }
        if (password.length() < 12) {
            log.error("Bootstrap SuperAdmin password must be at least 12 characters. Skipping.");
            return;
        }
        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(normalized)) {
            log.error("Bootstrap email already belongs to a non-admin account. Use a different email.");
            return;
        }

        User admin = new User();
        admin.setFullName(name);
        admin.setEmail(normalized);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(Role.SuperAdmin);
        userRepository.save(admin);

        auditService.record(null, admin.getId(), "USER_CREATED",
                Map.of("role", "SuperAdmin", "source", "bootstrap"),
                "Initial SuperAdmin bootstrap");

        log.info("Initial SuperAdmin created: {}", normalized);
    }
}