CREATE TABLE verification_challenges (
                                         id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                         user_id     UUID        NOT NULL REFERENCES users (id),
                                         purpose     VARCHAR(30) NOT NULL,
                                         code_hash   TEXT        NOT NULL,
                                         attempts    INTEGER     NOT NULL DEFAULT 0,
                                         expires_at  TIMESTAMPTZ NOT NULL,
                                         consumed_at TIMESTAMPTZ,
                                         created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
                                         CONSTRAINT ck_challenge_purpose CHECK (purpose IN ('EmailVerification', 'PhoneVerification', 'PasswordReset'))
);
CREATE INDEX ix_challenges_user ON verification_challenges (user_id, purpose);

-- No FK on purpose: audit rows must outlive the accounts they describe.
CREATE TABLE audit_logs (
                            id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            actor_user_id  UUID,
                            target_user_id UUID,
                            action         VARCHAR(100) NOT NULL,
                            changes        JSONB,
                            reason         TEXT,
                            ip_address     VARCHAR(45),
                            user_agent     TEXT,
                            correlation_id UUID,
                            created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_audit_target ON audit_logs (target_user_id, created_at DESC);
CREATE INDEX ix_audit_actor  ON audit_logs (actor_user_id, created_at DESC);

CREATE TABLE outbox_messages (
                                 id                UUID PRIMARY KEY,
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