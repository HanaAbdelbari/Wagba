package com.wagba.restaurants.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface OptionGroupRepository extends JpaRepository<OptionGroup, UUID> {

    List<OptionGroup> findByItemIdInAndActiveTrueOrderBySortOrderAscNameAsc(Collection<UUID> itemIds);
    long countByItemIdAndActiveTrue(UUID itemId);

    boolean existsByItemIdAndNameIgnoreCaseAndActiveTrue(UUID itemId, String name);

    boolean existsByItemIdAndNameIgnoreCaseAndActiveTrueAndIdNot(UUID itemId, String name, UUID id);
}