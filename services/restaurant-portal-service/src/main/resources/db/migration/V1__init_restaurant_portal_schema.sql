-- restaurant-portal-service / restaurant_portal_db : initial schema
--
-- Conventions
--  * ids are UUIDs, timestamps are TIMESTAMPTZ (UTC)
--  * money is BIGINT in minor units (15000 = 150.00 EGP); the currency lives on the restaurant
--  * references to other services (owner_user_id, ...) are plain UUIDs with no foreign key
--  * nothing is deleted: categories, items, groups and options are archived with is_active = false

-- ---------------------------------------------------------------------
-- Restaurants
-- ---------------------------------------------------------------------
CREATE TABLE restaurants (
                             id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             owner_user_id       UUID         NOT NULL,                 -- users-service reference
                             name                VARCHAR(150) NOT NULL,
                             description         TEXT,
                             phone               VARCHAR(30)  NOT NULL,
                             address             TEXT         NOT NULL,
                             latitude            NUMERIC(9,6),
                             longitude           NUMERIC(9,6),
                             timezone            VARCHAR(60)  NOT NULL DEFAULT 'Africa/Cairo',
                             currency            CHAR(3)      NOT NULL DEFAULT 'EGP',
                             status              VARCHAR(20)  NOT NULL DEFAULT 'Pending',
                             accepting_orders    BOOLEAN      NOT NULL DEFAULT TRUE,    -- owner switch, only matters when Approved
                             owner_suspended     BOOLEAN      NOT NULL DEFAULT FALSE,   -- set from users-service events
                             reviewed_by_user_id UUID,
                             reviewed_at         TIMESTAMPTZ,
                             review_note         TEXT,
                             version             INTEGER      NOT NULL DEFAULT 0,
                             created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
                             CONSTRAINT ck_restaurants_status      CHECK (status IN ('Pending', 'Approved', 'Rejected', 'Suspended')),
                             CONSTRAINT ck_restaurants_currency    CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT ck_restaurants_latitude    CHECK (latitude  BETWEEN -90  AND 90),
    CONSTRAINT ck_restaurants_longitude   CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT ck_restaurants_coordinates CHECK ((latitude IS NULL) = (longitude IS NULL))
);
CREATE INDEX ix_restaurants_owner  ON restaurants (owner_user_id);
CREATE INDEX ix_restaurants_status ON restaurants (status);

-- day_of_week follows java.time.DayOfWeek: 1 = Monday ... 7 = Sunday.
-- A restaurant may have several periods per day. No row for a day means closed that day.
-- Overlapping periods are rejected by the application, not by this table.
--ليه عملنا جدول منفصل للمواعيد بدل ما نحطها في جدول restaurants؟
--المطعم ممكن يكون عنده مواعيد مختلفة لكل يوم، وممكن يكون عنده أكتر من فترة في نفس اليوم.
CREATE TABLE restaurant_working_hours (
                                          id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                          restaurant_id   UUID     NOT NULL REFERENCES restaurants (id),
                                          day_of_week     SMALLINT NOT NULL,
                                          opens_at        TIME     NOT NULL,
                                          closes_at       TIME     NOT NULL,
                                          closes_next_day BOOLEAN  NOT NULL DEFAULT FALSE,
                                          CONSTRAINT ck_hours_day   CHECK (day_of_week BETWEEN 1 AND 7),
                                          CONSTRAINT ck_hours_range CHECK (
                                              (closes_next_day AND closes_at <= opens_at) OR
                                              (NOT closes_next_day AND closes_at > opens_at))
);
CREATE INDEX ix_hours_restaurant ON restaurant_working_hours (restaurant_id, day_of_week);

-- ---------------------------------------------------------------------
-- Menu
-- ---------------------------------------------------------------------
CREATE TABLE categories (
                            id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            restaurant_id UUID         NOT NULL REFERENCES restaurants (id),
                            name          VARCHAR(100) NOT NULL,
                            sort_order    INTEGER      NOT NULL DEFAULT 0,
                            is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
                            created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- needed by the composite foreign key in menu_items
                            CONSTRAINT uq_categories_id_restaurant UNIQUE (id, restaurant_id)
);
CREATE UNIQUE INDEX ux_categories_active_name ON categories (restaurant_id, lower(name)) WHERE is_active;
CREATE INDEX ix_categories_restaurant ON categories (restaurant_id, sort_order);

CREATE TABLE menu_items (
                            id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            restaurant_id UUID         NOT NULL,
                            category_id   UUID         NOT NULL,
                            name          VARCHAR(150) NOT NULL,
                            description   TEXT,
                            price_minor   BIGINT       NOT NULL,
                            image_url     TEXT,
                            sort_order    INTEGER      NOT NULL DEFAULT 0,
                            is_available  BOOLEAN      NOT NULL DEFAULT TRUE,   -- temporarily out of stock
                            is_active     BOOLEAN      NOT NULL DEFAULT TRUE,   -- false = archived
                            version       INTEGER      NOT NULL DEFAULT 0,
                            created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
                            CONSTRAINT ck_items_price CHECK (price_minor >= 0),
                            CONSTRAINT fk_items_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurants (id),
    -- the category must belong to the SAME restaurant as the item
                            CONSTRAINT fk_items_category FOREIGN KEY (category_id, restaurant_id)
                                REFERENCES categories (id, restaurant_id)
);
CREATE UNIQUE INDEX ux_items_active_name ON menu_items (category_id, lower(name)) WHERE is_active;
CREATE INDEX ix_items_restaurant ON menu_items (restaurant_id);
CREATE INDEX ix_items_category   ON menu_items (category_id, sort_order);

CREATE TABLE option_groups (
                               id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               item_id        UUID         NOT NULL REFERENCES menu_items (id),
                               name           VARCHAR(100) NOT NULL,                  -- Size / Extras
                               min_selections INTEGER      NOT NULL DEFAULT 0,        -- min > 0 means the choice is required
                               max_selections INTEGER      NOT NULL DEFAULT 1,
                               sort_order     INTEGER      NOT NULL DEFAULT 0,
                               is_active      BOOLEAN      NOT NULL DEFAULT TRUE,
                               created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               updated_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                               CONSTRAINT ck_groups_selections CHECK (
                                   min_selections >= 0 AND max_selections >= 1 AND max_selections >= min_selections)
);
CREATE INDEX ix_option_groups_item ON option_groups (item_id, sort_order);

CREATE TABLE options (
                         id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         group_id          UUID         NOT NULL REFERENCES option_groups (id),
                         name              VARCHAR(100) NOT NULL,
                         extra_price_minor BIGINT       NOT NULL DEFAULT 0,
                         sort_order        INTEGER      NOT NULL DEFAULT 0,
                         is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
                         created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
                         CONSTRAINT ck_options_price CHECK (extra_price_minor >= 0)
);
CREATE INDEX ix_options_group ON options (group_id, sort_order);

-- ---------------------------------------------------------------------
-- Audit log (no foreign keys: rows must outlive what they describe)
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
                            id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            actor_user_id  UUID,
                            restaurant_id  UUID,
                            entity_type    VARCHAR(50)  NOT NULL,                  -- Restaurant / Category / MenuItem ...
                            entity_id      UUID         NOT NULL,
                            action         VARCHAR(100) NOT NULL,                  -- RESTAURANT_APPROVED, ITEM_PRICE_CHANGED ...
                            changes        JSONB,
                            reason         TEXT,
                            ip_address     VARCHAR(45),
                            user_agent     TEXT,
                            correlation_id UUID,
                            created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_restaurant ON audit_logs (restaurant_id, created_at DESC);
CREATE INDEX ix_audit_entity     ON audit_logs (entity_type, entity_id);
CREATE INDEX ix_audit_actor      ON audit_logs (actor_user_id, created_at DESC);

-- ---------------------------------------------------------------------
-- Messaging
-- ---------------------------------------------------------------------
CREATE TABLE outbox_messages (
                                 id                UUID PRIMARY KEY,                    -- = eventId
                                 aggregate_type    VARCHAR(100) NOT NULL,
                                 aggregate_id      UUID         NOT NULL,
                                 aggregate_version INTEGER      NOT NULL,
                                 event_type        VARCHAR(150) NOT NULL,
                                 event_version     INTEGER      NOT NULL DEFAULT 1,
                                 correlation_id    UUID,
                                 payload           JSONB        NOT NULL,
                                 occurred_at       TIMESTAMPTZ  NOT NULL,
                                 published_at      TIMESTAMPTZ,
                                 attempt_count     INTEGER      NOT NULL DEFAULT 0,
                                 next_attempt_at   TIMESTAMPTZ
);
CREATE INDEX ix_outbox_unpublished ON outbox_messages (next_attempt_at) WHERE published_at IS NULL;

-- Idempotent consumers: an event that was already handled is skipped
CREATE TABLE processed_messages (
                                    consumer_name VARCHAR(150) NOT NULL,
                                    event_id      UUID         NOT NULL,
                                    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                    PRIMARY KEY (consumer_name, event_id)
);