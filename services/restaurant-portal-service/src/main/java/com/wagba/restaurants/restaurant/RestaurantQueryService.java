package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.common.PageResponse;
import com.wagba.restaurants.common.RestaurantNotFoundException;
import com.wagba.restaurants.menu.*;
import com.wagba.restaurants.menu.dto.MenuResponse;
import com.wagba.restaurants.restaurant.dto.PublicRestaurantResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class RestaurantQueryService {

    private static final int MAX_PAGE_SIZE = 50;

    private final RestaurantRepository restaurantRepository;
    private final WorkingHoursRepository workingHoursRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final OptionGroupRepository optionGroupRepository;
    private final MenuOptionRepository menuOptionRepository;

    // ------------------------------------------------------------------ list

    @Transactional(readOnly = true)
    public PageResponse<PublicRestaurantResponse> search(String search, int page, int size) {
        var pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("name").and(Sort.by("id"))); // id breaks ties so pages never overlap

        Page<Restaurant> result = restaurantRepository.findAll(publicSpec(search), pageable);

        Map<UUID, List<WorkingHours>> hours = hoursFor(result.getContent());
        Instant now = Instant.now();
        return PageResponse.from(result.map(
                r -> toPublic(r, hours.getOrDefault(r.getId(), List.of()), now)));
    }

    // --------------------------------------------------------------- details

    @Transactional(readOnly = true)
    public PublicRestaurantResponse get(UUID id, Viewer viewer) {
        Restaurant r = findVisible(id, viewer);
        List<WorkingHours> hours = workingHoursRepository.findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(id);
        return toPublic(r, hours, Instant.now());
    }

    // ------------------------------------------------------------------ menu

    @Transactional(readOnly = true)
    public MenuResponse menu(UUID id, Viewer viewer) {
        Restaurant r = findVisible(id, viewer);
        boolean showEmptyCategories = viewer.isOwnerOrAdmin(r);

        List<Category> categories =
                categoryRepository.findByRestaurantIdAndActiveTrueOrderBySortOrderAscNameAsc(id);
        Set<UUID> activeCategoryIds = categories.stream().map(Category::getId).collect(Collectors.toSet());

        // an active item inside an archived category stays hidden
        List<MenuItem> items = menuItemRepository
                .findByRestaurantIdAndActiveTrueOrderBySortOrderAscNameAsc(id).stream()
                .filter(i -> activeCategoryIds.contains(i.getCategoryId()))
                .toList();

        List<UUID> itemIds = items.stream().map(MenuItem::getId).toList();
        List<OptionGroup> groups = itemIds.isEmpty() ? List.of()
                : optionGroupRepository.findByItemIdInAndActiveTrueOrderBySortOrderAscNameAsc(itemIds);

        List<UUID> groupIds = groups.stream().map(OptionGroup::getId).toList();
        List<MenuOption> options = groupIds.isEmpty() ? List.of()
                : menuOptionRepository.findByGroupIdInAndActiveTrueOrderBySortOrderAscNameAsc(groupIds);

        Map<UUID, List<MenuResponse.OptionDto>> optionsByGroup = options.stream()
                .collect(groupingBy(MenuOption::getGroupId, LinkedHashMap::new,
                        mapping(o -> new MenuResponse.OptionDto(o.getId(), o.getName(), o.getExtraPriceMinor()),
                                toList())));

        Map<UUID, List<MenuResponse.OptionGroupDto>> groupsByItem = groups.stream()
                .collect(groupingBy(OptionGroup::getItemId, LinkedHashMap::new,
                        mapping(g -> new MenuResponse.OptionGroupDto(g.getId(), g.getName(),
                                        g.getMinSelections(), g.getMaxSelections(), g.getMinSelections() > 0,
                                        optionsByGroup.getOrDefault(g.getId(), List.of())),
                                toList())));

        Map<UUID, List<MenuResponse.ItemDto>> itemsByCategory = items.stream()
                .collect(groupingBy(MenuItem::getCategoryId, LinkedHashMap::new,
                        mapping(i -> new MenuResponse.ItemDto(i.getId(), i.getName(), i.getDescription(),
                                        i.getPriceMinor(), i.getImageUrl(), i.isAvailable(),
                                        groupsByItem.getOrDefault(i.getId(), List.of())),
                                toList())));

        List<MenuResponse.CategoryDto> result = categories.stream()
                .map(c -> new MenuResponse.CategoryDto(c.getId(), c.getName(), c.getSortOrder(),
                        itemsByCategory.getOrDefault(c.getId(), List.of())))
                .filter(c -> showEmptyCategories || !c.items().isEmpty())
                .toList();

        List<WorkingHours> hours = workingHoursRepository.findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(id);
        boolean openNow = OpeningHours.isOpenAt(hours, ZoneId.of(r.getTimezone()), Instant.now());

        return new MenuResponse(r.getId(), r.getCurrency(), r.isAcceptingOrders(), openNow, result);
    }

    // --------------------------------------------------------------- helpers

    /** Hidden restaurants look exactly like missing ones. */
    private Restaurant findVisible(UUID id, Viewer viewer) {
        return restaurantRepository.findById(id)
                .filter(viewer::canSee)
                .orElseThrow(() -> new RestaurantNotFoundException(id));
    }

    private PublicRestaurantResponse toPublic(Restaurant r, List<WorkingHours> hours, Instant now) {
        boolean openNow = OpeningHours.isOpenAt(hours, ZoneId.of(r.getTimezone()), now);
        return PublicRestaurantResponse.from(r, hours, openNow);
    }

    /** One query for the whole page instead of one per restaurant. */
    private Map<UUID, List<WorkingHours>> hoursFor(List<Restaurant> restaurants) {
        if (restaurants.isEmpty()) {
            return Map.of();
        }
        return workingHoursRepository
                .findByRestaurantIdInOrderByDayOfWeekAscOpensAtAsc(
                        restaurants.stream().map(Restaurant::getId).toList())
                .stream()
                .collect(groupingBy(WorkingHours::getRestaurantId));
    }

    private static Specification<Restaurant> publicSpec(String search) {
        Specification<Restaurant> spec = (root, q, cb) -> cb.and(
                cb.equal(root.get("status"), RestaurantStatus.Approved),
                cb.isFalse(root.get("ownerSuspended")));

        if (search != null && !search.isBlank()) {
            String pattern = "%" + escapeLike(search.trim().toLowerCase()) + "%";
            spec = spec.and((root, q, cb) -> cb.or(
                    cb.like(cb.lower(root.get("name")), pattern, '\\'),
                    cb.like(cb.lower(root.get("description")), pattern, '\\')));
        }
        return spec;
    }

    /** % and _ typed by the visitor are plain characters, not wildcards. */
    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}