package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.common.PageResponse;
import com.wagba.restaurants.menu.dto.MenuResponse;
import com.wagba.restaurants.restaurant.dto.PublicRestaurantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Open to everyone. A valid token only widens what the caller may see (own and, for admins, all restaurants). */
@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class PublicRestaurantController {

    private final RestaurantQueryService service;

    @GetMapping
    public PageResponse<PublicRestaurantResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(search, page, size);
    }

    @GetMapping("/{id}")
    public PublicRestaurantResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.get(id, Viewer.from(jwt));
    }

    @GetMapping("/{id}/menu")
    public MenuResponse menu(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return service.menu(id, Viewer.from(jwt));
    }
}