-- =====================================================================
-- Wagba / users-service : development SQL cheat sheet
-- =====================================================================
-- Run everything under sections 2-9 against the users_db database.
-- Section 1 must be run against the default "postgres" database.
-- Run statements one at a time (select it, then F5 in pgAdmin).
-- Anything marked DANGEROUS is commented out on purpose.
-- =====================================================================


-- =====================================================================
-- 1. ONE-TIME SETUP  (connect to database: postgres, NOT users_db)
-- =====================================================================
-- Run each statement separately. CREATE DATABASE cannot run inside a
-- transaction block together with other statements in some clients.

CREATE ROLE users_user LOGIN PASSWORD 'dev_password';
CREATE DATABASE users_db OWNER users_user;

-- Roles and databases for the other services. Create each one only when
-- you start building that service.
--
-- CREATE ROLE restaurant_user    LOGIN PASSWORD 'dev_password';
-- CREATE DATABASE restaurant_portal_db OWNER restaurant_user;
--
-- CREATE ROLE orders_user        LOGIN PASSWORD 'dev_password';
-- CREATE DATABASE orders_db OWNER orders_user;
--
-- CREATE ROLE payments_user      LOGIN PASSWORD 'dev_password';
-- CREATE DATABASE payments_db OWNER payments_user;
--
-- CREATE ROLE gateway_sim_user   LOGIN PASSWORD 'dev_password';
-- CREATE DATABASE gateway_simulator_db OWNER gateway_sim_user;
--
-- CREATE ROLE notifications_user LOGIN PASSWORD 'dev_password';
-- CREATE DATABASE notifications_db OWNER notifications_user;
--
-- CREATE ROLE reporting_user     LOGIN PASSWORD 'dev_password';
-- CREATE DATABASE reporting_db OWNER reporting_user;


-- =====================================================================
-- 2. SCHEMA / FLYWAY CHECKS
-- =====================================================================
-- (The schema itself is created by Flyway from
--  src/main/resources/db/migration/V1__..., V2__... Do not run it by hand.)

-- Which migrations did Flyway apply, and did they succeed?
SELECT installed_rank, version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- List all tables in the schema
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- Row count per table (quick overview)
SELECT 'users' AS table_name, count(*) FROM users
UNION ALL SELECT 'refresh_tokens',          count(*) FROM refresh_tokens
UNION ALL SELECT 'verification_challenges', count(*) FROM verification_challenges
UNION ALL SELECT 'audit_logs',              count(*) FROM audit_logs
UNION ALL SELECT 'outbox_messages',         count(*) FROM outbox_messages;


-- =====================================================================
-- 3. USERS
-- =====================================================================

-- All users. password_hash is shortened: a BCrypt hash starts with $2a$10
SELECT id, full_name, email, role, status,
       email_verified_at IS NOT NULL AS email_verified,
    left(password_hash, 7)        AS hash_start,
    created_at
FROM users
ORDER BY created_at DESC;

-- Suspend a user by hand (normally done through the admin API)
UPDATE users SET status = 'Suspended' WHERE email = 'hana@example.com';

-- Reactivate
UPDATE users SET status = 'Active' WHERE email = 'hana@example.com';

-- Put a user back to Customer by hand
-- UPDATE users SET role = 'Customer' WHERE email = 'hana@example.com';


-- =====================================================================
-- 4. REFRESH TOKENS / SESSIONS
-- =====================================================================
-- Each login creates a new family_id (one session). Each /auth/refresh
-- stays inside the same family: the old token is revoked, a new one is added.

-- All tokens (only a hash prefix is shown)
SELECT left(token_hash, 8)         AS token,
    family_id,
    revoked_at IS NOT NULL      AS revoked,
    replaced_by_id IS NOT NULL  AS replaced,
    expires_at,
    created_at
FROM refresh_tokens
ORDER BY created_at;

-- One row per session, and whether it is still active
SELECT family_id,
       count(*)                      AS tokens,
       bool_or(revoked_at IS NULL)   AS active
FROM refresh_tokens
GROUP BY family_id;

-- Active sessions per user
SELECT u.email, count(DISTINCT t.family_id) AS active_sessions
FROM refresh_tokens t
         JOIN users u ON u.id = t.user_id
WHERE t.revoked_at IS NULL AND t.expires_at > now()
GROUP BY u.email;


-- =====================================================================
-- 5. AUDIT LOG
-- =====================================================================
-- Actions: USER_CREATED, USER_SUSPENDED, USER_REACTIVATED,
-- USER_ROLE_CHANGED, PASSWORD_CHANGED, ALL_SESSIONS_REVOKED,
-- PASSWORD_RESET_REQUESTED, PASSWORD_RESET_COMPLETED, EMAIL_VERIFIED

SELECT created_at, action, actor_user_id, target_user_id,
    changes, reason, ip_address, correlation_id
FROM audit_logs
ORDER BY created_at DESC;

-- Audit trail of a single user
-- SELECT created_at, action, changes, reason
-- FROM audit_logs
-- WHERE target_user_id = 'PASTE_USER_ID_HERE'
-- ORDER BY created_at DESC;


-- =====================================================================
-- 6. OUTBOX (events waiting to be published to RabbitMQ)
-- =====================================================================
-- published_at IS NULL  -> not sent yet (or RabbitMQ was down)
-- attempt_count         -> how many times publishing failed

SELECT event_type, aggregate_version,
       published_at IS NOT NULL AS published,
       attempt_count, next_attempt_at, occurred_at
FROM outbox_messages
ORDER BY occurred_at DESC;

-- Events that are stuck
SELECT event_type, attempt_count, next_attempt_at, occurred_at
FROM outbox_messages
WHERE published_at IS NULL
ORDER BY occurred_at;

-- Retry stuck events right away instead of waiting for the backoff
UPDATE outbox_messages
SET next_attempt_at = NULL, attempt_count = 0
WHERE published_at IS NULL;

-- Compare the correlation id of an audit row with its outbox event
-- (they should match for the same HTTP request)
SELECT a.action, a.correlation_id AS audit_correlation,
       o.event_type, o.correlation_id AS outbox_correlation
FROM audit_logs a
         JOIN outbox_messages o ON o.correlation_id = a.correlation_id
ORDER BY a.created_at DESC;


-- =====================================================================
-- 7. VERIFICATION CODES (password reset / email verification)
-- =====================================================================
-- There is no email service yet, so read the code from the event payload.
-- DEVELOPMENT ONLY. Never expose codes like this in production.

SELECT event_type, payload->>'email' AS email, payload->>'code' AS code, occurred_at
FROM outbox_messages
WHERE event_type IN ('PasswordResetRequested', 'EmailVerificationRequested')
ORDER BY occurred_at DESC
    LIMIT 5;

-- State of the stored challenges (the code itself is stored only as an HMAC)
SELECT purpose, attempts,
       consumed_at IS NOT NULL AS consumed,
       expires_at, created_at
FROM verification_challenges
ORDER BY created_at DESC;


-- =====================================================================
-- 8. HANDY CHECKS FROM THE TESTS
-- =====================================================================

-- Is the first SuperAdmin there?
SELECT email, role, status FROM users WHERE role = 'SuperAdmin';

-- Is every password stored as a BCrypt hash? (should return 0 rows)
SELECT email FROM users WHERE password_hash NOT LIKE '$2%';


-- =====================================================================
-- 9. CLEANUP  (DANGEROUS - commented out on purpose)
-- =====================================================================

-- Delete ONE test user and everything that points at it.
-- Order matters because of the foreign keys.
-- DELETE FROM verification_challenges WHERE user_id = (SELECT id FROM users WHERE email = 'verify1@example.com');
-- DELETE FROM refresh_tokens          WHERE user_id = (SELECT id FROM users WHERE email = 'verify1@example.com');
-- DELETE FROM users                   WHERE email = 'verify1@example.com';

-- Wipe all test data but keep the schema.
-- Afterwards the SuperAdmin is recreated at next startup if the
-- WAGBA_ADMIN_EMAIL / WAGBA_ADMIN_PASSWORD environment variables are set.
-- TRUNCATE verification_challenges, refresh_tokens, audit_logs, outbox_messages, users;

-- Start completely from zero (drops every table, including flyway history).
-- Flyway recreates the schema on next startup.
-- Run it against users_db only, never against any other database.
-- DROP SCHEMA public CASCADE;
-- CREATE SCHEMA public;