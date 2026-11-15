package com.wagba.restaurants.common;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** TEMPORARY: proves that tokens issued by users-service are accepted here. Delete in KAN-19. */
@RestController
public class WhoAmIController {

    @GetMapping("/whoami")
    public Map<String, Object> whoAmI(@AuthenticationPrincipal Jwt jwt) {
        return Map.of(
                "userId", jwt.getSubject(),
                "role", jwt.getClaimAsString("role"),
                "expiresAt", jwt.getExpiresAt().toString());
    }
}