-- V19: workout_sessions — uniqueness guard so re-finishing a session upserts
--      rather than duplicating.
--      logged_meal_slots — slot-level toggle for the Home screen calorie ring.
--      logged_foods remains for individual foods added on top of planned slots.

-- 1. Workout session uniqueness
ALTER TABLE workout_sessions
    ADD CONSTRAINT uq_workout_session_uid_date_name
    UNIQUE (firebase_uid, date, name);

-- 2. Slot-level meal logging
--    One row per (user, date, slot). is_logged toggled from the Home checklist.
--    Calorie ring reads planned kcal from day_plans → diet for checked slots,
--    plus sums logged_foods.kcal_override for any individually added foods.
CREATE TABLE logged_meal_slots (
    id           BIGSERIAL    PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    date         DATE         NOT NULL,
    slot         VARCHAR(50)  NOT NULL,
    is_logged    BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_logged_slot_uid_date_slot UNIQUE (firebase_uid, date, slot)
);

CREATE INDEX idx_logged_meal_slots_uid_date ON logged_meal_slots (firebase_uid, date);
