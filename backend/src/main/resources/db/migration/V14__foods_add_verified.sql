-- V14: foods — add verified flag to distinguish trusted-DB foods from user-created ones.
-- verified=true  → sourced from USDA / OpenFoodFacts
-- verified=false → manually entered by user (default)

ALTER TABLE foods
    ADD COLUMN verified BOOLEAN NOT NULL DEFAULT FALSE;
