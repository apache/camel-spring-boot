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

echo "[DEBUG parse_errors.sh] Processing: $LOG_FILE" >&2

# Extract module path from log file path
# e.g., ./components-starter/camel-slack-starter/target/surefire-reports/foo.txt -> components-starter/camel-slack-starter
MODULE_PATH=$(echo "$LOG_FILE" | sed 's|^\./||' | sed 's|/target/.*||')
echo "[DEBUG parse_errors.sh] Module path: $MODULE_PATH" >&2

# Skip if file doesn't exist or is not readable
if [[ ! -f "$LOG_FILE" ]]; then
  echo "[DEBUG parse_errors.sh] File does not exist: $LOG_FILE" >&2
  exit 0
fi

if [[ ! -r "$LOG_FILE" ]]; then
  echo "[DEBUG parse_errors.sh] File is not readable: $LOG_FILE" >&2
  exit 0
fi

echo "[DEBUG parse_errors.sh] File exists and is readable, size: $(wc -c < "$LOG_FILE") bytes" >&2

# Extract failures/errors
echo "[DEBUG parse_errors.sh] Searching for FAILURE|ERROR patterns..." >&2
raw_failures=$(cat "$LOG_FILE" | egrep "FAILURE|ERROR" 2>/dev/null || true)
if [[ -z "$raw_failures" ]]; then
  echo "[DEBUG parse_errors.sh] No FAILURE or ERROR patterns found in file" >&2
  exit 0
fi

echo "[DEBUG parse_errors.sh] Found FAILURE/ERROR entries (count: $(echo "$raw_failures" | wc -l))" >&2
echo "[DEBUG parse_errors.sh] First few lines of raw_failures:" >&2
echo "$raw_failures" | head -5 >&2

# Look for "Time elapsed" entries and extract org.* test names
echo "[DEBUG parse_errors.sh] Searching for 'Time elapsed' entries..." >&2
time_elapsed_entries=$(echo "$raw_failures" | grep "Time elapsed" 2>/dev/null || true)
echo "[DEBUG parse_errors.sh] Time elapsed entries found: $(echo "$time_elapsed_entries" | wc -l)" >&2

echo "[DEBUG parse_errors.sh] Searching for org.* entries..." >&2
org_entries=$(echo "$time_elapsed_entries" | egrep "^org" 2>/dev/null || true)
echo "[DEBUG parse_errors.sh] org.* entries found: $(echo "$org_entries" | wc -l)" >&2

if [[ -n "$org_entries" ]]; then
  echo "[DEBUG parse_errors.sh] Processing org_entries:" >&2
  echo "$org_entries" >&2

  # Generate summary with module path included in test name
  # Format: | Module::TestClass | Duration | Type |
  echo "[DEBUG parse_errors.sh] Generating failed_summary..." >&2
  failed_summary=$(echo "$org_entries" | sed 's/\!//g' | awk -v mod="$MODULE_PATH" -F ' ' '{printf "| **%s**::%s | %s%s | %s |\n", mod, $1,$5,$6, $8}' 2>/dev/null || true)
  echo "[DEBUG parse_errors.sh] failed_summary generated (length: ${#failed_summary})" >&2

  if [[ -n "$failed_summary" ]]; then
    echo "[DEBUG parse_errors.sh] failed_summary is not empty" >&2

    if [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
      echo "[DEBUG parse_errors.sh] GITHUB_STEP_SUMMARY is set to: $GITHUB_STEP_SUMMARY" >&2

      # Add to GitHub step summary
      echo "[DEBUG parse_errors.sh] Writing summary to GITHUB_STEP_SUMMARY..." >&2
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
      echo "[DEBUG parse_errors.sh] Summary written to GITHUB_STEP_SUMMARY" >&2
    else
      echo "[DEBUG parse_errors.sh] GITHUB_STEP_SUMMARY is not set, skipping summary output" >&2
    fi
  else
    echo "[DEBUG parse_errors.sh] failed_summary is empty, skipping summary generation" >&2
  fi

  # Output to stderr for CI logs
  echo "" >&2
  echo "=============================================" >&2
  echo "FAILURE in module: $MODULE_PATH" >&2
  echo "=============================================" >&2
  echo "$org_entries" >&2
  echo "" >&2
  echo "Full error details:" >&2
  cat "$LOG_FILE" | egrep -A 10 "FAILURE|ERROR" | head -50 >&2
  echo "=============================================" >&2
  echo "" >&2
else
  echo "[DEBUG parse_errors.sh] No org.* entries found, but raw_failures exist" >&2
  echo "[DEBUG parse_errors.sh] Outputting raw_failures to help debug:" >&2
  echo "" >&2
  echo "=============================================" >&2
  echo "FAILURE/ERROR in module: $MODULE_PATH (no org.* pattern match)" >&2
  echo "=============================================" >&2
  echo "Raw failures found:" >&2
  echo "$raw_failures" | head -20 >&2
  echo "" >&2
  echo "Full log file content (first 100 lines):" >&2
  head -100 "$LOG_FILE" >&2
  echo "=============================================" >&2
  echo "" >&2
fi

echo "[DEBUG parse_errors.sh] Finished processing $LOG_FILE" >&2
exit 0
