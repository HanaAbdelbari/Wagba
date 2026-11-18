package com.wagba.restaurants.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID restaurantId);

    /** Used by the quote API: loads the requested items, but only those of this restaurant. */
    List<MenuItem> findByIdInAndRestaurantId(Collection<UUID> ids, UUID restaurantId);
}