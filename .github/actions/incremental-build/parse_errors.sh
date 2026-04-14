#!/bin/bash
#
# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Disable exit on error for this script
set +e

LOG_FILE=$1

# Echo immediately to verify script is being invoked
echo "" >&2
echo "============================================================" >&2
echo "[parse_errors.sh START] $(date '+%Y-%m-%d %H:%M:%S')" >&2
echo "[parse_errors.sh] PWD: $(pwd)" >&2
echo "[parse_errors.sh] Processing: $LOG_FILE" >&2
echo "============================================================" >&2

# Extract module path from log file path
# e.g., ./components-starter/camel-slack-starter/target/surefire-reports/foo.txt -> components-starter/camel-slack-starter
MODULE_PATH=$(echo "$LOG_FILE" | sed 's|^\./||' | sed 's|/target/.*||')
echo "[parse_errors.sh] Module: $MODULE_PATH" >&2

# Skip if file doesn't exist or is not readable
if [[ ! -f "$LOG_FILE" ]]; then
  echo "[parse_errors.sh] ⊘ File does not exist, skipping" >&2
  echo "[parse_errors.sh END] $(date '+%Y-%m-%d %H:%M:%S') - exit 0" >&2
  echo "============================================================" >&2
  exit 0
fi

if [[ ! -r "$LOG_FILE" ]]; then
  echo "[parse_errors.sh] ⊘ File not readable, skipping" >&2
  echo "[parse_errors.sh END] $(date '+%Y-%m-%d %H:%M:%S') - exit 0" >&2
  echo "============================================================" >&2
  exit 0
fi

echo "[parse_errors.sh] File size: $(wc -c < "$LOG_FILE") bytes" >&2

# Extract failures/errors
echo "[parse_errors.sh] Searching for FAILURE|ERROR patterns..." >&2
raw_failures=$(cat "$LOG_FILE" | egrep "FAILURE|ERROR" 2>/dev/null || true)
if [[ -z "$raw_failures" ]]; then
  echo "[parse_errors.sh] ✓ No failures found" >&2
  echo "[parse_errors.sh END] $(date '+%Y-%m-%d %H:%M:%S') - exit 0" >&2
  echo "============================================================" >&2
  exit 0
fi

echo "[parse_errors.sh] ✗ Found FAILURE/ERROR entries: $(echo "$raw_failures" | wc -l)" >&2
echo "[parse_errors.sh] First 5 lines:" >&2
echo "$raw_failures" | head -5 >&2

# Look for "Time elapsed" entries and extract org.* test names
time_elapsed_entries=$(echo "$raw_failures" | grep "Time elapsed" 2>/dev/null || true)
echo "[parse_errors.sh] Time elapsed entries: $(echo "$time_elapsed_entries" | wc -l)" >&2

org_entries=$(echo "$time_elapsed_entries" | egrep "^org" 2>/dev/null || true)
echo "[parse_errors.sh] org.* test entries: $(echo "$org_entries" | wc -l)" >&2

if [[ -n "$org_entries" ]]; then
  echo "[parse_errors.sh] Processing failures:" >&2
  echo "$org_entries" >&2

  # Generate summary with module path included in test name
  # Format: | Module::TestClass | Duration | Type |
  failed_summary=$(echo "$org_entries" | sed 's/\!//g' | awk -v mod="$MODULE_PATH" -F ' ' '{printf "| **%s**::%s | %s%s | %s |\n", mod, $1,$5,$6, $8}' 2>/dev/null || true)

  if [[ -n "$failed_summary" ]]; then
    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
      echo "[parse_errors.sh] Writing to GITHUB_STEP_SUMMARY" >&2

      # Add to GitHub step summary
      echo "$failed_summary" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true

      # Add detailed error output
      echo "" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      echo "<details><summary>❌ Failure details for $MODULE_PATH</summary>" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      echo "" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      echo "\`\`\`" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      cat "$LOG_FILE" | egrep -A 10 "FAILURE|ERROR" | head -100 >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      echo "\`\`\`" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      echo "</details>" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
      echo "" >> "$GITHUB_STEP_SUMMARY" 2>/dev/null || true
    else
      echo "[parse_errors.sh] ⚠ GITHUB_STEP_SUMMARY not set, cannot write summary" >&2
    fi
  fi

  # Output to stderr for CI logs
  echo "" >&2
  echo "╔═════════════════════════════════════════════╗" >&2
  echo "║  FAILURE in module: $MODULE_PATH" >&2
  echo "╚═════════════════════════════════════════════╝" >&2
  echo "$org_entries" >&2
  echo "" >&2
  echo "Full error details:" >&2
  cat "$LOG_FILE" | egrep -A 10 "FAILURE|ERROR" | head -50 >&2
  echo "╚═════════════════════════════════════════════╝" >&2
  echo "" >&2
else
  echo "[parse_errors.sh] ⚠ No org.* pattern match (unusual format)" >&2
  echo "" >&2
  echo "╔═════════════════════════════════════════════╗" >&2
  echo "║  ERROR in module: $MODULE_PATH (unusual format)" >&2
  echo "╚═════════════════════════════════════════════╝" >&2
  echo "Raw failures found:" >&2
  echo "$raw_failures" | head -20 >&2
  echo "" >&2
  echo "Full log file content (first 100 lines):" >&2
  head -100 "$LOG_FILE" >&2
  echo "╚═════════════════════════════════════════════╝" >&2
  echo "" >&2
fi

echo "============================================================" >&2
echo "[parse_errors.sh END] $(date '+%Y-%m-%d %H:%M:%S') - exit 0" >&2
echo "============================================================" >&2
exit 0
