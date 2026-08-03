-- Allow tags to be attached to workout templates.
-- The tag machinery is already generic (a `tags` row of a given `entity_type`, linked to an entity
-- via the `entity_tags` table keyed by (tag_id, entity_type, entity_id)) — so nothing new is needed
-- beyond permitting the WORKOUT value in the `tag_entity_type` enum. No new tables.
ALTER TYPE public.tag_entity_type ADD VALUE IF NOT EXISTS 'WORKOUT';
