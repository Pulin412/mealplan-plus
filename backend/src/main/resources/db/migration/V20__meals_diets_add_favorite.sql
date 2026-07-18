-- V20: Add is_favorite to meals and diets.
--      Foods already have this column (V5). Meals/diets follow the same pattern:
--      per-user rows so is_favorite lives directly on the entity row.

ALTER TABLE meals ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE diets ADD COLUMN is_favorite BOOLEAN NOT NULL DEFAULT FALSE;
