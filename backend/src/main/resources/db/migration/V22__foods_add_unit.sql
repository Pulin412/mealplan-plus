-- Per-food natural measurement unit. Macros stay per-100g; for PIECE/CUP/TBSP/TSP the
-- matching grams_per_* factor converts a quantity (in this unit) to grams.
ALTER TABLE foods ADD COLUMN IF NOT EXISTS unit varchar(16) NOT NULL DEFAULT 'GRAM';
