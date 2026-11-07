package com.wagba.users.user;

import com.wagba.users.user.dto.ChangePasswordRequest;
import com.wagba.users.user.dto.UpdateProfileRequest;
import com.wagba.users.user.dto.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getById(currentUserId(jwt));
    }

    @PatchMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(currentUserId(jwt), request);
    }

    @PostMapping("/me/change-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserId(jwt), request);
    }

    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}