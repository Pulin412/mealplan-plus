-- V18: day_plans — make diet_id optional (a day can have workouts but no diet).
--      planned_workouts — child table for workouts/activities planned per day.

-- 1. Make diet_id nullable
ALTER TABLE day_plans
    ALTER COLUMN diet_id DROP NOT NULL;

-- 2. Planned workouts per day
--    workout_template_id is optional — NULL means a standalone activity (no template)
--    activity_name is always required as the display label
CREATE TABLE planned_workouts (
    id                   BIGSERIAL    PRIMARY KEY,
    day_plan_id          BIGINT       NOT NULL REFERENCES day_plans(id) ON DELETE CASCADE,
    workout_template_id  BIGINT       REFERENCES workout_templates(id) ON DELETE SET NULL,
    activity_name        VARCHAR(255) NOT NULL
);

CREATE INDEX idx_planned_workouts_day_plan_id ON planned_workouts (day_plan_id);
