-- Explicit per-day "marked complete" flag. Presence of a row = the user marked that day done;
-- un-ticking deletes the row. A completed day is the unit the streak counts.
CREATE TABLE day_completions (
    id           BIGSERIAL PRIMARY KEY,
    firebase_uid CHARACTER VARYING(255) NOT NULL,
    date         DATE NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_day_completion UNIQUE (firebase_uid, date)
);

CREATE INDEX idx_day_completion_uid_date ON day_completions (firebase_uid, date);
