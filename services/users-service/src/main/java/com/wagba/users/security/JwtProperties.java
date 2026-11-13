package com.wagba.users.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "wagba.jwt")
public record JwtProperties(
        String issuer,
        String privateKeyPath,
        String publicKeyPath,
        Duration accessTokenTtl,
        Duration refreshTokenTtl,
        boolean generateIfMissing
) {
}