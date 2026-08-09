-- When a whole meal is logged to a day (from the Home "Add to today" picker), its foods share this
-- name so the client can group them back into one collapsible meal row. Null for single foods.
ALTER TABLE logged_foods ADD COLUMN meal_name VARCHAR(255);
