-- Phase 2: Workout domain tables

CREATE TABLE exercises (
    id           BIGSERIAL    PRIMARY KEY,
    firebase_uid VARCHAR(255),
    name         VARCHAR(255) NOT NULL,
    category     VARCHAR(50)  NOT NULL DEFAULT 'STRENGTH',
    muscle_group VARCHAR(255),
    equipment    VARCHAR(255),
    description  VARCHAR(1000),
    video_link   VARCHAR(500),
    is_system    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL,
    updated_at   TIMESTAMPTZ  NOT NULL,
    server_id    UUID         NOT NULL UNIQUE
);

CREATE TABLE workout_sessions (
    id               BIGSERIAL    PRIMARY KEY,
    firebase_uid     VARCHAR(255) NOT NULL,
    name             VARCHAR(255) NOT NULL,
    date             DATE         NOT NULL,
    duration_minutes INTEGER,
    notes            VARCHAR(1000),
    is_completed     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMPTZ  NOT NULL,
    updated_at       TIMESTAMPTZ  NOT NULL,
    server_id        UUID         NOT NULL UNIQUE
);

CREATE TABLE workout_sets (
    id               BIGSERIAL        PRIMARY KEY,
    session_id       BIGINT           NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id      BIGINT           NOT NULL,
    set_number       INTEGER          NOT NULL DEFAULT 0,
    reps             INTEGER,
    weight_kg        DOUBLE PRECISION,
    duration_seconds INTEGER,
    distance_meters  DOUBLE PRECISION,
    notes            VARCHAR(500)
);

CREATE INDEX idx_exercises_firebase_uid ON exercises(firebase_uid);
CREATE INDEX idx_workout_sessions_firebase_uid ON workout_sessions(firebase_uid);
CREATE INDEX idx_workout_sets_session_id ON workout_sets(session_id);
