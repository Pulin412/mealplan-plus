-- V15: Replace narrow diet-only tags + diet_tag_cross_refs with a universal
--      tags system that works for any entity (diet, exercise, meal, food).

-- 1. Enum for supported entity types
CREATE TYPE tag_entity_type AS ENUM ('DIET','EXERCISE','MEAL','FOOD');

-- 2. Add entity_type discriminator to existing tags table
ALTER TABLE tags
    ADD COLUMN entity_type tag_entity_type NOT NULL DEFAULT 'DIET';

-- Remove the default now that existing rows are stamped
ALTER TABLE tags
    ALTER COLUMN entity_type DROP DEFAULT;

-- 3. Universal join table
CREATE TABLE entity_tags (
    tag_id      BIGINT          NOT NULL REFERENCES tags(id) ON DELETE CASCADE,
    entity_type tag_entity_type NOT NULL,
    entity_id   BIGINT          NOT NULL,
    PRIMARY KEY (tag_id, entity_type, entity_id)
);

CREATE INDEX idx_entity_tags_entity ON entity_tags (entity_type, entity_id);

-- 4. Migrate existing diet_tag_cross_refs → entity_tags
INSERT INTO entity_tags (tag_id, entity_type, entity_id)
SELECT tag_id, 'DIET', diet_id
FROM diet_tag_cross_refs;

-- 5. Drop old join table
DROP TABLE diet_tag_cross_refs;

-- 6. Seed the 10 fixed exercise tags (system tags, firebase_uid = NULL)
--    Colors from spec §3 exercise tag palette: oklch(~0.52 ~0.13 <hue>)
INSERT INTO tags (firebase_uid, name, color, entity_type) VALUES
    (NULL, 'Chest',      'oklch(0.52 0.13 25)',  'EXERCISE'),
    (NULL, 'Back',       'oklch(0.52 0.13 255)', 'EXERCISE'),
    (NULL, 'Legs',       'oklch(0.52 0.13 150)', 'EXERCISE'),
    (NULL, 'Shoulders',  'oklch(0.52 0.13 65)',  'EXERCISE'),
    (NULL, 'Arms',       'oklch(0.52 0.13 310)', 'EXERCISE'),
    (NULL, 'Core',       'oklch(0.52 0.13 200)', 'EXERCISE'),
    (NULL, 'Cardio',     'oklch(0.52 0.13 20)',  'EXERCISE'),
    (NULL, 'Push',       'oklch(0.52 0.13 215)', 'EXERCISE'),
    (NULL, 'Pull',       'oklch(0.52 0.13 285)', 'EXERCISE'),
    (NULL, 'Mobility',   'oklch(0.52 0.13 170)', 'EXERCISE');
