-- V16: feature flags — generic runtime on/off toggles, managed via the admin API.
-- First flag: mcp_server (external MCP connector server). Postgres/prod only
-- (H2 dev builds its schema from JPA entities; the FeatureFlag entity mirrors this).
CREATE TABLE feature_flags (
    flag_key   varchar(100) PRIMARY KEY,
    enabled    boolean      NOT NULL DEFAULT false,
    updated_by TEXT,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed the MCP server flag, off by default.
INSERT INTO feature_flags (flag_key, enabled) VALUES ('mcp_server', false);
