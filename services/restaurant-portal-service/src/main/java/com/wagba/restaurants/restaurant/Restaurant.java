package com.wagba.restaurants.restaurant;

import com.wagba.restaurants.common.Timestamped;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "restaurants")
@Getter
@Setter
@NoArgsConstructor
public class Restaurant extends Timestamped {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_user_id", nullable = false)
    private UUID ownerUserId;

    @Column(nullable = false, length = 150)
    private String name;

    private String description;

    @Column(nullable = false, length = 30)
    private String phone;

    @Column(nullable = false)
    private String address;

    @Column(precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(precision = 9, scale = 6)
    private BigDecimal longitude;

    @Column(nullable = false, length = 60)
    private String timezone = "Africa/Cairo";

    // the column is CHAR(3), so Hibernate must be told it is not a VARCHAR
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String currency = "EGP";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RestaurantStatus status = RestaurantStatus.Pending;

    @Column(name = "accepting_orders", nullable = false)
    private boolean acceptingOrders = true;

    @Column(name = "owner_suspended", nullable = false)
    private boolean ownerSuspended = false;

    @Column(name = "reviewed_by_user_id")
    private UUID reviewedByUserId;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note")
    private String reviewNote;

    @Version
    private Integer version;

    /** Shown to the public: approved, and its owner is not suspended. */
    public boolean isPubliclyVisible() {
        return status == RestaurantStatus.Approved && !ownerSuspended;
    }

    /** Can receive orders right now (working hours are checked separately). */
    public boolean isOpenForOrders() {
        return isPubliclyVisible() && acceptingOrders;
    }
}