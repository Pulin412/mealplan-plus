#!/usr/bin/env bash
# Extract the release-notes block for a client + version from CHANGELOG.md.
#
#   scripts/changelog.sh <Android|Webapp> <version>
#
# Prints the lines under `### [<version>]` within the `## <client>` section, up to the next
# `###`/`##` heading (leading/trailing blank lines trimmed). Exits non-zero (no output) if that
# block isn't found, so callers can fall back to a default. Used by the release workflows.
set -euo pipefail

client="${1:?usage: changelog.sh <client> <version>}"
version="${2:?usage: changelog.sh <client> <version>}"
file="${3:-CHANGELOG.md}"

out="$(
  awk -v client="## $client" -v ver="### [$version]" '
    $0 == client                    { inclient=1; next }
    /^## / && $0 != client          { inclient=0 }
    inclient && index($0, ver) == 1 { inver=1; next }
    inclient && inver && /^#{2,3} /  { inver=0 }
    inclient && inver               { buf[n++] = $0 }
    END {
      s = 0;   while (s < n   && buf[s] ~ /^[[:space:]]*$/) s++
      e = n-1; while (e >= 0  && buf[e] ~ /^[[:space:]]*$/) e--
      for (i = s; i <= e; i++) print buf[i]
    }
  ' "$file"
)"

[ -n "$out" ] || exit 1
printf '%s\n' "$out"
