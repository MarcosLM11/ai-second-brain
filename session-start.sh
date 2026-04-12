#!/usr/bin/env bash
# session-start.sh — inject brain context at the start of each Claude Code session
# Usage: called automatically via Claude Code UserPromptSubmit hook

set -euo pipefail

# Locate the brain CLI (adjust path as needed)
BRAIN_CLI="${BRAIN_CLI:-brain}"

# Get the working directory (passed by Claude Code hook as $CLAUDE_PROJECT_DIR, else use PWD)
PROJECT_DIR="${CLAUDE_PROJECT_DIR:-$(pwd)}"

# Run brain context; if it fails (e.g., no graph), produce no output
CONTEXT=$("$BRAIN_CLI" context --project "$PROJECT_DIR" --max-tokens 2000 2>/dev/null || true)

if [ -n "$CONTEXT" ]; then
    printf '---BRAIN-CONTEXT-START---\n%s\n---BRAIN-CONTEXT-END---\n' "$CONTEXT"
fi
