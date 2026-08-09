-- Per-exercise notes within a workout session: one free-text note-to-self per (session, exercise),
-- kept across repeats and surfaced on the next "copy last" (never copied with sets/reps).
CREATE TABLE workout_session_exercise_notes (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_id BIGINT NOT NULL,
    note        TEXT,
    CONSTRAINT uq_wsen_session_exercise UNIQUE (session_id, exercise_id)
);

CREATE INDEX idx_wsen_session ON workout_session_exercise_notes(session_id);
