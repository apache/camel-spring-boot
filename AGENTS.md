# Apache Camel Spring Boot - AI Agent Guidelines

Guidelines for AI agents working on this codebase.

## Project Info

Apache Camel Spring Boot provides Spring Boot auto-configuration and starter modules for Apache Camel components.

- Version: 4.22.0-SNAPSHOT
- Java: 17+
- Build: Maven (use `./mvnw` wrapper)
- Issue tracker: [JIRA (CAMEL project)](https://issues.apache.org/jira/browse/CAMEL)
- Related repository: [apache/camel](https://github.com/apache/camel) — Camel core

## AI Agent Rules of Engagement

These rules apply to ALL AI agents working on this codebase.

### Attribution

- All AI-generated content (GitHub PR descriptions, review comments, JIRA comments) MUST clearly
  identify itself as AI-generated and mention the human operator.
  Example: "_Claude Code on behalf of [Human Name]_"

### PR Volume

- An agent MUST NOT open more than 10 PRs per day per operator to ensure human reviewers can keep up.
- Prioritize quality over quantity — fewer well-tested PRs are better than many shallow ones.

### Git branch

- An agent MUST NEVER push commits to a branch it did not create.
- If a contributor's PR needs changes, the agent may suggest changes via review comments,
  but must not push to their branch without explicit permission.
- An agent should prefer to use his own fork to push branches instead of the main
  apache/camel-spring-boot repository. It avoids filling the main repository with a long list
  of uncleaned branches.
- An agent must provide a useful name for the git branch. It should contain the global topic
  and issue number if possible.
- After a Pull Request is merged or rejected, the branch should be deleted.

### JIRA Ticket Ownership

- An agent MUST ONLY pick up **Unassigned** JIRA tickets.
- If a ticket is already assigned to a human, the agent must not reassign it or work on it.
- Before starting work, the agent must assign the ticket to its operator and transition it to
  "In Progress".
- Before closing a ticket, always set the correct `fixVersions` field.
  Note: `fixVersions` cannot be set on an already-closed issue — set it before closing,
  or reopen/set/close if needed.

### PR Description Maintenance

When pushing new commits to a PR, **always update the PR description** (and title if needed) to
reflect the current state of the changeset. PRs evolve across commits — the description must stay
accurate and complete. Use `gh pr edit --title "..." --body "..."` after each push.

### PR Reviewers

When creating a PR, **always identify and request reviews** from the most relevant committers:

- Run `git log --format='%an' --since='1 year' -- <affected-files> | sort | uniq -c | sort -rn | head -10`
  to find who has been most active on the affected files.
- Use `git blame` on key modified files to identify who wrote the code being changed.
- Cross-reference with the [committer list](https://home.apache.org/committers-by-project.html#camel)
  to ensure you request reviews from active committers (not just contributors).
- For component-specific changes, prefer reviewers who have recently worked on that component.
- For cross-cutting changes (core, auto-configuration), include committers with broader project
  knowledge.
- Request review from **at least 2 relevant committers** using `gh pr edit --add-reviewer`.
- When all comments on the Pull Request are addressed (by providing a fix or providing more
  explanation) and the PR checks are green, re-request review on existing reviewers so that they
  are aware that the new changeset is ready to be reviewed.

### Merge Requirements

- An agent MUST NOT merge a PR if there are any **unresolved review conversations**.
- An agent MUST NOT merge a PR without at least **one human approval**.
- An agent MUST NOT approve its own PRs — human review is always required.

### Code Quality

- Every PR must include tests for new functionality or bug fixes.
- Every PR must include documentation updates where applicable.
- All generated files must be regenerated and committed (CI checks for uncommitted changes).

### Asynchronous Testing: Use Awaitility Instead of Thread.sleep

Do **NOT** use `Thread.sleep()` in test code. It leads to flaky, slow, and non-deterministic tests.
Use the [Awaitility](https://github.com/awaitility/awaitility) library instead, which is already
available as a test dependency in the project.

**Example — waiting for a route to be registered:**

```java
import static org.awaitility.Awaitility.await;

await().atMost(20, TimeUnit.SECONDS)
       .untilAsserted(() -> assertEquals(1, context.getRoutes().size()));
```

**Rules:**

- New test code MUST NOT introduce `Thread.sleep()` calls.
- When modifying existing test code that contains `Thread.sleep()`, migrate it to Awaitility.
- Always set an explicit `atMost` timeout to avoid hanging builds.
- Use `untilAsserted` or `until` with a clear predicate — do not replace a sleep with a
  busy-wait loop.

### Issue Investigation (Before Implementation)

Before implementing a fix for a JIRA issue, **thoroughly investigate** the issue's validity and
context. Camel is a large, long-lived project — code often looks "wrong" but exists for good
reasons. Do NOT jump straight to implementation after reading the issue description and the
current code.

**Required investigation steps:**

1. **Validate the issue**: Confirm the reported problem is real and reproducible. Question
   assumptions in the issue description — they may be incomplete or based on misunderstanding.
2. **Check git history**: Run `git log --oneline <file>` and `git blame <file>` on the affected
   code. Read the commit messages and linked JIRA tickets for prior changes to understand *why*
   the code is written the way it is.
3. **Search for related issues**: Search JIRA for related tickets (same component, similar
   keywords) to find prior discussions, rejected approaches, or intentional design decisions.
4. **Understand the broader context**: If the issue involves a module that replaced or deprecated
   another, understand *why* the replacement was made and what was intentionally changed vs.
   accidentally omitted. Check the [apache/camel](https://github.com/apache/camel) repository
   for design documents in `proposals/` if the affected area touches core Camel behaviour.
5. **Check if the "fix" reverts prior work**: If your proposed change effectively reverts a prior
   intentional commit, stop and reconsider. If the revert is still justified, explicitly
   acknowledge it in the PR description and explain why despite the original rationale.

**Present your findings** to the operator before implementing. Flag any risks, ambiguities, or
cases where the issue may be invalid or the proposed approach may conflict with prior decisions.

### Knowledge Cutoff Awareness

AI agents have a training data cutoff and may not know about recent releases, API changes, or
deprecations in external projects. **Never make authoritative claims about external project state
based solely on training knowledge.**

- When a JIRA issue, PR, or code references a specific version of an external dependency (e.g.,
  Spring Boot 4.0, JUnit 6, Jakarta EE 11), **verify it exists** by checking official sources
  (web search, Maven Central, release notes) before questioning or relying on it.
- When implementing or reviewing changes that depend on external project behavior, verify the
  current state rather than assuming training data is up to date.
- If uncertain about whether something exists or has changed, say so and verify — do not
  confidently assert something is wrong based on potentially stale knowledge.

### Git History Review (When Reviewing PRs)

When reviewing PRs, apply the same investigative rigor:

- Check `git log` and `git blame` on modified files to see if the change conflicts with prior
  intentional decisions.
- Verify that "fixes" don't revert deliberate behavior without justification.
- Search for related JIRA tickets that provide context on why the code was written that way.

## Security Model

This project shares the same security model as [Apache Camel core](https://github.com/apache/camel).
The canonical security model document is maintained in the `apache/camel` repository at
[`docs/user-manual/modules/ROOT/pages/security-model.adoc`](https://github.com/apache/camel/blob/main/docs/user-manual/modules/ROOT/pages/security-model.adoc).

camel-spring-boot is an auto-configuration layer — it wraps Camel components with
`@ConfigurationProperties` and Spring Boot lifecycle integration but does not define its own
trust boundaries, consumers, producers, header filtering, or deserialization paths. The Camel
core security model's trust assumptions, in-scope vulnerability classes, and out-of-scope
categories apply directly.

When triaging security reports or reviewing security-sensitive PRs in this repository, refer to
the Camel core security model for:

- **Trust assumptions**: Route authors and deployment operators are fully trusted; external
  message senders are untrusted.
- **In-scope classes**: Unsafe deserialization, XXE, expression injection, path traversal, SSRF,
  header/bean-dispatch abuse, auth bypass, information disclosure, insecure defaults,
  query injection.
- **Out-of-scope**: Route-author code, explicit opt-ins, DoS via unthrottled routes, deployer
  misconfiguration of management surfaces, unreachable transitive CVEs, scanner reports
  without PoC.

For reporting vulnerabilities, see [`SECURITY.md`](SECURITY.md).

## Structure

```
camel-spring-boot/
├── core/                # Core auto-configuration (CamelAutoConfiguration, actuator, vault)
│   ├── camel-spring-boot/     # Main auto-configuration module
│   └── camel-spring-boot-xml/ # XML (JAXB) route loading variant
├── components-starter/  # ~425 generated starter modules, one per Camel component
├── core-starter/        # Starters for core Camel modules
├── dsl-starter/         # Starters for Camel DSL modules (YAML, Groovy, Kotlin, etc.)
├── tooling/             # Maven plugins for code generation (starters, BOMs, catalog)
├── catalog/             # Spring Boot runtime provider for Camel catalog
├── tests/               # Integration tests and fat-jar tests
└── archetypes/          # Maven archetype for new Camel Spring Boot projects
```

## Build

```bash
# Build a specific starter (preferred — always build in the module directory)
cd components-starter/camel-aws2-s3-starter && mvn verify

# Build core module
cd core/camel-spring-boot && mvn verify

# Fast install (skip tests)
mvn install -Dfastinstall

# Run a single test
cd components-starter/camel-aws2-s3-starter && mvn verify -Dtest=S3ComponentTest

# Format/install without tests
cd <module> && mvn -DskipTests install
```

Do NOT parallelize Maven jobs — builds are resource-intensive.

## Code Generation

Most code in `components-starter/` is **generated** by the Maven plugins in `tooling/`. For each
Camel component, the generator produces:
- `*ComponentAutoConfiguration.java` — Spring Boot `@AutoConfiguration` that registers the
  component bean
- `*ComponentConfiguration.java` — `@ConfigurationProperties` class exposing component options
  as `camel.component.<name>.*`
- `*ComponentConverter.java` — Type converter for complex property types
- `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` — Registers
  auto-configuration classes

Generated code blocks are delimited by `<!--START OF GENERATED CODE-->` / `<!--END OF GENERATED CODE-->`
in POM files. Do not manually edit generated sections.

### Auto-Configuration Pattern

Each starter's auto-configuration class follows a consistent pattern:
1. `@AutoConfiguration(after = CamelAutoConfiguration.class)` — ensures CamelContext is available
2. `@ConditionalOnHierarchicalProperties` — enables/disables via `camel.component.<name>.enabled`
3. `ComponentCustomizer` bean copies Spring Boot properties onto the Camel component

The core `CamelAutoConfiguration` in `core/camel-spring-boot` is the central configuration that
all starters depend on.

### Creating/Deleting Starters

```sh
./starter-create <component-name>    # Create a new starter
./starter-delete <component-name>    # Delete a starter
```

These invoke the `camel-spring-boot-generator-maven-plugin` under `tooling/`.

### Relationship with Camel Core

This project depends on `apache/camel` for component implementations. The `camel-version` property
must match the target Camel release. When Camel core adds/removes/changes a component, the starters
here must be regenerated.

## Conventions

Code style:
- Do NOT use Records or Lombok (unless already present in the file)
- Do NOT change public API signatures without justification
- Do NOT add new dependencies without justification
- Maintain backwards compatibility for public APIs

Import style:
- Do NOT use fully qualified class names (FQCNs) in Java code. Always add an `import` statement
  and use the simple class name.
- Exception: when two classes share the same simple name, import the most-used one and qualify
  the other.

Tests: `*Test.java` (JUnit 5)

## Commits

```
CAMEL-XXXX: Brief description
```

Reference JIRA when applicable.

## Links

- https://camel.apache.org/camel-spring-boot/latest/
- https://github.com/apache/camel-spring-boot
- https://github.com/apache/camel
- https://issues.apache.org/jira/browse/CAMEL
- dev@camel.apache.org
- https://camel.zulipchat.com/
