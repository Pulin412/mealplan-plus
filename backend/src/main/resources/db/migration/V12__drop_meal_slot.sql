-- CR-11: meals.slot is redundant — slot context lives on diet_meals / logged_foods.meal_slot.
-- A Meal is a reusable food collection; which slot it fills is context, not identity.
ALTER TABLE meals DROP COLUMN IF EXISTS slot;
