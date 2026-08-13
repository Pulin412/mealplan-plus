-- V14: cardio support — per-exercise tracking type + cardio target fields on template sets.
-- Additive only. Existing exercises default to STRENGTH; existing template sets keep NULL
-- cardio fields. (workout_sets already has duration_seconds/distance_meters from V1.)
ALTER TABLE exercises
    ADD COLUMN type varchar(20) NOT NULL DEFAULT 'STRENGTH';

ALTER TABLE template_exercise_sets
    ADD COLUMN duration_seconds integer,
    ADD COLUMN distance_meters double precision;
