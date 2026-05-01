-- Phase 3a #99: Day plans — stores which diet is assigned to each date per user

CREATE TABLE day_plans (
    id           BIGSERIAL    PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    date         DATE         NOT NULL,
    diet_id      BIGINT       NOT NULL REFERENCES diets(id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE,
    CONSTRAINT uq_day_plans_uid_date UNIQUE (firebase_uid, date)
);

CREATE INDEX idx_day_plans_firebase_uid ON day_plans(firebase_uid);
CREATE INDEX idx_day_plans_date         ON day_plans(date);
