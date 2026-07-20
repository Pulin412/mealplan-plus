-- A meal can be tagged with multiple slots (Breakfast, Evening, …) for filtering.
-- Stored as a JSON array in a text column so a meal stays one row.
ALTER TABLE meals ADD COLUMN IF NOT EXISTS slots text NOT NULL DEFAULT '[]';
