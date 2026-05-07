-- FK constraints and indexes for child tables that were missing them.
-- workout_sets and template_exercises already have FKs from V3/V6 — skipped here.
--
-- NOTE: If any of these ALTER TABLE statements fail on your database it means
-- orphaned rows exist in that child table. Run the corresponding cleanup query
-- shown in the comment before retrying.

-- ── meal_food_items ───────────────────────────────────────────────────────────
-- Cleanup if needed: DELETE FROM meal_food_items WHERE meal_id NOT IN (SELECT id FROM meals);
-- Cleanup if needed: DELETE FROM meal_food_items WHERE food_id NOT IN (SELECT id FROM foods);
ALTER TABLE meal_food_items
    ADD CONSTRAINT fk_mfi_meal FOREIGN KEY (meal_id) REFERENCES meals(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_mfi_food FOREIGN KEY (food_id) REFERENCES foods(id);

CREATE INDEX idx_mfi_meal_id ON meal_food_items (meal_id);

-- ── diet_meals ────────────────────────────────────────────────────────────────
-- Cleanup if needed: DELETE FROM diet_meals WHERE diet_id NOT IN (SELECT id FROM diets);
-- Cleanup if needed: DELETE FROM diet_meals WHERE meal_id NOT IN (SELECT id FROM meals);
ALTER TABLE diet_meals
    ADD CONSTRAINT fk_dm_diet FOREIGN KEY (diet_id) REFERENCES diets(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_dm_meal FOREIGN KEY (meal_id) REFERENCES meals(id);

CREATE INDEX idx_dm_diet_id ON diet_meals (diet_id);

-- ── logged_foods ──────────────────────────────────────────────────────────────
-- Cleanup if needed: DELETE FROM logged_foods WHERE daily_log_id NOT IN (SELECT id FROM daily_logs);
ALTER TABLE logged_foods
    ADD CONSTRAINT fk_lf_log  FOREIGN KEY (daily_log_id) REFERENCES daily_logs(id) ON DELETE CASCADE,
    ADD CONSTRAINT fk_lf_food FOREIGN KEY (food_id)      REFERENCES foods(id);

CREATE INDEX idx_lf_daily_log_id ON logged_foods (daily_log_id);

-- ── grocery_items ─────────────────────────────────────────────────────────────
-- Cleanup if needed: DELETE FROM grocery_items WHERE grocery_list_id NOT IN (SELECT id FROM grocery_lists);
ALTER TABLE grocery_items
    ADD CONSTRAINT fk_gi_list FOREIGN KEY (grocery_list_id) REFERENCES grocery_lists(id) ON DELETE CASCADE;

CREATE INDEX idx_gi_grocery_list_id ON grocery_items (grocery_list_id);

-- ── Additional query-performance indexes ──────────────────────────────────────
-- Fast tombstone lookup by server_id (used during pull to check if an entity was deleted)
CREATE INDEX idx_tombstones_server_id ON tombstones (server_id);

-- Fast "latest weight / top metric" queries used by DashboardService and HealthService
CREATE INDEX idx_health_uid_type_ts ON health_metrics (firebase_uid, type, recorded_at DESC);
