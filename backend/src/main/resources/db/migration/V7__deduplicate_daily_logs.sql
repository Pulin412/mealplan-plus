-- Deduplicate daily_logs rows that share the same (firebase_uid, date).
-- Caused by Android sync pushing the same log multiple times without a server-side uniqueness guard.
-- Strategy: keep the highest-id row (most recent upsert) per (firebase_uid, date).

-- 1. Remove logged_foods that belong to the duplicate (lower-id) daily_log rows
DELETE FROM logged_foods
WHERE daily_log_id IN (
    SELECT id
    FROM daily_logs
    WHERE id NOT IN (
        SELECT MAX(id)
        FROM daily_logs
        GROUP BY firebase_uid, date
    )
);

-- 2. Remove the duplicate daily_log rows themselves
DELETE FROM daily_logs
WHERE id NOT IN (
    SELECT MAX(id)
    FROM daily_logs
    GROUP BY firebase_uid, date
);

-- 3. Prevent future duplicates
ALTER TABLE daily_logs
    ADD CONSTRAINT uq_daily_logs_uid_date UNIQUE (firebase_uid, date);
