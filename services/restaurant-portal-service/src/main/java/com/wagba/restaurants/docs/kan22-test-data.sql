-- ===================== A) suspend Menu Test R1 =====================
UPDATE restaurants SET status = 'Suspended' WHERE name = 'Menu Test R1';

-- ===================== B) restore =====================
UPDATE restaurants SET status = 'Approved' WHERE name = 'Menu Test R1';
--

-- the menu as stored (archived rows included)
SELECT c.name AS category, c.is_active AS cat_active,
       i.name AS item, i.price_minor, i.is_available, i.is_active AS item_active, i.version
FROM categories c
         LEFT JOIN menu_items i ON i.category_id = c.id
WHERE c.restaurant_id = (SELECT id FROM restaurants WHERE name = 'Menu Test R1' ORDER BY created_at DESC LIMIT 1)
ORDER BY c.sort_order, c.name, i.sort_order;

-- audit trail of the menu
SELECT created_at, action, entity_type, changes
FROM audit_logs
WHERE entity_type IN ('Category', 'MenuItem')
ORDER BY created_at;

-- events waiting for the publisher (KAN-25)
SELECT event_type, aggregate_version, payload->>'name' AS name,
    payload->>'oldPriceMinor' AS old_price, payload->>'newPriceMinor' AS new_price,
    payload->>'available' AS available, payload->>'active' AS active, published_at
FROM outbox_messages
WHERE event_type IN ('MenuItemCreated','MenuItemUpdated','ItemPriceChanged','ItemAvailabilityChanged')
ORDER BY occurred_at;