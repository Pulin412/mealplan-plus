import { readFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

// Single source of truth for the webapp version = package.json "version". Injected at build so the
// Settings screen (and feedback) always show the current version — bump it in ONE place per release.
const pkg = JSON.parse(readFileSync(join(dirname(fileURLToPath(import.meta.url)), "package.json"), "utf8"));

/** @type {import('next').NextConfig} */
const nextConfig = {
  env: { NEXT_PUBLIC_APP_VERSION: pkg.version },
};

export default nextConfig;
