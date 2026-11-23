package com.wagba.restaurants.restaurant;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

/** Who is asking. Anonymous visitors have no user id. */
public record Viewer(UUID userId, boolean admin) {

    public static Viewer anonymous() {
        return new Viewer(null, false);
    }

    /** The jwt is null when the request carries no token. */
    public static Viewer from(Jwt jwt) {
        if (jwt == null) {
            return anonymous();
        }
        return new Viewer(UUID.fromString(jwt.getSubject()), "SuperAdmin".equals(jwt.getClaimAsString("role")));
    }

    public boolean isOwnerOrAdmin(Restaurant r) {
        return admin || (userId != null && userId.equals(r.getOwnerUserId()));
    }

    public boolean canSee(Restaurant r) {
        return r.isPubliclyVisible() || isOwnerOrAdmin(r);
    }
}