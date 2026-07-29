-- Consent + onboarding tracking on users (server-side source of truth).
--   consented_at / privacy_policy_version : recorded when the user accepts the privacy policy in
--     the onboarding consent step (GDPR Art. 9 consent for health-related data).
--   onboarding_completed_at : set when first-run onboarding is finished/skipped, so onboarding is
--     shown once per account instead of once per device.
-- All nullable, no backfill needed (existing rows = not-yet-consented / not-yet-onboarded).
ALTER TABLE public.users
    ADD COLUMN consented_at            timestamp with time zone,
    ADD COLUMN privacy_policy_version  character varying(32),
    ADD COLUMN onboarding_completed_at timestamp with time zone;
