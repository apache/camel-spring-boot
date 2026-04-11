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

# Extract module path from log file path
# e.g., ./components-starter/camel-slack-starter/target/surefire-reports/foo.txt -> components-starter/camel-slack-starter
MODULE_PATH=$(echo "$LOG_FILE" | sed 's|^\./||' | sed 's|/target/.*||')

# Skip if file doesn't exist or is not readable
if [[ ! -f "$LOG_FILE" ]] || [[ ! -r "$LOG_FILE" ]]; then
  exit 0
fi

# Extract failures/errors
raw_failures=$(cat "$LOG_FILE" | egrep "FAILURE|ERROR" 2>/dev/null || true)
if [[ -z "$raw_failures" ]]; then
  exit 0
fi

# Look for "Time elapsed" entries and extract org.* test names
time_elapsed_entries=$(echo "$raw_failures" | grep "Time elapsed" 2>/dev/null || true)
org_entries=$(echo "$time_elapsed_entries" | egrep "^org" 2>/dev/null || true)

if [[ -n "$org_entries" ]]; then
  # Generate summary with module path included in test name
  # Format: | Module::TestClass | Duration | Type |
  failed_summary=$(echo "$org_entries" | sed 's/\!//g' | awk -v mod="$MODULE_PATH" -F ' ' '{printf "| **%s**::%s | %s%s | %s |\n", mod, $1,$5,$6, $8}' 2>/dev/null || true)

  if [[ -n "$failed_summary" ]] && [[ -n "${GITHUB_STEP_SUMMARY:-}" ]]; then
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
fi

exit 0
