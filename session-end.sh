#!/usr/bin/env bash
# session-end.sh — capture session transcript and ingest into the brain wiki
# Usage: called automatically via Claude Code Stop hook

set -euo pipefail

# If no transcript, exit cleanly without error
if [ -z "${CLAUDE_SESSION_TRANSCRIPT:-}" ]; then
    exit 0
fi

TIMESTAMP=$(date +%s)
TRANSCRIPT_FILE="/tmp/brain-session-${TIMESTAMP}.md"

printf '%s' "$CLAUDE_SESSION_TRANSCRIPT" > "$TRANSCRIPT_FILE"

BRAIN_CLI="${BRAIN_CLI:-brain}"

# Launch ingestion asynchronously so the hook returns immediately
nohup "$BRAIN_CLI" ingest \
    --source-type=conversation \
    --file="$TRANSCRIPT_FILE" \
    --async \
    > "/tmp/brain-ingest-${TIMESTAMP}.log" 2>&1 &

exit 0
