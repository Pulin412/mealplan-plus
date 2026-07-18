-- V13: users — add enum types for gender/activity/goal/units,
--       convert existing VARCHAR columns, add missing target columns.

-- 1. Enum types
CREATE TYPE gender_enum         AS ENUM ('MALE','FEMALE','OTHER');
CREATE TYPE activity_level_enum AS ENUM ('SEDENTARY','LIGHTLY_ACTIVE','MODERATELY_ACTIVE','VERY_ACTIVE','EXTRA_ACTIVE');
CREATE TYPE goal_type_enum      AS ENUM ('LOSE_WEIGHT','MAINTAIN','GAIN_MUSCLE','GAIN_WEIGHT');
CREATE TYPE units_enum          AS ENUM ('METRIC','IMPERIAL');

-- 2. Convert existing VARCHAR columns → enums (unrecognised legacy values become NULL)
ALTER TABLE users
    ALTER COLUMN gender         TYPE gender_enum
        USING CASE WHEN upper(gender) IN ('MALE','FEMALE','OTHER')
                   THEN upper(gender)::gender_enum END,
    ALTER COLUMN activity_level TYPE activity_level_enum
        USING CASE WHEN upper(activity_level) IN ('SEDENTARY','LIGHTLY_ACTIVE','MODERATELY_ACTIVE','VERY_ACTIVE','EXTRA_ACTIVE')
                   THEN upper(activity_level)::activity_level_enum END,
    ALTER COLUMN goal_type      TYPE goal_type_enum
        USING CASE WHEN upper(goal_type) IN ('LOSE_WEIGHT','MAINTAIN','GAIN_MUSCLE','GAIN_WEIGHT')
                   THEN upper(goal_type)::goal_type_enum END;

-- 3. Add missing target columns
ALTER TABLE users
    ADD COLUMN target_weight_kg DOUBLE PRECISION,
    ADD COLUMN target_protein   INT,
    ADD COLUMN target_carbs     INT,
    ADD COLUMN target_fat       INT,
    ADD COLUMN preferred_units  units_enum NOT NULL DEFAULT 'METRIC';
