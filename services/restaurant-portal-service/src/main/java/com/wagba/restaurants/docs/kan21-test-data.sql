-- review trail on the restaurants themselves
SELECT name, status, reviewed_by_user_id IS NOT NULL AS reviewed, review_note, version
FROM restaurants WHERE name LIKE 'Admin Test%' ORDER BY name;

-- audit trail
SELECT created_at, action, changes, reason, actor_user_id
FROM audit_logs
WHERE action IN ('RESTAURANT_APPROVED','RESTAURANT_REJECTED','RESTAURANT_SUSPENDED','RESTAURANT_REACTIVATED')
ORDER BY created_at;

-- events waiting for the publisher (KAN-25)
SELECT event_type, aggregate_version, payload->>'name' AS name,
    payload->>'previousStatus' AS previous, payload->>'status' AS status,
    payload->>'reason' AS reason, published_at
FROM outbox_messages
WHERE event_type IN ('RestaurantApproved','RestaurantRejected','RestaurantSuspended','RestaurantReactivated')
ORDER BY occurred_at;