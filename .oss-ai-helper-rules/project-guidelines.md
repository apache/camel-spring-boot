# Project Guidelines

This rule file contains branching, commit, PR, and task-finding conventions for the project. Commands read this file to determine how to name branches, format commits, and search for tasks.

- **Fix branch:** `fix/<ISSUE_ID>`
- **Feature branch:** `feature/<ISSUE_ID>-<short-slug>`
- **Bugfix branch:** `bugfix/<ISSUE_ID>`
- **Quick-fix branch:** `quick-fix/<short-slug>`
- **Commit format (fix):** `<ISSUE_ID>: <brief description of fix>`
- **Commit format (quick-fix):** `chore: <brief description>`
- **CI-issue branch:** `ci-issue/<short-slug>`
- **Commit format (ci-issue):** `ci: <brief description>`
- **PR creation:** always
- **Find-task source:** Jira
- **Find-task beginner JQL:** `project = CAMEL AND status = Open AND labels = good-first-issue` (maxResults=10)
- **Find-task intermediate:** Filter 12352792 (easy issues)
- **Find-task experienced JQL:** `project = CAMEL AND status = Open AND labels = help-wanted` (maxResults=10)
- **Scope-too-large redirect:** create a Jira issue directly
