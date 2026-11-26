package com.wagba.restaurants.menu;

import com.wagba.restaurants.menu.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Menu management for restaurant owners. Ownership is checked in MenuService. */
@RestController
@RequiredArgsConstructor
public class MenuManagementController {

    private final MenuService service;

    // ----- categories

    @PostMapping("/restaurants/{restaurantId}/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse createCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID restaurantId,
                                           @Valid @RequestBody CreateCategoryRequest request) {
        return service.createCategory(userId(jwt), restaurantId, request);
    }

    @PatchMapping("/categories/{id}")
    public CategoryResponse updateCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                           @Valid @RequestBody UpdateCategoryRequest request) {
        return service.updateCategory(userId(jwt), id, request);
    }

    @DeleteMapping("/categories/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveCategory(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.archiveCategory(userId(jwt), id);
    }

    // ----- items

    @PostMapping("/restaurants/{restaurantId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public ItemResponse createItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID restaurantId,
                                   @Valid @RequestBody CreateItemRequest request) {
        return service.createItem(userId(jwt), restaurantId, request);
    }

    @PatchMapping("/items/{id}")
    public ItemResponse updateItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                   @Valid @RequestBody UpdateItemRequest request) {
        return service.updateItem(userId(jwt), id, request);
    }

    @DeleteMapping("/items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archiveItem(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        service.archiveItem(userId(jwt), id);
    }

    @PutMapping("/items/{id}/availability")
    public ItemResponse setAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                        @Valid @RequestBody ItemAvailabilityRequest request) {
        return service.setAvailability(userId(jwt), id, request.available());
    }

    // the owner id always comes from the token, never from the URL or the body
    private static UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}