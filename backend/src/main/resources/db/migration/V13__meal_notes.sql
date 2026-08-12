-- V13: Free-text notes on a meal (parity with diets, which already have `description`).
-- Postgres/prod only — the H2 dev profile builds its schema from JPA entities.
-- Additive + nullable: existing meals keep NULL (no note).
ALTER TABLE meals
    ADD COLUMN notes text;
