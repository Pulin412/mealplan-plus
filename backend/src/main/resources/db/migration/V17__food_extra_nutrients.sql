-- Extra per-100g nutrients on foods (Tier A: available from Open Food Facts).
-- Nullable — NULL means "unknown" (existing rows, or an OFF hit missing the value),
-- which the clients render as "—" rather than a real 0.
ALTER TABLE foods
    ADD COLUMN fiber_per100         double precision,
    ADD COLUMN sugars_per100        double precision,
    ADD COLUMN saturated_fat_per100 double precision,
    ADD COLUMN sodium_per100        double precision;
