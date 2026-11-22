package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.audit.AuditService;
import com.wagba.restaurants.common.InvalidRequestException;
import com.wagba.restaurants.common.RestaurantNotFoundException;
import com.wagba.restaurants.common.RestaurantStateException;
import com.wagba.restaurants.outbox.OutboxService;
import com.wagba.restaurants.restaurant.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Currency;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.wagba.restaurants.restaurant.RestaurantEventPayloads.availability;
import static com.wagba.restaurants.restaurant.RestaurantEventPayloads.snapshot;
import static com.wagba.restaurants.restaurant.RestaurantEventPayloads.version;

@Service
@RequiredArgsConstructor
public class RestaurantService {

    private static final String DEFAULT_TIMEZONE = "Africa/Cairo";
    private static final String DEFAULT_CURRENCY = "EGP";
    private static final String ENTITY = "Restaurant";

    private final RestaurantRepository restaurantRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;

    // ------------------------------------------------------------------ create

    @Transactional
    public RestaurantResponse create(UUID ownerId, CreateRestaurantRequest request) {
        requireBothOrNone(request.latitude(), request.longitude());

        Restaurant r = new Restaurant();
        r.setOwnerUserId(ownerId);
        r.setName(request.name().trim());
        r.setDescription(blankToNull(request.description()));
        r.setPhone(request.phone());
        r.setAddress(request.address().trim());
        r.setLatitude(scale(request.latitude()));
        r.setLongitude(scale(request.longitude()));
        r.setTimezone(resolveTimezone(request.timezone()));
        r.setCurrency(resolveCurrency(request.currency()));
        restaurantRepository.saveAndFlush(r); // status is Pending by default

        auditService.record(ownerId, r.getId(), ENTITY, r.getId(), "RESTAURANT_SUBMITTED",
                Map.of("name", r.getName(), "status", "Pending"), null);
        outboxService.publish(ENTITY, r.getId(), version(r), "RestaurantSubmitted", snapshot(r));

        return toResponse(r);
    }

    // -------------------------------------------------------------------- read

    @Transactional(readOnly = true)
    public List<RestaurantResponse> listMine(UUID ownerId) {
        List<Restaurant> restaurants = restaurantRepository.findByOwnerUserIdOrderByCreatedAtDesc(ownerId);
        if (restaurants.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<WorkingHours>> hours = workingHoursRepository
                .findByRestaurantIdInOrderByDayOfWeekAscOpensAtAsc(
                        restaurants.stream().map(Restaurant::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(WorkingHours::getRestaurantId));

        return restaurants.stream()
                .map(r -> RestaurantResponse.from(r, hours.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    // ------------------------------------------------------------------ update

    @Transactional
    public RestaurantResponse update(UUID ownerId, UUID id, UpdateRestaurantRequest request) {
        Restaurant r = loadOwned(ownerId, id);
        requireEditable(r);

        // PATCH cannot clear coordinates (null means unchanged), so they travel together
        if (request.latitude() != null || request.longitude() != null) {
            requireBothOrNone(request.latitude(), request.longitude());
            if (request.latitude() == null) {
                throw new InvalidRequestException("Latitude and longitude must be provided together");
            }
        }

        Map<String, Object> changes = new LinkedHashMap<>();

        if (request.name() != null) {
            String value = request.name().trim();
            track(changes, "name", r.getName(), value);
            r.setName(value);
        }
        if (request.description() != null) {
            String value = blankToNull(request.description()); // an empty string clears it
            track(changes, "description", r.getDescription(), value);
            r.setDescription(value);
        }
        if (request.phone() != null) {
            track(changes, "phone", r.getPhone(), request.phone());
            r.setPhone(request.phone());
        }
        if (request.address() != null) {
            String value = request.address().trim();
            track(changes, "address", r.getAddress(), value);
            r.setAddress(value);
        }
        if (request.latitude() != null) {
            BigDecimal lat = scale(request.latitude());
            BigDecimal lon = scale(request.longitude());
            track(changes, "latitude", r.getLatitude(), lat);
            track(changes, "longitude", r.getLongitude(), lon);
            r.setLatitude(lat);
            r.setLongitude(lon);
        }
        if (request.timezone() != null) {
            String value = resolveTimezone(request.timezone());
            track(changes, "timezone", r.getTimezone(), value);
            r.setTimezone(value);
        }

        if (changes.isEmpty()) {
            return toResponse(r); // nothing changed: no write, no audit, no event
        }

        restaurantRepository.saveAndFlush(r);

        auditService.record(ownerId, id, ENTITY, id, "RESTAURANT_UPDATED", changes, null);
        outboxService.publish(ENTITY, id, version(r), "RestaurantUpdated", snapshot(r));

        return toResponse(r);
    }

    // ----------------------------------------------------------- working hours

    @Transactional
    public List<WorkingHoursResponse> replaceWorkingHours(UUID ownerId, UUID id, WorkingHoursRequest request) {
        Restaurant r = loadOwned(ownerId, id);
        requireEditable(r);

        List<WorkingHoursRequest.Period> periods = request.hours();
        WorkingHoursValidator.validateNoOverlap(periods);

        int before = workingHoursRepository.findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(id).size();

        workingHoursRepository.deleteAllForRestaurant(id);
        List<WorkingHours> rows = periods.stream().map(p -> toRow(id, p)).toList();
        workingHoursRepository.saveAll(rows);

        auditService.record(ownerId, id, ENTITY, id, "RESTAURANT_HOURS_CHANGED",
                Map.of("periods", Map.of("before", String.valueOf(before), "after", String.valueOf(rows.size()))),
                null);

        return workingHoursRepository.findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(id).stream()
                .map(WorkingHoursResponse::from)
                .toList();
    }

    // ------------------------------------------------------------ availability

    @Transactional
    public RestaurantResponse setAcceptingOrders(UUID ownerId, UUID id, boolean accepting) {
        Restaurant r = loadOwned(ownerId, id);
        requireEditable(r);

        if (r.isAcceptingOrders() == accepting) {
            return toResponse(r);
        }
        r.setAcceptingOrders(accepting);
        restaurantRepository.saveAndFlush(r);

        auditService.record(ownerId, id, ENTITY, id, "RESTAURANT_AVAILABILITY_CHANGED",
                Map.of("acceptingOrders", Map.of("before", String.valueOf(!accepting),
                        "after", String.valueOf(accepting))), null);
        outboxService.publish(ENTITY, id, version(r), "RestaurantAvailabilityChanged", availability(r));

        return toResponse(r);
    }

    // ----------------------------------------------------------------- helpers

    /** Missing and "not yours" look the same, so the existence of other people's ids is not revealed. */
    private Restaurant loadOwned(UUID ownerId, UUID id) {
        return restaurantRepository.findById(id)
                .filter(r -> r.getOwnerUserId().equals(ownerId))
                .orElseThrow(() -> new RestaurantNotFoundException(id));
    }

    private static void requireEditable(Restaurant r) {
        if (r.getStatus() == RestaurantStatus.Suspended) {
            throw new RestaurantStateException("A suspended restaurant cannot be edited. Please contact support.");
        }
    }

    private RestaurantResponse toResponse(Restaurant r) {
        return RestaurantResponse.from(r,
                workingHoursRepository.findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(r.getId()));
    }

    private static WorkingHours toRow(UUID restaurantId, WorkingHoursRequest.Period p) {
        WorkingHours w = new WorkingHours();
        w.setRestaurantId(restaurantId);
        w.setDayOfWeek(p.dayOfWeek().shortValue());
        w.setOpensAt(p.opensAt().truncatedTo(ChronoUnit.MINUTES));
        w.setClosesAt(p.closesAt().truncatedTo(ChronoUnit.MINUTES));
        w.setClosesNextDay(WorkingHoursValidator.closesNextDay(p.opensAt(), p.closesAt()));
        return w;
    }

    private static void requireBothOrNone(BigDecimal latitude, BigDecimal longitude) {
        if ((latitude == null) != (longitude == null)) {
            throw new InvalidRequestException("Latitude and longitude must be provided together");
        }
    }

    private static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(6, RoundingMode.HALF_UP);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String resolveTimezone(String timezone) {
        if (timezone == null || timezone.isBlank()) {
            return DEFAULT_TIMEZONE;
        }
        String value = timezone.trim();
        if (!ZoneId.getAvailableZoneIds().contains(value)) {
            throw new InvalidRequestException("Unknown timezone: " + value);
        }
        return value;
    }

    private static String resolveCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        try {
            return Currency.getInstance(currency.trim()).getCurrencyCode();
        } catch (IllegalArgumentException e) {
            throw new InvalidRequestException("Unknown currency: " + currency);
        }
    }

    private static void track(Map<String, Object> changes, String field, Object before, Object after) {
        if (!Objects.equals(before, after)) {
            Map<String, Object> pair = new LinkedHashMap<>();
            pair.put("before", before == null ? null : before.toString());
            pair.put("after", after == null ? null : after.toString());
            changes.put(field, pair);
        }
    }
}