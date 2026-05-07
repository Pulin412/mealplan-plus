-- Per-user food favorites table.
--
-- Previously, isFavorite was a single column on foods, meaning any user
-- favoriting a system food (shared across all users) would set it for everyone.
-- This table fixes that: system food favorites are stored here, keyed by
-- (firebase_uid, food_id). User-owned food favorites still use foods.is_favorite
-- since those rows already belong to exactly one user.

CREATE TABLE food_user_prefs (
    firebase_uid VARCHAR(255) NOT NULL,
    food_id      BIGINT       NOT NULL REFERENCES foods(id) ON DELETE CASCADE,
    is_favorite  BOOLEAN      NOT NULL DEFAULT FALSE,
    PRIMARY KEY (firebase_uid, food_id)
);

CREATE INDEX idx_fup_firebase_uid ON food_user_prefs (firebase_uid);

-- Reset is_favorite on all system foods to false.
-- We cannot know which user set them, so they start fresh.
-- Each user's favorites will be re-populated as they interact with the app.
UPDATE foods SET is_favorite = FALSE WHERE is_system_food = TRUE;
