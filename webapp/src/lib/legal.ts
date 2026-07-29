// Legal / compliance constants shared across the app.
//
// PRIVACY_POLICY_VERSION is the value we record against a user's consent (see onboarding consent
// step). Bump it (to the new "Last updated" date) whenever the policy text materially changes so
// we can tell who consented to which version and re-prompt if needed.
export const PRIVACY_POLICY_VERSION = "2026-07-29";

// Public, unauthenticated URL — paste this into the Google Play & App Store listings.
export const PRIVACY_POLICY_PATH = "/privacy";

// Contact for privacy requests. TODO: consider a dedicated support address before public launch.
export const PRIVACY_CONTACT_EMAIL = "pulin4122001@gmail.com";
