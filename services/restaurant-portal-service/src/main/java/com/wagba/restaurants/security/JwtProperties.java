package com.wagba.restaurants.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "wagba.jwt")
public record JwtProperties(String issuer, String publicKeyPath) {
}