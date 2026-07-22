-- Workout templates now store per-set targets (reps + optional weight) instead of a single
-- target_sets / target_reps / target_weight_kg. Mirrors workout_sets (session logging), so the
-- Session Runner's "Copy last" maps 1:1 later.

CREATE TABLE template_exercise_sets (
    id                   BIGSERIAL        PRIMARY KEY,
    template_exercise_id BIGINT           NOT NULL REFERENCES template_exercises(id) ON DELETE CASCADE,
    set_number           INTEGER          NOT NULL DEFAULT 0,
    reps                 INTEGER,
    weight_kg            DOUBLE PRECISION
);

CREATE INDEX idx_template_exercise_sets_te ON template_exercise_sets(template_exercise_id);

-- Expand each existing (target_sets × target_reps @ target_weight_kg) into per-set rows.
INSERT INTO template_exercise_sets (template_exercise_id, set_number, reps, weight_kg)
SELECT te.id, gs.n, te.target_reps, te.target_weight_kg
FROM template_exercises te
CROSS JOIN LATERAL generate_series(0, GREATEST(te.target_sets, 1) - 1) AS gs(n);

ALTER TABLE template_exercises DROP COLUMN target_sets;
ALTER TABLE template_exercises DROP COLUMN target_reps;
ALTER TABLE template_exercises DROP COLUMN target_weight_kg;
