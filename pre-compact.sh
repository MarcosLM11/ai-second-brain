#!/usr/bin/env bash
# pre-compact.sh — capture context before compaction and ingest into the brain wiki
# Usage: called automatically via Claude Code PreCompact hook

set -euo pipefail

# If no context, exit cleanly without error
if [ -z "${CLAUDE_CONTEXT:-}" ]; then
    exit 0
fi

TIMESTAMP=$(date +%s)
CONTEXT_FILE="/tmp/brain-precompact-${TIMESTAMP}.md"

printf '%s' "$CLAUDE_CONTEXT" > "$CONTEXT_FILE"

BRAIN_CLI="${BRAIN_CLI:-brain}"

# Launch ingestion asynchronously so the hook returns immediately
nohup "$BRAIN_CLI" ingest \
    --source-type=conversation \
    --file="$CONTEXT_FILE" \
    --async \
    > "/tmp/brain-ingest-precompact-${TIMESTAMP}.log" 2>&1 &

exit 0
