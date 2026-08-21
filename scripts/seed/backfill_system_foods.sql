-- V18 backfill: extra per-100g nutrients for the shared system-food catalog.
-- Applied DIRECTLY to prod Neon (not a Flyway migration) on 2026-08-21.
-- Source: USDA FoodData Central (Foundation/SR Legacy) + curated overrides (data/system_food_nutrients_overrides.json).
-- Idempotent & non-destructive: COALESCE only fills columns still NULL; safe to re-run.
-- 204 UPDATEs (199 system foods filled; 5 catalog-only supplements no-op). 17 rows use curated raw
-- values because FDC had matched prepared/cooked dishes (e.g. Garlic->garlic butter, fish->dishes).

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.2),
    sodium_per100        = COALESCE(sodium_per100, 0.129)
  WHERE is_system_food = true AND name = 'Egg Whole';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.71),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.166)
  WHERE is_system_food = true AND name = 'Egg White';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.7),
    sugars_per100        = COALESCE(sugars_per100, 5.8),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.042),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Onion';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.2),
    sugars_per100        = COALESCE(sugars_per100, 2.63),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.028),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Tomato';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 27.2),
    sugars_per100        = COALESCE(sugars_per100, 10.3),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.26),
    sodium_per100        = COALESCE(sodium_per100, 0.03)
  WHERE is_system_food = true AND name = 'Capsicum';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.1),
    sugars_per100        = COALESCE(sugars_per100, 4.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.03),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Red Bell Pepper';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.07),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, NULL),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Yellow Bell Pepper';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 60.0),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Butter';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.3),
    sugars_per100        = COALESCE(sugars_per100, 0.94),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.445),
    sodium_per100        = COALESCE(sodium_per100, 0.598)
  WHERE is_system_food = true AND name = 'BB Toast';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.04),
    sugars_per100        = COALESCE(sugars_per100, 12.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, NULL),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Red Apple';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.8),
    sugars_per100        = COALESCE(sugars_per100, 24.17),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.64),
    sodium_per100        = COALESCE(sodium_per100, 0.062)
  WHERE is_system_food = true AND name = 'Green Apple';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.4),
    sugars_per100        = COALESCE(sugars_per100, 6.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.016),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Watermelon';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 9.6),
    sugars_per100        = COALESCE(sugars_per100, 4.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 4.225),
    sodium_per100        = COALESCE(sodium_per100, 0.232)
  WHERE is_system_food = true AND name = 'Almonds';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.0),
    sugars_per100        = COALESCE(sugars_per100, 9.09),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 10.606),
    sodium_per100        = COALESCE(sodium_per100, 0.295)
  WHERE is_system_food = true AND name = 'Cashew';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 9.1),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Walnuts';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 10.6),
    sugars_per100        = COALESCE(sugars_per100, 7.66),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 5.91),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Pista';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.9),
    sugars_per100        = COALESCE(sugars_per100, 4.75),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.028),
    sodium_per100        = COALESCE(sodium_per100, 0.075)
  WHERE is_system_food = true AND name = 'Carrot';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.2),
    sugars_per100        = COALESCE(sugars_per100, 0.82),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.026),
    sodium_per100        = COALESCE(sodium_per100, 0.006)
  WHERE is_system_food = true AND name = 'Potato';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 5.7),
    sugars_per100        = COALESCE(sugars_per100, 5.67),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.071),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Matar';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.7),
    sugars_per100        = COALESCE(sugars_per100, 0.21),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.603),
    sodium_per100        = COALESCE(sodium_per100, 0.231)
  WHERE is_system_food = true AND name = 'White Rice';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 15.52),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Olive Oil';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 11.6),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Mustard Oil';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 61.924),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Desi Ghee';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.5),
    sugars_per100        = COALESCE(sugars_per100, 16.07),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.085),
    sodium_per100        = COALESCE(sodium_per100, 2.055)
  WHERE is_system_food = true AND name = 'Hot & Sweet Chilli Sauce';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.3),
    sugars_per100        = COALESCE(sugars_per100, 21.54),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.014),
    sodium_per100        = COALESCE(sodium_per100, 0.928)
  WHERE is_system_food = true AND name = 'Tomato Ketchup';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.9),
    sugars_per100        = COALESCE(sugars_per100, 1.89),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.81),
    sodium_per100        = COALESCE(sodium_per100, 0.242)
  WHERE is_system_food = true AND name = 'Paneer';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.5),
    sugars_per100        = COALESCE(sugars_per100, 0.19),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.026),
    sodium_per100        = COALESCE(sodium_per100, 0.135)
  WHERE is_system_food = true AND name = 'Corn';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.2),
    sugars_per100        = COALESCE(sugars_per100, 1.24),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.259),
    sodium_per100        = COALESCE(sodium_per100, 0.035)
  WHERE is_system_food = true AND name = 'Tofu';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.9),
    sugars_per100        = COALESCE(sugars_per100, 22.72),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.173),
    sodium_per100        = COALESCE(sodium_per100, 0.29)
  WHERE is_system_food = true AND name = 'Zucchini';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 9.9),
    sugars_per100        = COALESCE(sugars_per100, 47.3),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.698),
    sodium_per100        = COALESCE(sodium_per100, 0.003)
  WHERE is_system_food = true AND name = 'Banana';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.4),
    sugars_per100        = COALESCE(sugars_per100, 9.36),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.028),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Blueberry';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.8),
    sugars_per100        = COALESCE(sugars_per100, 4.86),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Strawberry';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.0),
    sugars_per100        = COALESCE(sugars_per100, 9.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 10.0),
    sodium_per100        = COALESCE(sodium_per100, 0.43)
  WHERE is_system_food = true AND name = 'Peanut Butter';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 1.06),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.067),
    sodium_per100        = COALESCE(sodium_per100, 0.498)
  WHERE is_system_food = true AND name = 'Cabbage';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 13.1),
    sugars_per100        = COALESCE(sugars_per100, 1.02),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.43),
    sodium_per100        = COALESCE(sodium_per100, 0.003)
  WHERE is_system_food = true AND name = 'Wheat Flour';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 10.8),
    sugars_per100        = COALESCE(sugars_per100, 10.8),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.65),
    sodium_per100        = COALESCE(sodium_per100, 0.064)
  WHERE is_system_food = true AND name = 'Gram Flour';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.7),
    sugars_per100        = COALESCE(sugars_per100, 0.27),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.155),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Maida';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.5),
    sugars_per100        = COALESCE(sugars_per100, 2.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.0),
    sodium_per100        = COALESCE(sodium_per100, 0.9)
  WHERE is_system_food = true AND name = 'Large Tortilla';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 1.08),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.816),
    sodium_per100        = COALESCE(sodium_per100, 0.326)
  WHERE is_system_food = true AND name = 'Cauliflower';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 1.27),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.789),
    sodium_per100        = COALESCE(sodium_per100, 0.317)
  WHERE is_system_food = true AND name = 'Mushroom';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 25.2),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.221),
    sodium_per100        = COALESCE(sodium_per100, 0.018)
  WHERE is_system_food = true AND name = 'French Beans';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.1),
    sugars_per100        = COALESCE(sugars_per100, 4.46),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.12),
    sodium_per100        = COALESCE(sodium_per100, 0.222)
  WHERE is_system_food = true AND name = 'Chickpea';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.1),
    sugars_per100        = COALESCE(sugars_per100, 4.46),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.12),
    sodium_per100        = COALESCE(sodium_per100, 0.222)
  WHERE is_system_food = true AND name = 'Black Chickpea';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.9),
    sugars_per100        = COALESCE(sugars_per100, 7.86),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.046),
    sodium_per100        = COALESCE(sodium_per100, 0.016)
  WHERE is_system_food = true AND name = 'Kharbooja';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.3),
    sugars_per100        = COALESCE(sugars_per100, 15.55),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.02),
    sodium_per100        = COALESCE(sodium_per100, 0.013)
  WHERE is_system_food = true AND name = 'Cantaloupe';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.8),
    sugars_per100        = COALESCE(sugars_per100, 0.56),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.175),
    sodium_per100        = COALESCE(sodium_per100, 0.232)
  WHERE is_system_food = true AND name = 'Pasta Penne';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.8),
    sugars_per100        = COALESCE(sugars_per100, 5.5),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.17),
    sodium_per100        = COALESCE(sodium_per100, 0.419)
  WHERE is_system_food = true AND name = 'Spaghetti';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.1),
    sugars_per100        = COALESCE(sugars_per100, 9.73),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.022),
    sodium_per100        = COALESCE(sodium_per100, 0.003)
  WHERE is_system_food = true AND name = 'Pear';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.0),
    sugars_per100        = COALESCE(sugars_per100, 8.99),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.029),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Kiwi';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.9),
    sugars_per100        = COALESCE(sugars_per100, 16.74),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.054),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Grapes';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.5),
    sugars_per100        = COALESCE(sugars_per100, 1.71),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.508),
    sodium_per100        = COALESCE(sodium_per100, 0.309)
  WHERE is_system_food = true AND name = 'Yellow Moong Dal';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 16.3),
    sugars_per100        = COALESCE(sugars_per100, 6.6),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.348),
    sodium_per100        = COALESCE(sodium_per100, 0.015)
  WHERE is_system_food = true AND name = 'Green Moong';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, NULL),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 16.2),
    sodium_per100        = COALESCE(sodium_per100, 0.601)
  WHERE is_system_food = true AND name = 'Cheese Slice';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.3),
    sugars_per100        = COALESCE(sugars_per100, 1.1),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.026),
    sodium_per100        = COALESCE(sodium_per100, 0.023)
  WHERE is_system_food = true AND name = 'Lettuce';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.5),
    sugars_per100        = COALESCE(sugars_per100, 5.33),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.027),
    sodium_per100        = COALESCE(sodium_per100, 0.236)
  WHERE is_system_food = true AND name = 'Cucumber';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.8),
    sugars_per100        = COALESCE(sugars_per100, 6.76),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.027),
    sodium_per100        = COALESCE(sodium_per100, 0.078)
  WHERE is_system_food = true AND name = 'Beetroot';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.2),
    sugars_per100        = COALESCE(sugars_per100, 0.76),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.296),
    sodium_per100        = COALESCE(sodium_per100, 0.45)
  WHERE is_system_food = true AND name = 'Spinach';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.5),
    sugars_per100        = COALESCE(sugars_per100, 2.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.006),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Lauki';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.2),
    sugars_per100        = COALESCE(sugars_per100, 1.48),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.026),
    sodium_per100        = COALESCE(sodium_per100, 0.007)
  WHERE is_system_food = true AND name = 'Bhindi';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, NULL),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.349),
    sodium_per100        = COALESCE(sodium_per100, 0.066)
  WHERE is_system_food = true AND name = 'Boneless Chicken';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 8.0),
    sugars_per100        = COALESCE(sugars_per100, 63.35),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.032),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Black Dates';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 13.66),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.092),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Mango';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 2.5),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.083),
    sodium_per100        = COALESCE(sodium_per100, 0.019)
  WHERE is_system_food = true AND name = 'Milk';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 0.3),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.041),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Fresh Cream';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 34.4),
    sugars_per100        = COALESCE(sugars_per100, 1.55),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.33),
    sodium_per100        = COALESCE(sodium_per100, 0.016)
  WHERE is_system_food = true AND name = 'Chia Seeds';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.4),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.019),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Ragi Flour';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 30.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.5),
    sodium_per100        = COALESCE(sodium_per100, 0.05)
  WHERE is_system_food = true AND name = 'Ashwagandha';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 9.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.008),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Vitamin E (Evion 400)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.02)
  WHERE is_system_food = true AND name = 'Shilajeet';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 22.6),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Sea Cod Liver Oil';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.1),
    sugars_per100        = COALESCE(sugars_per100, 9.94),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.616),
    sodium_per100        = COALESCE(sodium_per100, 0.065)
  WHERE is_system_food = true AND name = 'Vitamin D3';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.002),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Black Coffee';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Luke Warm Water';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.1),
    sugars_per100        = COALESCE(sugars_per100, 6.97),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.121),
    sodium_per100        = COALESCE(sodium_per100, 0.056)
  WHERE is_system_food = true AND name = 'Almond Milk';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 1.16),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.581),
    sodium_per100        = COALESCE(sodium_per100, 0.372)
  WHERE is_system_food = true AND name = 'Whey Isolate';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.1),
    sugars_per100        = COALESCE(sugars_per100, 21.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.58),
    sodium_per100        = COALESCE(sodium_per100, 0.075)
  WHERE is_system_food = true AND name = 'Oreo Biscuits';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.5),
    sugars_per100        = COALESCE(sugars_per100, 2.04),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.187),
    sodium_per100        = COALESCE(sodium_per100, 0.043)
  WHERE is_system_food = true AND name = 'Oats';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 60.5),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.007),
    sodium_per100        = COALESCE(sodium_per100, 0.012)
  WHERE is_system_food = true AND name = 'Maple Syrup';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 53.1),
    sugars_per100        = COALESCE(sugars_per100, 2.17),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.345),
    sodium_per100        = COALESCE(sodium_per100, 0.01)
  WHERE is_system_food = true AND name = 'Cinnamon Powder';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.5),
    sugars_per100        = COALESCE(sugars_per100, 65.18),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.094),
    sodium_per100        = COALESCE(sodium_per100, 0.026)
  WHERE is_system_food = true AND name = 'Raisin';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.2),
    sugars_per100        = COALESCE(sugars_per100, 82.1),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Honey';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 5.3),
    sugars_per100        = COALESCE(sugars_per100, 72.56),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.088),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Cranberry';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.5),
    sugars_per100        = COALESCE(sugars_per100, 2.68),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.019),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Raspberry';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.4),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.019),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Jowar Flour';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.1),
    sugars_per100        = COALESCE(sugars_per100, 4.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 5.0),
    sodium_per100        = COALESCE(sodium_per100, 0.633)
  WHERE is_system_food = true AND name = 'Veggie Mayo';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.0),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.729),
    sodium_per100        = COALESCE(sodium_per100, 0.028)
  WHERE is_system_food = true AND name = 'Roasted Melon Seeds';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 4.7),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.1),
    sodium_per100        = COALESCE(sodium_per100, 0.046)
  WHERE is_system_food = true AND name = 'Dahi';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.4),
    sugars_per100        = COALESCE(sugars_per100, 9.9),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.017),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Plum';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.1),
    sugars_per100        = COALESCE(sugars_per100, 21.1),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.007),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Soya Chunks';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.5),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.1),
    sodium_per100        = COALESCE(sodium_per100, 0.003)
  WHERE is_system_food = true AND name = 'Poha Raw';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.279),
    sodium_per100        = COALESCE(sodium_per100, 0.735)
  WHERE is_system_food = true AND name = 'Black Olives';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.1),
    sugars_per100        = COALESCE(sugars_per100, 4.2),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 5.0),
    sodium_per100        = COALESCE(sodium_per100, 0.633)
  WHERE is_system_food = true AND name = 'Mayo';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.6),
    sugars_per100        = COALESCE(sugars_per100, 32.53),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.148),
    sodium_per100        = COALESCE(sodium_per100, 0.516)
  WHERE is_system_food = true AND name = 'Corn Flakes';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.1),
    sugars_per100        = COALESCE(sugars_per100, 36.61),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 4.235),
    sodium_per100        = COALESCE(sodium_per100, 0.286)
  WHERE is_system_food = true AND name = 'Brownie';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.2),
    sugars_per100        = COALESCE(sugars_per100, 4.62),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.529),
    sodium_per100        = COALESCE(sodium_per100, 0.602)
  WHERE is_system_food = true AND name = 'Sourdough Bread';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.8),
    sugars_per100        = COALESCE(sugars_per100, 0.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.002),
    sodium_per100        = COALESCE(sodium_per100, 5.493)
  WHERE is_system_food = true AND name = 'Soy Sauce';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 12.2),
    sugars_per100        = COALESCE(sugars_per100, 10.7),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.603),
    sodium_per100        = COALESCE(sodium_per100, 0.024)
  WHERE is_system_food = true AND name = 'Chickpea Raw';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 5.3),
    sugars_per100        = COALESCE(sugars_per100, 0.36),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.126),
    sodium_per100        = COALESCE(sodium_per100, 0.238)
  WHERE is_system_food = true AND name = 'Pink Salt';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.7),
    sugars_per100        = COALESCE(sugars_per100, 7.82),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.026),
    sodium_per100        = COALESCE(sodium_per100, 0.008)
  WHERE is_system_food = true AND name = 'Papaya';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.5),
    sugars_per100        = COALESCE(sugars_per100, 3.19),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.867),
    sodium_per100        = COALESCE(sodium_per100, 0.157)
  WHERE is_system_food = true AND name = 'Fresh Cream Cooking';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.0),
    sugars_per100        = COALESCE(sugars_per100, 13.52),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.213),
    sodium_per100        = COALESCE(sodium_per100, 0.315)
  WHERE is_system_food = true AND name = 'Sweet Potato';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.8),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, NULL),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Bitter Gourd (Karela)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.4),
    sugars_per100        = COALESCE(sugars_per100, 22.59),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.154),
    sodium_per100        = COALESCE(sodium_per100, 0.285)
  WHERE is_system_food = true AND name = 'Pumpkin';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 1.86),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.032),
    sodium_per100        = COALESCE(sodium_per100, 0.039)
  WHERE is_system_food = true AND name = 'Radish';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.02),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.002),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Ginger';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.1),
    sugars_per100        = COALESCE(sugars_per100, 0.99),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.178),
    sodium_per100        = COALESCE(sodium_per100, 0.053)
  WHERE is_system_food = true AND name = 'Kale';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.8),
    sugars_per100        = COALESCE(sugars_per100, 0.87),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.014),
    sodium_per100        = COALESCE(sodium_per100, 0.046)
  WHERE is_system_food = true AND name = 'Coriander Leaves';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 5.7),
    sugars_per100        = COALESCE(sugars_per100, 5.67),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.071),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Green Peas';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.2),
    sugars_per100        = COALESCE(sugars_per100, 8.96),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.015),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Orange';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.9),
    sugars_per100        = COALESCE(sugars_per100, 11.42),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.009),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Pineapple';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.2),
    sugars_per100        = COALESCE(sugars_per100, 17.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 5.54),
    sodium_per100        = COALESCE(sodium_per100, 0.231)
  WHERE is_system_food = true AND name = 'Guava';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.0),
    sugars_per100        = COALESCE(sugars_per100, 13.67),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.12),
    sodium_per100        = COALESCE(sodium_per100, 0.003)
  WHERE is_system_food = true AND name = 'Pomegranate';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 9.0),
    sugars_per100        = COALESCE(sugars_per100, 6.23),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 29.698),
    sodium_per100        = COALESCE(sodium_per100, 0.02)
  WHERE is_system_food = true AND name = 'Coconut (Fresh)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.2),
    sugars_per100        = COALESCE(sugars_per100, 17.18),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.045),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Fig (Anjeer)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.8),
    sugars_per100        = COALESCE(sugars_per100, 2.5),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.039),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Lemon';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.5),
    sugars_per100        = COALESCE(sugars_per100, 8.39),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.019),
    sodium_per100        = COALESCE(sodium_per100, 0.013)
  WHERE is_system_food = true AND name = 'Peach';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.0),
    sugars_per100        = COALESCE(sugars_per100, 9.24),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.027),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Apricot';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.1),
    sugars_per100        = COALESCE(sugars_per100, 13.87),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.038),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Cherry';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.3),
    sugars_per100        = COALESCE(sugars_per100, 15.23),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.099),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Lychee';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 5.4),
    sugars_per100        = COALESCE(sugars_per100, 20.1),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.169),
    sodium_per100        = COALESCE(sodium_per100, 0.007)
  WHERE is_system_food = true AND name = 'Chikoo (Sapota)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.8),
    sugars_per100        = COALESCE(sugars_per100, 37.32),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.727),
    sodium_per100        = COALESCE(sodium_per100, 0.079)
  WHERE is_system_food = true AND name = 'Sweet Lime (Mosambi)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.9),
    sugars_per100        = COALESCE(sugars_per100, 0.28),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.652),
    sodium_per100        = COALESCE(sodium_per100, 0.213)
  WHERE is_system_food = true AND name = 'Brown Rice';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.8),
    sugars_per100        = COALESCE(sugars_per100, 0.87),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.231),
    sodium_per100        = COALESCE(sodium_per100, 0.007)
  WHERE is_system_food = true AND name = 'Quinoa';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.0),
    sugars_per100        = COALESCE(sugars_per100, 4.41),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.732),
    sodium_per100        = COALESCE(sodium_per100, 0.43)
  WHERE is_system_food = true AND name = 'Whole Wheat Bread';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.3),
    sugars_per100        = COALESCE(sugars_per100, 5.34),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.821),
    sodium_per100        = COALESCE(sodium_per100, 0.45)
  WHERE is_system_food = true AND name = 'White Bread';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.9),
    sugars_per100        = COALESCE(sugars_per100, 2.72),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.95),
    sodium_per100        = COALESCE(sodium_per100, 0.409)
  WHERE is_system_food = true AND name = 'Roti (Chapati)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.9),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.15),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Semolina (Sooji)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.7),
    sugars_per100        = COALESCE(sugars_per100, 0.28),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.088),
    sodium_per100        = COALESCE(sodium_per100, 0.199)
  WHERE is_system_food = true AND name = 'Barley';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.4),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.019),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Bajra Flour';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 9.1),
    sugars_per100        = COALESCE(sugars_per100, 20.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.496),
    sodium_per100        = COALESCE(sodium_per100, 0.027)
  WHERE is_system_food = true AND name = 'Muesli';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.9),
    sugars_per100        = COALESCE(sugars_per100, 17.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.014),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Vermicelli';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.5),
    sugars_per100        = COALESCE(sugars_per100, 1.71),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.508),
    sodium_per100        = COALESCE(sodium_per100, 0.309)
  WHERE is_system_food = true AND name = 'Toor Dal (Arhar)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.5),
    sugars_per100        = COALESCE(sugars_per100, 1.71),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.508),
    sodium_per100        = COALESCE(sodium_per100, 0.309)
  WHERE is_system_food = true AND name = 'Masoor Dal';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.5),
    sugars_per100        = COALESCE(sugars_per100, 1.71),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.508),
    sodium_per100        = COALESCE(sodium_per100, 0.309)
  WHERE is_system_food = true AND name = 'Urad Dal';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.3),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, NULL),
    sodium_per100        = COALESCE(sodium_per100, NULL)
  WHERE is_system_food = true AND name = 'Kidney Beans (Rajma, dry)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.4),
    sugars_per100        = COALESCE(sugars_per100, 3.53),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.567),
    sodium_per100        = COALESCE(sodium_per100, 0.35)
  WHERE is_system_food = true AND name = 'Black Beans';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, NULL),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.066),
    sodium_per100        = COALESCE(sodium_per100, 0.007)
  WHERE is_system_food = true AND name = 'Cowpea (Lobia)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.56),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 9.55),
    sodium_per100        = COALESCE(sodium_per100, 0.048)
  WHERE is_system_food = true AND name = 'Egg Yolk';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.12),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.927),
    sodium_per100        = COALESCE(sodium_per100, 0.335)
  WHERE is_system_food = true AND name = 'Chicken Thigh';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.54),
    sodium_per100        = COALESCE(sodium_per100, 0.402)
  WHERE is_system_food = true AND name = 'Tuna';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.3),
    sugars_per100        = COALESCE(sugars_per100, 0.14),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.526),
    sodium_per100        = COALESCE(sodium_per100, 0.388)
  WHERE is_system_food = true AND name = 'Prawns';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, NULL),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.54),
    sodium_per100        = COALESCE(sodium_per100, 0.009)
  WHERE is_system_food = true AND name = 'Tempeh';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.5),
    sugars_per100        = COALESCE(sugars_per100, 10.74),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.838),
    sodium_per100        = COALESCE(sodium_per100, 0.058)
  WHERE is_system_food = true AND name = 'Greek Yogurt';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 5.36),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.552),
    sodium_per100        = COALESCE(sodium_per100, 0.092)
  WHERE is_system_food = true AND name = 'Buttermilk (Chaas)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.33),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 19.2),
    sodium_per100        = COALESCE(sodium_per100, 0.654)
  WHERE is_system_food = true AND name = 'Cheddar Cheese';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.1),
    sugars_per100        = COALESCE(sugars_per100, 2.8),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 6.73),
    sodium_per100        = COALESCE(sodium_per100, 0.838)
  WHERE is_system_food = true AND name = 'Mozzarella';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 54.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 5.486),
    sodium_per100        = COALESCE(sodium_per100, 0.127)
  WHERE is_system_food = true AND name = 'Condensed Milk';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 24.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 15.0),
    sodium_per100        = COALESCE(sodium_per100, 0.09)
  WHERE is_system_food = true AND name = 'Khoya (Mawa)';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 5.05),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.049),
    sodium_per100        = COALESCE(sodium_per100, 0.041)
  WHERE is_system_food = true AND name = 'Skimmed Milk';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, NULL),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 16.2),
    sodium_per100        = COALESCE(sodium_per100, NULL)
  WHERE is_system_food = true AND name = 'Peanuts';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 23.1),
    sugars_per100        = COALESCE(sugars_per100, 1.55),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.28),
    sodium_per100        = COALESCE(sodium_per100, 0.037)
  WHERE is_system_food = true AND name = 'Flax Seeds';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.4),
    sugars_per100        = COALESCE(sugars_per100, 1.27),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 8.443),
    sodium_per100        = COALESCE(sodium_per100, 0.477)
  WHERE is_system_food = true AND name = 'Pumpkin Seeds';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 11.6),
    sugars_per100        = COALESCE(sugars_per100, 0.48),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 9.055),
    sodium_per100        = COALESCE(sodium_per100, 0.047)
  WHERE is_system_food = true AND name = 'Sesame Seeds';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.1),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, NULL),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Makhana (Fox Nuts)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 82.5),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Coconut Oil';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 10.3),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Sunflower Oil';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 16.9),
    sodium_per100        = COALESCE(sodium_per100, 0.0)
  WHERE is_system_food = true AND name = 'Groundnut Oil';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Green Tea';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.1),
    sugars_per100        = COALESCE(sugars_per100, 2.61),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.018),
    sodium_per100        = COALESCE(sodium_per100, 0.105)
  WHERE is_system_food = true AND name = 'Coconut Water';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.2),
    sugars_per100        = COALESCE(sugars_per100, 8.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.009),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Orange Juice';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 10.6),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Cola';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Beer';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 99.22),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Sugar';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 85.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.06),
    sodium_per100        = COALESCE(sodium_per100, 0.03)
  WHERE is_system_food = true AND name = 'Jaggery (Gur)';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 22.7),
    sugars_per100        = COALESCE(sugars_per100, 3.21),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.84),
    sodium_per100        = COALESCE(sodium_per100, 0.027)
  WHERE is_system_food = true AND name = 'Turmeric Powder';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 10.5),
    sugars_per100        = COALESCE(sugars_per100, 2.25),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.54),
    sodium_per100        = COALESCE(sodium_per100, 0.168)
  WHERE is_system_food = true AND name = 'Cumin (Jeera)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 25.3),
    sugars_per100        = COALESCE(sugars_per100, 0.64),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.39),
    sodium_per100        = COALESCE(sodium_per100, 0.02)
  WHERE is_system_food = true AND name = 'Black Pepper';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 44.5),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.244),
    sodium_per100        = COALESCE(sodium_per100, 0.01)
  WHERE is_system_food = true AND name = 'Red Chilli Powder';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.6),
    sugars_per100        = COALESCE(sugars_per100, 2.3),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.18),
    sodium_per100        = COALESCE(sodium_per100, 0.092)
  WHERE is_system_food = true AND name = 'Garam Masala';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 5.1),
    sugars_per100        = COALESCE(sugars_per100, 38.8),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.272),
    sodium_per100        = COALESCE(sodium_per100, 0.028)
  WHERE is_system_food = true AND name = 'Tamarind';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Vinegar';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 7.2),
    sugars_per100        = COALESCE(sugars_per100, 46.14),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 18.993),
    sodium_per100        = COALESCE(sodium_per100, 0.023)
  WHERE is_system_food = true AND name = 'Dark Chocolate';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.1),
    sugars_per100        = COALESCE(sugars_per100, 6.97),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.121),
    sodium_per100        = COALESCE(sodium_per100, 0.056)
  WHERE is_system_food = true AND name = 'Milk Chocolate';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.1),
    sugars_per100        = COALESCE(sugars_per100, 0.33),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 3.4),
    sodium_per100        = COALESCE(sodium_per100, 0.527)
  WHERE is_system_food = true AND name = 'Potato Chips';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.4),
    sugars_per100        = COALESCE(sugars_per100, 45.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 6.0),
    sodium_per100        = COALESCE(sodium_per100, 0.05)
  WHERE is_system_food = true AND name = 'Gulab Jamun';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.0),
    sugars_per100        = COALESCE(sugars_per100, 40.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 9.0),
    sodium_per100        = COALESCE(sodium_per100, 0.02)
  WHERE is_system_food = true AND name = 'Laddu';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, NULL),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 9.3),
    sodium_per100        = COALESCE(sodium_per100, 0.079)
  WHERE is_system_food = true AND name = 'Casein Protein';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.0),
    sugars_per100        = COALESCE(sugars_per100, 20.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.0),
    sodium_per100        = COALESCE(sodium_per100, 0.2)
  WHERE is_system_food = true AND name = 'Mass Gainer';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.6),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Red Wine';  -- override

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.96),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'White Wine';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.47),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.011)
  WHERE is_system_food = true AND name = 'Rosé Wine';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 1.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.005)
  WHERE is_system_food = true AND name = 'Sparkling Wine (Champagne)';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Whiskey';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Vodka';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Rum';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Brandy';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Tequila';  -- fdc

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.1),
    sugars_per100        = COALESCE(sugars_per100, 1.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.089),
    sodium_per100        = COALESCE(sodium_per100, 0.017)
  WHERE is_system_food = true AND name = 'Garlic';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.5),
    sugars_per100        = COALESCE(sugars_per100, 5.1),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.048),
    sodium_per100        = COALESCE(sodium_per100, 0.007)
  WHERE is_system_food = true AND name = 'Green Chilli';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.7),
    sugars_per100        = COALESCE(sugars_per100, 0.66),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 2.126),
    sodium_per100        = COALESCE(sodium_per100, 0.007)
  WHERE is_system_food = true AND name = 'Avocado';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 3.0),
    sugars_per100        = COALESCE(sugars_per100, 3.53),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.034),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Eggplant (Brinjal)';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 4.1),
    sugars_per100        = COALESCE(sugars_per100, 0.4),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.041),
    sodium_per100        = COALESCE(sodium_per100, 0.011)
  WHERE is_system_food = true AND name = 'Colocasia (Arbi)';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.1),
    sodium_per100        = COALESCE(sodium_per100, 0.05)
  WHERE is_system_food = true AND name = 'Basa Fish';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 1.5),
    sodium_per100        = COALESCE(sodium_per100, 0.06)
  WHERE is_system_food = true AND name = 'Rohu Fish';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.79),
    sodium_per100        = COALESCE(sodium_per100, 0.082)
  WHERE is_system_food = true AND name = 'Mutton';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 6.4),
    sugars_per100        = COALESCE(sugars_per100, 0.3),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.056),
    sodium_per100        = COALESCE(sodium_per100, 0.002)
  WHERE is_system_food = true AND name = 'Boiled Rajma';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 8.6),
    sugars_per100        = COALESCE(sugars_per100, 2.62),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 4.455),
    sodium_per100        = COALESCE(sodium_per100, 0.009)
  WHERE is_system_food = true AND name = 'Sunflower Seeds';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.6),
    sugars_per100        = COALESCE(sugars_per100, 1.7),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.039),
    sodium_per100        = COALESCE(sodium_per100, 0.033)
  WHERE is_system_food = true AND name = 'Broccoli';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.981),
    sodium_per100        = COALESCE(sodium_per100, 0.044)
  WHERE is_system_food = true AND name = 'Salmon';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 9.0),
    sugars_per100        = COALESCE(sugars_per100, 2.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.5),
    sodium_per100        = COALESCE(sodium_per100, 0.02)
  WHERE is_system_food = true AND name = 'Chana Dal';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 2.4),
    sugars_per100        = COALESCE(sugars_per100, 13.5),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.05),
    sodium_per100        = COALESCE(sodium_per100, 0.004)
  WHERE is_system_food = true AND name = 'Custard Apple (Sitaphal)';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 0.0),
    sugars_per100        = COALESCE(sugars_per100, 0.0),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.0),
    sodium_per100        = COALESCE(sodium_per100, 0.001)
  WHERE is_system_food = true AND name = 'Gin';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.2),
    sugars_per100        = COALESCE(sugars_per100, 25.36),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 6.8),
    sodium_per100        = COALESCE(sodium_per100, 0.076)
  WHERE is_system_food = true AND name = 'Chocolate Ice Cream';  -- override (curated raw)

UPDATE public.foods SET
    fiber_per100         = COALESCE(fiber_per100, 1.6),
    sugars_per100        = COALESCE(sugars_per100, 2.44),
    saturated_fat_per100 = COALESCE(saturated_fat_per100, 0.74),
    sodium_per100        = COALESCE(sodium_per100, 0.199)
  WHERE is_system_food = true AND name = 'Pizza Pasta Sauce';  -- override (curated raw)

-- Bump updated_at so the client delta-sync (findByIsSystemFoodTrueAndUpdatedAtAfter) re-ships these
-- rows to already-synced devices. Raw SQL bypasses the JPA lifecycle that would normally stamp it,
-- so without this the webapp (live fetch) shows the values but Android (offline cache) never pulls them.
UPDATE public.foods SET updated_at = now() WHERE is_system_food = true;
