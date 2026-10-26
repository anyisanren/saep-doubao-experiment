#!/bin/bash
set -euo pipefail
PROJECT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
exec python3 -m http.server 8765 --bind 0.0.0.0 --directory "$PROJECT_ROOT/dist"
