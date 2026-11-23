-- ===================== A) approve Burger Town =====================
UPDATE restaurants
SET status = 'Approved', reviewed_at = now()
WHERE name = 'Burger Town';


-- ===================== B) menu test data for Burger Town =====================
DO $$
DECLARE
r            UUID;
    pizza        UUID;
    drinks       UUID;
    archived_cat UUID;
    margherita   UUID;
    size_group   UUID;
BEGIN
SELECT id INTO r FROM restaurants WHERE name = 'Burger Town' ORDER BY created_at DESC LIMIT 1;

INSERT INTO categories (restaurant_id, name, sort_order) VALUES (r, 'Pizza', 1)    RETURNING id INTO pizza;
INSERT INTO categories (restaurant_id, name, sort_order) VALUES (r, 'Drinks', 2)   RETURNING id INTO drinks;
INSERT INTO categories (restaurant_id, name, sort_order) VALUES (r, 'Desserts', 3);               -- empty on purpose
INSERT INTO categories (restaurant_id, name, sort_order, is_active)
VALUES (r, 'Old Menu', 4, FALSE) RETURNING id INTO archived_cat;                                   -- archived category

INSERT INTO menu_items (restaurant_id, category_id, name, price_minor, sort_order)
VALUES (r, pizza, 'Margherita', 15000, 1) RETURNING id INTO margherita;
INSERT INTO menu_items (restaurant_id, category_id, name, price_minor, sort_order, is_available)
VALUES (r, pizza, 'Pepperoni', 18000, 2, FALSE);                                                   -- sold out
INSERT INTO menu_items (restaurant_id, category_id, name, price_minor, sort_order, is_active)
VALUES (r, pizza, 'Old Special', 9900, 3, FALSE);                                                  -- archived item
INSERT INTO menu_items (restaurant_id, category_id, name, price_minor)
VALUES (r, archived_cat, 'Ghost', 5000);                                                           -- active item, archived category
INSERT INTO menu_items (restaurant_id, category_id, name, price_minor)
VALUES (r, drinks, 'Cola', 3000);

INSERT INTO option_groups (item_id, name, min_selections, max_selections)
VALUES (margherita, 'Size', 1, 1) RETURNING id INTO size_group;
INSERT INTO options (group_id, name, extra_price_minor, sort_order) VALUES (size_group, 'Small', 0, 1);
INSERT INTO options (group_id, name, extra_price_minor, sort_order) VALUES (size_group, 'Large', 2500, 2);
INSERT INTO options (group_id, name, extra_price_minor, sort_order, is_active)
VALUES (size_group, 'Huge', 5000, 3, FALSE);                                                       -- archived option
END $$;


-- ===================== C) suspend Burger Town =====================
UPDATE restaurants SET status = 'Suspended' WHERE name = 'Burger Town';


-- ===================== D) approve again, then suspend its OWNER =====================
UPDATE restaurants SET status = 'Approved' WHERE name = 'Burger Town';
UPDATE restaurants SET owner_suspended = TRUE WHERE name = 'Burger Town';


-- ===================== E) restore =====================
UPDATE restaurants SET owner_suspended = FALSE WHERE name = 'Burger Town';