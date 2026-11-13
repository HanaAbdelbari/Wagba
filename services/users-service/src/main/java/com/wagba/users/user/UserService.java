package com.wagba.users.user;

import com.wagba.users.auth.RefreshTokenService;
import com.wagba.users.common.InvalidPasswordChangeException;
import com.wagba.users.common.UserNotFoundException;
import com.wagba.users.user.dto.ChangePasswordRequest;
import com.wagba.users.user.dto.UpdateProfileRequest;
import com.wagba.users.user.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenService refreshTokenService;

    @Transactional(readOnly = true)
    public UserResponse getById(UUID id) {
        return UserResponse.from(find(id));
    }

    @Transactional
    public UserResponse updateProfile(UUID id, UpdateProfileRequest request) {
        User user = find(id);

        if (request.fullName() != null) {
            user.setFullName(request.fullName().trim());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(UUID id, ChangePasswordRequest request) {
        User user = find(id);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordChangeException("Current password is incorrect");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordChangeException("New password must be different from the current one");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        // same transaction: the password change and the session revocation succeed or fail together
        refreshTokenService.revokeAllForUser(user.getId());
    }

    private User find(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new UserNotFoundException(id));
    }
}