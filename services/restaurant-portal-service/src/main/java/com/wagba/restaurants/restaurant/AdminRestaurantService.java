package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.audit.AuditService;
import com.wagba.restaurants.common.PageResponse;
import com.wagba.restaurants.common.RestaurantNotFoundException;
import com.wagba.restaurants.common.RestaurantStateException;
import com.wagba.restaurants.outbox.OutboxService;
import com.wagba.restaurants.restaurant.dto.AdminRestaurantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRestaurantService {

    private static final String ENTITY = "Restaurant";
    private static final int MAX_PAGE_SIZE = 100;

    private final RestaurantRepository restaurantRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;

    // ------------------------------------------------------------------ read

    @Transactional(readOnly = true)
    public PageResponse<AdminRestaurantResponse> search(RestaurantStatus status, String search,
                                                        UUID ownerUserId, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id")));

        return PageResponse.from(restaurantRepository
                .findAll(buildSpec(status, search, ownerUserId), pageable)
                .map(AdminRestaurantResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminRestaurantResponse get(UUID id) {
        return AdminRestaurantResponse.from(find(id));
    }

    // ----------------------------------------------------------- transitions

    @Transactional
    public AdminRestaurantResponse approve(UUID adminId, UUID id, String note) {
        return transition(adminId, id, "approve", RestaurantStatus.Approved,
                EnumSet.of(RestaurantStatus.Pending),
                "RESTAURANT_APPROVED", "RestaurantApproved", note);
    }

    @Transactional
    public AdminRestaurantResponse reject(UUID adminId, UUID id, String reason) {
        return transition(adminId, id, "reject", RestaurantStatus.Rejected,
                EnumSet.of(RestaurantStatus.Pending),
                "RESTAURANT_REJECTED", "RestaurantRejected", reason);
    }

    @Transactional
    public AdminRestaurantResponse suspend(UUID adminId, UUID id, String reason) {
        return transition(adminId, id, "suspend", RestaurantStatus.Suspended,
                EnumSet.of(RestaurantStatus.Approved),
                "RESTAURANT_SUSPENDED", "RestaurantSuspended", reason);
    }

    @Transactional
    public AdminRestaurantResponse reactivate(UUID adminId, UUID id, String note) {
        return transition(adminId, id, "reactivate", RestaurantStatus.Approved,
                EnumSet.of(RestaurantStatus.Suspended),
                "RESTAURANT_REACTIVATED", "RestaurantReactivated", note);
    }

    /**
     * One place for every status change, so they all follow the same rules:
     * already in the target status is a harmless no-op, an invalid move is a 409,
     * and a real change writes the review trail, an audit row and an outbox event together.
     */
    private AdminRestaurantResponse transition(UUID adminId, UUID id, String verb, RestaurantStatus target,
                                               Set<RestaurantStatus> allowedFrom, String auditAction,
                                               String eventType, String reason) {
        Restaurant r = find(id);
        RestaurantStatus before = r.getStatus();

        if (before == target) {
            return AdminRestaurantResponse.from(r); // retry-safe: nothing to do
        }
        if (!allowedFrom.contains(before)) {
            throw new RestaurantStateException("Cannot " + verb + " a restaurant that is " + before);
        }

        String cleanReason = blankToNull(reason);
        r.setStatus(target);
        r.setReviewedByUserId(adminId);
        r.setReviewedAt(Instant.now());
        r.setReviewNote(cleanReason);
        restaurantRepository.saveAndFlush(r); // flush first so the event carries the new version

        auditService.record(adminId, id, ENTITY, id, auditAction,
                Map.of("status", Map.of("before", before.name(), "after", target.name())),
                cleanReason);
        outboxService.publish(ENTITY, id, RestaurantEventPayloads.version(r), eventType,
                RestaurantEventPayloads.reviewed(r, before, adminId, cleanReason));

        return AdminRestaurantResponse.from(r);
    }

    // --------------------------------------------------------------- helpers

    private Restaurant find(UUID id) {
        return restaurantRepository.findById(id).orElseThrow(() -> new RestaurantNotFoundException(id));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static Specification<Restaurant> buildSpec(RestaurantStatus status, String search, UUID ownerUserId) {
        Specification<Restaurant> spec = (root, q, cb) -> cb.conjunction();

        if (status != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("status"), status));
        }
        if (ownerUserId != null) {
            spec = spec.and((root, q, cb) -> cb.equal(root.get("ownerUserId"), ownerUserId));
        }
        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("description")), pattern, '\\'),
                    cb.like(cb.lower(root.get("address")), pattern, '\\')));
        }
        return spec;
    }

    /** % and _ typed by the admin are plain characters, not wildcards. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}