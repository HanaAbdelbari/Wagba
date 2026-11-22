package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.restaurant.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@RequiredArgsConstructor
public class RestaurantController {

    private final RestaurantService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantResponse create(@AuthenticationPrincipal Jwt jwt,
                                     @Valid @RequestBody CreateRestaurantRequest request) {
        return service.create(currentUserId(jwt), request);
    }

    @GetMapping("/mine")
    public List<RestaurantResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listMine(currentUserId(jwt));
    }

    @PatchMapping("/{id}")
    public RestaurantResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                     @Valid @RequestBody UpdateRestaurantRequest request) {
        return service.update(currentUserId(jwt), id, request);
    }

    @PutMapping("/{id}/working-hours")
    public List<WorkingHoursResponse> replaceWorkingHours(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                                          @Valid @RequestBody WorkingHoursRequest request) {
        return service.replaceWorkingHours(currentUserId(jwt), id, request);
    }

    @PutMapping("/{id}/availability")
    public RestaurantResponse setAvailability(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                              @Valid @RequestBody AvailabilityRequest request) {
        return service.setAcceptingOrders(currentUserId(jwt), id, request.acceptingOrders());
    }

    // the owner id always comes from the token, never from the URL or the body
    private static UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}