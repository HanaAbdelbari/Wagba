package com.wagba.restaurants.menu;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MenuOptionRepository extends JpaRepository<MenuOption, UUID> {

    List<MenuOption> findByGroupIdInAndActiveTrueOrderBySortOrderAscNameAsc(Collection<UUID> groupIds);
}