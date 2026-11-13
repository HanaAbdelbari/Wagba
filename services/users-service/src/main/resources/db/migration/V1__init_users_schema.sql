-- users-service / users_db : initial schema

CREATE TABLE users (
                       id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name         VARCHAR(150) NOT NULL,
                       email             VARCHAR(254) NOT NULL,
                       phone             VARCHAR(30),
                       password_hash     TEXT         NOT NULL,
                       role              VARCHAR(30)  NOT NULL,
                       status            VARCHAR(20)  NOT NULL DEFAULT 'Active',
                       email_verified_at TIMESTAMPTZ,
                       phone_verified_at TIMESTAMPTZ,
                       version           INTEGER      NOT NULL DEFAULT 0,
                       created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
                       CONSTRAINT ck_users_role   CHECK (role   IN ('Customer', 'RestaurantUser', 'SuperAdmin')),
                       CONSTRAINT ck_users_status CHECK (status IN ('Active', 'Suspended'))
);
CREATE UNIQUE INDEX ux_users_email_lower ON users (lower(email));

CREATE TABLE refresh_tokens (
                                id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                user_id        UUID        NOT NULL REFERENCES users (id),
                                token_hash     TEXT        NOT NULL UNIQUE,
                                family_id      UUID        NOT NULL,
                                expires_at     TIMESTAMPTZ NOT NULL,
                                revoked_at     TIMESTAMPTZ,
                                replaced_by_id UUID REFERENCES refresh_tokens (id),
                                created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user   ON refresh_tokens (user_id);
CREATE INDEX ix_refresh_tokens_family ON refresh_tokens (family_id);