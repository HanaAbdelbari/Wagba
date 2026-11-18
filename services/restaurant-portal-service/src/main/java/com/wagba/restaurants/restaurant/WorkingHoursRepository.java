package com.wagba.restaurants.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WorkingHoursRepository extends JpaRepository<WorkingHours, UUID> {

    List<WorkingHours> findByRestaurantIdOrderByDayOfWeekAscOpensAtAsc(UUID restaurantId);

    /** Used by PUT /working-hours: the whole schedule is replaced at once. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from WorkingHours w where w.restaurantId = :restaurantId")
    int deleteAllForRestaurant(@Param("restaurantId") UUID restaurantId);
}