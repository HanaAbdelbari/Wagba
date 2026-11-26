package com.wagba.restaurants.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {

    List<Category> findByRestaurantIdAndActiveTrueOrderBySortOrderAscNameAsc(UUID restaurantId);
    //when adding new category, be sure that category name isn't duplicated
    boolean existsByRestaurantIdAndNameIgnoreCaseAndActiveTrue(UUID restaurantId, String name);

    //when editing existing category ,don't use new name as same as existing category name
    boolean existsByRestaurantIdAndNameIgnoreCaseAndActiveTrueAndIdNot(UUID restaurantId, String name, UUID id);
}