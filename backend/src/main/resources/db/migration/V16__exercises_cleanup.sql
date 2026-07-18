-- V16: exercises — drop columns not in the new design.
--      Tags are now handled via entity_tags (V15).
--      Kept: id, firebase_uid, name, is_system, created_at, updated_at, server_id.

ALTER TABLE exercises
    DROP COLUMN IF EXISTS category,
    DROP COLUMN IF EXISTS muscle_group,
    DROP COLUMN IF EXISTS equipment,
    DROP COLUMN IF EXISTS description,
    DROP COLUMN IF EXISTS video_link;
