-- Optional, free-form food category (user picks from suggestions or types a new one).
-- Nullable + additive → safe on the existing prod schema. The H2 dev profile builds its
-- schema from JPA entities, so it picks this up via the Food.category field instead.
ALTER TABLE public.foods ADD COLUMN category varchar(32);
