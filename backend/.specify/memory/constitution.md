<!--
Sync Impact Report
- Version change: template placeholders -> 1.0.0
- Modified principles:
	- [PRINCIPLE_1_NAME] -> I. Hexagonal Boundaries First
	- [PRINCIPLE_2_NAME] -> II. AI and Tool Calls Are Explicit
	- [PRINCIPLE_3_NAME] -> III. Test the Boundaries
	- [PRINCIPLE_4_NAME] -> IV. LocalStack and Config Safety
	- [PRINCIPLE_5_NAME] -> V. Docker and Windows Compatibility
- Added sections:
	- Platform & Runtime Constraints
	- Development Workflow & Quality Gates
- Removed sections:
	- Template placeholder section names and guidance comments
- Templates requiring updates:
	- .specify/templates/plan-template.md: reviewed and aligned
	- .specify/templates/spec-template.md: reviewed and aligned
	- .specify/templates/tasks-template.md: reviewed and aligned
	- .github/copilot-instructions.md: reviewed and aligned
- Follow-up TODOs: none
-->

# FATEC AI Bot Backend Constitution

## Core Principles

### I. Hexagonal Boundaries First
Business logic MUST live behind ports and service layers. HTTP resources, WebSocket handlers,
MCP clients, DynamoDB repositories, S3 adapters, and other infrastructure integrations MUST
stay at the edges. New features MUST define or extend the domain contract before the adapter
implementation. Direct infrastructure access from presentation code is forbidden unless the
access is isolated behind a named port.

### II. AI and Tool Calls Are Explicit
LLM behavior MUST be implemented through the Quarkus and LangChain4j stack already declared in
the repository. Tool access MUST flow through the MCP client and tool-provider wiring, and the
agent prompt MUST define the order of operations, fallbacks, and language of response. Any AI
feature that depends on external tools MUST remain observable through tests, logs, or both.
Hidden tool invocation outside the agent boundary is not allowed.

### III. Test the Boundaries
Any change that crosses HTTP, WebSocket, MCP, DynamoDB, LocalStack, or external API boundaries
MUST have the narrowest useful automated test at the affected slice, plus at least one contract
or integration test for the user-visible flow. Regression fixes MUST add a test that fails before
the fix. Unit tests alone are insufficient for new cross-boundary behavior.

### IV. LocalStack and Config Safety
Storage and integration work MUST run against LocalStack by default in local development and
tests. DynamoDB table bootstrap, seed data, and migration scripts MUST be idempotent. Secrets,
tokens, and provider keys MUST come from environment variables or `.env` files and MUST NOT be
committed to source control. Persisted key names, table names, and GSI names are public contracts
and MUST only change with a documented migration.

### V. Docker and Windows Compatibility
Developer workflows MUST work through the documented Docker Compose and repo script entry points
on Windows and Unix-like shells. Makefile targets and helper scripts MUST avoid shell-specific
assumptions that break on Windows. When paths or environment variables differ by platform, the
documented host fallbacks in the repository MUST be used. Any new operational command MUST be
reflected in the runtime docs when it changes setup or troubleshooting.

## Platform & Runtime Constraints

- The backend remains a Quarkus application built with the Maven wrapper and the versions declared
	in `pom.xml`. Java, Quarkus, and dependency upgrades MUST be intentional and documented.
- The MCP server remains a separate Python service with a stable health endpoint and documented
	HTTP/SSE transport.
- The data model follows the single-table DynamoDB pattern documented in the repository README.
	Key names and GSI names are part of the external contract.
- Groq, MCP, LocalStack, and other runtime endpoints MUST stay configurable through environment
	variables so local, test, and deployment environments do not require code edits.

## Development Workflow & Quality Gates

- New user-visible work MUST start from a specification and plan when it spans more than one file
	or boundary. Tasks MUST be broken into independently testable slices.
- A change is not complete until the relevant automated tests, build checks, and any required
	Docker or compose validation pass for the touched slice.
- Changes that affect APIs, WebSockets, agent behavior, or data shapes MUST update the runtime
	documentation and examples when the user workflow changes.
- Implementations MUST stay small and direct. Prefer explicit ports, services, and adapters over
	hidden framework behavior. Temporary diagnostics MUST be removed before merge.

## Governance

- This constitution overrides conflicting guidance in README files, quickstarts, plans, and task
	templates. When a conflict exists, the constitution wins.
- Amendments require a version bump, a same-day update to the amendment date, and a sync report in
	this file that states what changed and what was reviewed for alignment.
- Versioning follows semantic rules: MAJOR for principle removal or redefinition, MINOR for added
	principles or materially expanded guidance, and PATCH for clarifications or wording fixes.
- Reviews and automated checks MUST confirm compliance with these rules before merge. Any approved
	exception MUST be documented in the plan or task set with explicit justification.

**Version**: 1.0.0 | **Ratified**: 2026-05-25 | **Last Amended**: 2026-05-25
