-- MealPlan+ — initial schema (PostgreSQL)
-- Managed by Flyway; do not edit manually.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    firebase_uid  VARCHAR(255) NOT NULL UNIQUE,
    email         VARCHAR(255),
    display_name  VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL
);

CREATE TABLE foods (
    id               BIGSERIAL PRIMARY KEY,
    firebase_uid     VARCHAR(255),
    name             VARCHAR(255) NOT NULL,
    brand            VARCHAR(255),
    barcode          VARCHAR(255),
    calories_per_100 DOUBLE PRECISION NOT NULL DEFAULT 0,
    protein_per_100  DOUBLE PRECISION NOT NULL DEFAULT 0,
    carbs_per_100    DOUBLE PRECISION NOT NULL DEFAULT 0,
    fat_per_100      DOUBLE PRECISION NOT NULL DEFAULT 0,
    grams_per_piece  DOUBLE PRECISION,
    grams_per_cup    DOUBLE PRECISION,
    grams_per_tbsp   DOUBLE PRECISION,
    grams_per_tsp    DOUBLE PRECISION,
    glycemic_index   INTEGER,
    is_system_food   BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    server_id        UUID         NOT NULL UNIQUE
);

CREATE TABLE meals (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    slot         VARCHAR(50)  NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE
);

CREATE TABLE meal_food_items (
    id       BIGSERIAL PRIMARY KEY,
    meal_id  BIGINT           NOT NULL,
    food_id  BIGINT           NOT NULL,
    quantity DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit     VARCHAR(50)      NOT NULL,
    notes    VARCHAR(500)
);

CREATE TABLE diets (
    id               BIGSERIAL PRIMARY KEY,
    firebase_uid     VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    description      VARCHAR(1000),
    target_calories  DOUBLE PRECISION,
    target_protein   DOUBLE PRECISION,
    target_carbs     DOUBLE PRECISION,
    target_fat       DOUBLE PRECISION,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    server_id        UUID         NOT NULL UNIQUE
);

CREATE TABLE diet_meals (
    id           BIGSERIAL PRIMARY KEY,
    diet_id      BIGINT      NOT NULL,
    meal_id      BIGINT      NOT NULL,
    day_of_week  INTEGER     NOT NULL,
    slot         VARCHAR(50) NOT NULL,
    instructions VARCHAR(1000)
);

CREATE TABLE tags (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE TABLE diet_tag_cross_refs (
    id      BIGSERIAL PRIMARY KEY,
    diet_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL
);

CREATE TABLE grocery_lists (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    diet_id      BIGINT,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE
);

CREATE TABLE grocery_items (
    id               BIGSERIAL PRIMARY KEY,
    grocery_list_id  BIGINT           NOT NULL,
    food_id          BIGINT,
    name             VARCHAR(255)     NOT NULL,
    quantity         DOUBLE PRECISION NOT NULL DEFAULT 1,
    unit             VARCHAR(50)      NOT NULL,
    category         VARCHAR(100),
    done             BOOLEAN          NOT NULL DEFAULT FALSE
);

CREATE TABLE health_metrics (
    id              BIGSERIAL PRIMARY KEY,
    firebase_uid    VARCHAR(255)     NOT NULL,
    type            VARCHAR(100)     NOT NULL,
    sub_type        VARCHAR(100),
    value           DOUBLE PRECISION NOT NULL DEFAULT 0,
    secondary_value DOUBLE PRECISION,
    unit            VARCHAR(50)      NOT NULL,
    recorded_at     TIMESTAMPTZ      NOT NULL,
    created_at      TIMESTAMPTZ      NOT NULL,
    updated_at      TIMESTAMPTZ      NOT NULL,
    server_id       UUID             NOT NULL UNIQUE
);

CREATE TABLE custom_metric_types (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    unit         VARCHAR(50)  NOT NULL,
    icon         VARCHAR(100),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE
);

CREATE TABLE daily_logs (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    date         DATE         NOT NULL,
    notes        VARCHAR(1000),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE
);

CREATE TABLE logged_foods (
    id            BIGSERIAL PRIMARY KEY,
    daily_log_id  BIGINT           NOT NULL,
    food_id       BIGINT           NOT NULL,
    meal_slot     VARCHAR(50)      NOT NULL,
    quantity      DOUBLE PRECISION NOT NULL DEFAULT 0,
    unit          VARCHAR(50)      NOT NULL
);

CREATE TABLE tombstones (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    entity_type  VARCHAR(100) NOT NULL,
    server_id    UUID         NOT NULL,
    deleted_at   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX idx_foods_uid          ON foods         (firebase_uid);
CREATE INDEX idx_meals_uid          ON meals         (firebase_uid);
CREATE INDEX idx_diets_uid          ON diets         (firebase_uid);
CREATE INDEX idx_grocery_lists_uid  ON grocery_lists (firebase_uid);
CREATE INDEX idx_health_metrics_uid ON health_metrics (firebase_uid);
CREATE INDEX idx_daily_logs_uid     ON daily_logs    (firebase_uid);
CREATE INDEX idx_tombstones_uid_ts  ON tombstones    (firebase_uid, deleted_at);
