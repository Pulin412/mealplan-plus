-- Deduplicate diets and meals caused by Android pushing system records (no serverId)
-- on every sync cycle. Keep the lowest-id row per (firebase_uid, name) as canonical.

-- 1. Remove diet_meal links for duplicate diets
DELETE FROM diet_meals
WHERE diet_id IN (
    SELECT id FROM diets
    WHERE id NOT IN (
        SELECT MIN(id) FROM diets GROUP BY firebase_uid, name
    )
);

-- 2. Remove duplicate diets
DELETE FROM diets
WHERE id NOT IN (
    SELECT MIN(id) FROM diets GROUP BY firebase_uid, name
);

-- 3. Remove meal_food_item links for duplicate meals
DELETE FROM meal_food_items
WHERE meal_id IN (
    SELECT id FROM meals
    WHERE id NOT IN (
        SELECT MIN(id) FROM meals GROUP BY firebase_uid, name
    )
);

-- 4. Remove duplicate meals
DELETE FROM meals
WHERE id NOT IN (
    SELECT MIN(id) FROM meals GROUP BY firebase_uid, name
);
