package com.wagba.restaurants.menu;

import com.wagba.restaurants.common.Timestamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "options")
@Getter
@Setter
@NoArgsConstructor
public class MenuOption extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "extra_price_minor", nullable = false)
    private long extraPriceMinor;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
}