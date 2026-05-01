-- Phase 3a #101: Workout templates

CREATE TABLE workout_templates (
    id           BIGSERIAL    PRIMARY KEY,
    firebase_uid VARCHAR(255) NOT NULL,
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(50)  NOT NULL DEFAULT 'STRENGTH',
    notes        VARCHAR(1000),
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE
);

CREATE TABLE template_exercises (
    id               BIGSERIAL  PRIMARY KEY,
    template_id      BIGINT     NOT NULL REFERENCES workout_templates(id) ON DELETE CASCADE,
    exercise_id      BIGINT     NOT NULL,
    order_index      INTEGER    NOT NULL DEFAULT 0,
    target_sets      INTEGER    NOT NULL DEFAULT 3,
    target_reps      INTEGER,
    target_weight_kg DOUBLE PRECISION,
    notes            VARCHAR(500)
);

CREATE INDEX idx_workout_templates_firebase_uid ON workout_templates(firebase_uid);
CREATE INDEX idx_template_exercises_template_id ON template_exercises(template_id);
