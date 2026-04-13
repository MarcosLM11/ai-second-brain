#!/usr/bin/env bash
# session-end.sh — save session transcript as raw source for later ingestion
# Usage: called automatically via Claude Code Stop hook
# To process: /ingest ~/brain/raw/sessions/session-<timestamp>.md

set -euo pipefail

# If no transcript, exit cleanly without error
if [ -z "${CLAUDE_SESSION_TRANSCRIPT:-}" ]; then
    exit 0
fi

TIMESTAMP=$(date +%s)
SESSIONS_DIR="${HOME}/brain/raw/sessions"
mkdir -p "$SESSIONS_DIR"

TRANSCRIPT_FILE="${SESSIONS_DIR}/session-${TIMESTAMP}.md"
printf '%s' "$CLAUDE_SESSION_TRANSCRIPT" > "$TRANSCRIPT_FILE"

exit 0
