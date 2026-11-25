package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.common.PageResponse;
import com.wagba.restaurants.restaurant.dto.AdminRestaurantResponse;
import com.wagba.restaurants.restaurant.dto.ReasonRequest;
import com.wagba.restaurants.restaurant.dto.ReviewNoteRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** SuperAdmin only: the rule for /admin/** lives in SecurityConfig. */
@RestController
@RequestMapping("/admin/restaurants")
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final AdminRestaurantService service;

    @GetMapping
    public PageResponse<AdminRestaurantResponse> list(
            @RequestParam(required = false) RestaurantStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID ownerUserId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.search(status, search, ownerUserId, page, size);
    }

    @GetMapping("/{id}")
    public AdminRestaurantResponse get(@PathVariable UUID id) {
        return service.get(id);
    }

    // the body is optional here: an empty POST approves without a note
    @PostMapping("/{id}/approve")
    public AdminRestaurantResponse approve(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody(required = false) ReviewNoteRequest request) {
        return service.approve(adminId(jwt), id, request == null ? null : request.note());
    }

    @PostMapping("/{id}/reject")
    public AdminRestaurantResponse reject(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                          @Valid @RequestBody ReasonRequest request) {
        return service.reject(adminId(jwt), id, request.reason());
    }

    @PostMapping("/{id}/suspend")
    public AdminRestaurantResponse suspend(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody ReasonRequest request) {
        return service.suspend(adminId(jwt), id, request.reason());
    }

    @PostMapping("/{id}/reactivate")
    public AdminRestaurantResponse reactivate(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @Valid @RequestBody(required = false) ReviewNoteRequest request) {
        return service.reactivate(adminId(jwt), id, request == null ? null : request.note());
    }

    // the admin id always comes from the token
    private static UUID adminId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}