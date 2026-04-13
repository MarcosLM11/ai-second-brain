#!/usr/bin/env bash
# pre-compact.sh — save context before compaction as raw source for later ingestion
# Usage: called automatically via Claude Code PreCompact hook
# To process: /ingest ~/brain/raw/sessions/precompact-<timestamp>.md

set -euo pipefail

# If no context, exit cleanly without error
if [ -z "${CLAUDE_CONTEXT:-}" ]; then
    exit 0
fi

TIMESTAMP=$(date +%s)
SESSIONS_DIR="${HOME}/brain/raw/sessions"
mkdir -p "$SESSIONS_DIR"

CONTEXT_FILE="${SESSIONS_DIR}/precompact-${TIMESTAMP}.md"
printf '%s' "$CLAUDE_CONTEXT" > "$CONTEXT_FILE"

exit 0
