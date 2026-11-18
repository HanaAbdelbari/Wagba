package com.wagba.restaurants.menu;

import com.wagba.restaurants.common.Timestamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "menu_items")
@Getter
@Setter
@NoArgsConstructor
public class MenuItem extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, length = 150)
    private String name;

    private String description;

    /** Minor units: 15000 = 150.00. The currency belongs to the restaurant. */
    @Column(name = "price_minor", nullable = false)
    private long priceMinor;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** Temporarily out of stock. */
    @Column(name = "is_available", nullable = false)
    private boolean available = true;

    /** false = archived. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Version
    private Integer version;
}