package com.wagba.restaurants.menu;

import com.wagba.restaurants.audit.AuditService;
import com.wagba.restaurants.common.Changes;
import com.wagba.restaurants.common.DuplicateNameException;
import com.wagba.restaurants.common.InvalidRequestException;
import com.wagba.restaurants.common.MenuNotFoundException;
import com.wagba.restaurants.common.RestaurantNotFoundException;
import com.wagba.restaurants.common.RestaurantStateException;
import com.wagba.restaurants.menu.dto.*;
import com.wagba.restaurants.outbox.OutboxService;
import com.wagba.restaurants.restaurant.Restaurant;
import com.wagba.restaurants.restaurant.RestaurantRepository;
import com.wagba.restaurants.restaurant.RestaurantStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import static com.wagba.restaurants.menu.MenuEventPayloads.availability;
import static com.wagba.restaurants.menu.MenuEventPayloads.priceChanged;
import static com.wagba.restaurants.menu.MenuEventPayloads.snapshot;
import static com.wagba.restaurants.menu.MenuEventPayloads.version;

@Service
@RequiredArgsConstructor
public class MenuService {

    private static final String CATEGORY = "Category";
    private static final String ITEM = "MenuItem";

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository itemRepository;
    private final AuditService auditService;
    private final OutboxService outboxService;

    private record OwnedCategory(Category category, Restaurant restaurant) {
    }

    private record OwnedItem(MenuItem item, Restaurant restaurant) {
    }

    // ================================================================ categories

    @Transactional
    public CategoryResponse createCategory(UUID ownerId, UUID restaurantId, CreateCategoryRequest request) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .filter(x -> x.getOwnerUserId().equals(ownerId))
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));
        requireEditable(r);

        String name = request.name().trim();
        if (categoryRepository.existsByRestaurantIdAndNameIgnoreCaseAndActiveTrue(restaurantId, name)) {
            throw new DuplicateNameException("A category named '" + name + "' already exists");
        }

        Category c = new Category();
        c.setRestaurantId(restaurantId);
        c.setName(name);
        c.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        saveOrConflict(() -> categoryRepository.saveAndFlush(c), "category", name);

        auditService.record(ownerId, restaurantId, CATEGORY, c.getId(), "CATEGORY_CREATED",
                Map.of("name", name), null);
        return CategoryResponse.from(c);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID ownerId, UUID id, UpdateCategoryRequest request) {
        OwnedCategory owned = findOwnedCategory(ownerId, id);
        requireEditable(owned.restaurant());
        Category c = owned.category();
        if (!c.isActive()) {
            throw new MenuNotFoundException("Category not found");
        }

        Map<String, Object> changes = new LinkedHashMap<>();

        if (request.name() != null) {
            String name = request.name().trim();
            if (categoryRepository.existsByRestaurantIdAndNameIgnoreCaseAndActiveTrueAndIdNot(
                    c.getRestaurantId(), name, c.getId())) {
                throw new DuplicateNameException("A category named '" + name + "' already exists");
            }
            Changes.track(changes, "name", c.getName(), name);
            c.setName(name);
        }
        if (request.sortOrder() != null) {
            Changes.track(changes, "sortOrder", c.getSortOrder(), request.sortOrder());
            c.setSortOrder(request.sortOrder());
        }
        if (changes.isEmpty()) {
            return CategoryResponse.from(c);
        }

        saveOrConflict(() -> categoryRepository.saveAndFlush(c), "category", c.getName());
        auditService.record(ownerId, c.getRestaurantId(), CATEGORY, c.getId(), "CATEGORY_UPDATED", changes, null);
        return CategoryResponse.from(c);
    }

    @Transactional
    public void archiveCategory(UUID ownerId, UUID id) {
        OwnedCategory owned = findOwnedCategory(ownerId, id);
        requireEditable(owned.restaurant());
        Category c = owned.category();

        if (!c.isActive()) {
            return; // already archived: nothing to do
        }
        if (itemRepository.existsByCategoryIdAndActiveTrue(c.getId())) {
            throw new RestaurantStateException(
                    "This category still has active items. Archive or move them first.");
        }

        c.setActive(false);
        categoryRepository.saveAndFlush(c);
        auditService.record(ownerId, c.getRestaurantId(), CATEGORY, c.getId(), "CATEGORY_ARCHIVED",
                Map.of("name", c.getName()), null);
    }

    // ===================================================================== items

    @Transactional
    public ItemResponse createItem(UUID ownerId, UUID restaurantId, CreateItemRequest request) {
        Restaurant r = restaurantRepository.findById(restaurantId)
                .filter(x -> x.getOwnerUserId().equals(ownerId))
                .orElseThrow(() -> new RestaurantNotFoundException(restaurantId));
        requireEditable(r);

        activeCategoryOf(restaurantId, request.categoryId());

        String name = request.name().trim();
        if (itemRepository.existsByCategoryIdAndNameIgnoreCaseAndActiveTrue(request.categoryId(), name)) {
            throw new DuplicateNameException("An item named '" + name + "' already exists in this category");
        }

        MenuItem item = new MenuItem();
        item.setRestaurantId(restaurantId);
        item.setCategoryId(request.categoryId());
        item.setName(name);
        item.setDescription(blankToNull(request.description()));
        item.setPriceMinor(request.priceMinor());
        item.setImageUrl(blankToNull(request.imageUrl()));
        item.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        item.setAvailable(request.available() == null || request.available());
        saveOrConflict(() -> itemRepository.saveAndFlush(item), "item", name);

        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("name", name);
        changes.put("priceMinor", String.valueOf(item.getPriceMinor()));
        changes.put("categoryId", item.getCategoryId().toString());
        auditService.record(ownerId, restaurantId, ITEM, item.getId(), "ITEM_CREATED", changes, null);
        outboxService.publish(ITEM, item.getId(), version(item), "MenuItemCreated",
                snapshot(item, r.getCurrency()));

        return ItemResponse.from(item, r.getCurrency());
    }

    @Transactional
    public ItemResponse updateItem(UUID ownerId, UUID id, UpdateItemRequest request) {
        OwnedItem owned = findOwnedItem(ownerId, id);
        Restaurant r = owned.restaurant();
        requireEditable(r);
        MenuItem item = owned.item();
        if (!item.isActive()) {
            throw new MenuNotFoundException("Item not found");
        }

        Map<String, Object> changes = new LinkedHashMap<>();
        long oldPrice = item.getPriceMinor();

        // decide the new name and category first, validate, and only then touch the entity
        UUID newCategoryId = item.getCategoryId();
        if (request.categoryId() != null && !request.categoryId().equals(item.getCategoryId())) {
            activeCategoryOf(r.getId(), request.categoryId());
            newCategoryId = request.categoryId();
        }
        String newName = request.name() != null ? request.name().trim() : item.getName();

        boolean nameOrCategoryChanged = !newName.equals(item.getName())
                || !newCategoryId.equals(item.getCategoryId());
        if (nameOrCategoryChanged
                && itemRepository.existsByCategoryIdAndNameIgnoreCaseAndActiveTrueAndIdNot(
                newCategoryId, newName, item.getId())) {
            throw new DuplicateNameException("An item named '" + newName + "' already exists in this category");
        }

        Changes.track(changes, "categoryId", item.getCategoryId(), newCategoryId);
        Changes.track(changes, "name", item.getName(), newName);
        item.setCategoryId(newCategoryId);
        item.setName(newName);

        if (request.description() != null) {
            String value = blankToNull(request.description());
            Changes.track(changes, "description", item.getDescription(), value);
            item.setDescription(value);
        }
        if (request.imageUrl() != null) {
            String value = blankToNull(request.imageUrl());
            Changes.track(changes, "imageUrl", item.getImageUrl(), value);
            item.setImageUrl(value);
        }
        if (request.sortOrder() != null) {
            Changes.track(changes, "sortOrder", item.getSortOrder(), request.sortOrder());
            item.setSortOrder(request.sortOrder());
        }
        if (request.priceMinor() != null) {
            Changes.track(changes, "priceMinor", item.getPriceMinor(), request.priceMinor());
            item.setPriceMinor(request.priceMinor());
        }

        if (changes.isEmpty()) {
            return ItemResponse.from(item, r.getCurrency()); // nothing changed: no write, no audit, no event
        }

        saveOrConflict(() -> itemRepository.saveAndFlush(item), "item", item.getName());

        auditService.record(ownerId, r.getId(), ITEM, item.getId(), "ITEM_UPDATED", changes, null);
        outboxService.publish(ITEM, item.getId(), version(item), "MenuItemUpdated",
                snapshot(item, r.getCurrency()));
        if (item.getPriceMinor() != oldPrice) {
            outboxService.publish(ITEM, item.getId(), version(item), "ItemPriceChanged",
                    priceChanged(item, r.getCurrency(), oldPrice));
        }

        return ItemResponse.from(item, r.getCurrency());
    }

    @Transactional
    public ItemResponse setAvailability(UUID ownerId, UUID id, boolean available) {
        OwnedItem owned = findOwnedItem(ownerId, id);
        Restaurant r = owned.restaurant();
        requireEditable(r);
        MenuItem item = owned.item();
        if (!item.isActive()) {
            throw new MenuNotFoundException("Item not found");
        }

        if (item.isAvailable() == available) {
            return ItemResponse.from(item, r.getCurrency());
        }
        item.setAvailable(available);
        itemRepository.saveAndFlush(item);

        auditService.record(ownerId, r.getId(), ITEM, item.getId(), "ITEM_AVAILABILITY_CHANGED",
                Map.of("available", Map.of("before", String.valueOf(!available),
                        "after", String.valueOf(available))), null);
        outboxService.publish(ITEM, item.getId(), version(item), "ItemAvailabilityChanged", availability(item));

        return ItemResponse.from(item, r.getCurrency());
    }

    @Transactional
    public void archiveItem(UUID ownerId, UUID id) {
        OwnedItem owned = findOwnedItem(ownerId, id);
        Restaurant r = owned.restaurant();
        requireEditable(r);
        MenuItem item = owned.item();

        if (!item.isActive()) {
            return; // already archived
        }
        item.setActive(false);
        itemRepository.saveAndFlush(item);

        auditService.record(ownerId, r.getId(), ITEM, item.getId(), "ITEM_ARCHIVED",
                Map.of("name", item.getName()), null);
        outboxService.publish(ITEM, item.getId(), version(item), "MenuItemUpdated",
                snapshot(item, r.getCurrency())); // the snapshot carries active = false
    }

    // ================================================================== helpers

    /** Someone else's category looks exactly like a missing one. */
    private OwnedCategory findOwnedCategory(UUID ownerId, UUID id) {
        Category c = categoryRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Category not found"));
        Restaurant r = restaurantRepository.findById(c.getRestaurantId())
                .filter(x -> x.getOwnerUserId().equals(ownerId))
                .orElseThrow(() -> new MenuNotFoundException("Category not found"));
        return new OwnedCategory(c, r);
    }

    private OwnedItem findOwnedItem(UUID ownerId, UUID id) {
        MenuItem item = itemRepository.findById(id)
                .orElseThrow(() -> new MenuNotFoundException("Item not found"));
        Restaurant r = restaurantRepository.findById(item.getRestaurantId())
                .filter(x -> x.getOwnerUserId().equals(ownerId))
                .orElseThrow(() -> new MenuNotFoundException("Item not found"));
        return new OwnedItem(item, r);
    }

    /** A request body that points at a category of another restaurant (or an archived one) is a 400. */
    private Category activeCategoryOf(UUID restaurantId, UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .filter(c -> c.getRestaurantId().equals(restaurantId) && c.isActive())
                .orElseThrow(() -> new InvalidRequestException("Category not found in this restaurant"));
    }

    private static void requireEditable(Restaurant r) {
        if (r.getStatus() == RestaurantStatus.Suspended) {
            throw new RestaurantStateException("A suspended restaurant cannot be edited. Please contact support.");
        }
    }

    /** The unique indexes are the real guard: two parallel requests can both pass the pre-check. */
    private static <T> T saveOrConflict(Supplier<T> save, String what, String name) {
        try {
            return save.get();
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateNameException("A " + what + " named '" + name + "' already exists");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}