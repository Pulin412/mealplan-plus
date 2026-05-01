-- Phase 3a: user profile fields (#103) + food favourite flag (#104)

ALTER TABLE users
    ADD COLUMN age            INT,
    ADD COLUMN weight_kg      DOUBLE PRECISION,
    ADD COLUMN height_cm      DOUBLE PRECISION,
    ADD COLUMN gender         VARCHAR(20),
    ADD COLUMN activity_level VARCHAR(30),
    ADD COLUMN target_calories INT,
    ADD COLUMN goal_type      VARCHAR(30);

ALTER TABLE foods
    ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE;
