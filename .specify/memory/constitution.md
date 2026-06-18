<!--
Sync Impact Report
Version change: template -> 1.0.0
Modified principles: placeholders -> I. Monorepo Boundaries Are Explicit; II. Contracts First,
	Implementations Second; III. Tests Prove Each Deliverable; IV. Security and Data Hygiene Are
	Default; V. Operate Transparently and Change Safely
Added sections: Platform & Tooling Standards; Workflow & Quality Gates
Removed sections: none
Templates reviewed: ✅ .specify/templates/plan-template.md; ✅ .specify/templates/spec-template.md;
	✅ .specify/templates/tasks-template.md; ✅ no command templates found under
	.specify/templates/commands/
Deferred items: RATIFICATION_DATE unknown
-->
# FATEC-PI-2026-FATEC-AI-BOT Constitution

## Core Principles

### I. Monorepo Boundaries Are Explicit
The repository is split into `frontend/`, `backend/`, and `AI/`. Each surface owns its runtime,
dependencies, and tests. Changes MUST stay inside the owning surface unless they are updating a
documented contract shared across surfaces. Shared behavior MUST be expressed through an API,
schema, or other explicit interface rather than implicit coupling.
Rationale: the project uses different stacks and must remain independently testable and runnable.

### II. Contracts First, Implementations Second
Any feature that crosses a surface boundary MUST define or update the contract before code changes
land. Contracts include HTTP routes, request/response payloads, data models, prompt/tool contracts,
and persisted data shapes. Breaking contract changes MUST be versioned and justified; otherwise,
changes MUST preserve backward compatibility.
Rationale: the frontend, backend, and AI components are coupled through explicit interfaces.

### III. Tests Prove Each Deliverable
Every user-visible change MUST include the narrowest meaningful automated validation for the touched
surface. Backend work MUST include Quarkus/Maven tests; frontend work MUST include Vitest, ESLint,
or Cypress coverage as appropriate; AI work SHOULD include pytest coverage when behavior changes.
A story is not complete until the changed behavior can be verified independently.
Rationale: this repo spans multiple runtimes, so regressions must be caught near the change.

### IV. Security and Data Hygiene Are Default
Secrets, credentials, tokens, and production data MUST NOT be committed. All external input MUST be
validated and treated as untrusted. Backend endpoints MUST enforce authorization and least privilege
where applicable. AI prompts and tool calls MUST avoid exposing sensitive context unless it is
required for the task and already authorized. LocalStack and synthetic fixtures are the default for
local development and automated tests.
Rationale: the project handles user-facing data and AI-assisted workflows across external systems.

### V. Operate Transparently and Change Safely
Services MUST expose observable health and error signals appropriate to their stack, and changes MUST
be traceable through logs, tests, and documentation. Breaking changes require a migration path or an
explicit compatibility note. Prefer the smallest design that satisfies the feature and keep the code
simple enough to run locally without hidden steps.
Rationale: the project is meant for iterative academic development and team collaboration.

## Platform & Tooling Standards

- Backend implementation uses Java 25 with Quarkus, REST, OpenAPI, JWT support, DynamoDB, and
	LocalStack for local infrastructure.
- Frontend implementation uses Vue 3, Vite, TypeScript, Pinia, Vue Router, Vitest, Cypress, ESLint,
	and Prettier.
- The AI surface is managed as a Python package under `AI/` and uses Poetry-based tooling with
	pytest-oriented verification when tests are added.
- Dockerfiles, Makefiles, and environment files are the preferred way to run, test, and package each
	surface locally.
- Dependency upgrades MUST preserve compatibility with the current runtime targets unless the change
	explicitly updates the supported baseline.

## Workflow & Quality Gates

- Non-trivial work MUST start from a spec, plan, and ordered task list when that workflow exists.
- Every plan MUST pass the constitution check before research or implementation proceeds.
- Tasks MUST be organized so each user story can be implemented and validated independently.
- Cross-surface changes MUST update the contract and all consumers in the same change set whenever
	practical.
- PRs and reviews MUST verify tests, health checks, and documentation updates for the behavior that
	changed.
- Any exception to these rules MUST be recorded with an explicit rationale in the plan or task list.

## Governance

This constitution overrides informal practices and lower-level guidance. Amendments require a single
documented update to this file, a sync impact report, and a version bump that matches the scope of the
change.

Versioning policy:
- MAJOR: remove or redefine a principle, or make a governance change that is backward incompatible.
- MINOR: add a new principle or section, or materially expand existing guidance.
- PATCH: clarify wording, fix typos, or make non-semantic refinements.

Compliance review expectations:
- Every spec, plan, and task review MUST check this constitution before approval.
- Complexity exceptions MUST be justified in the relevant design artifact.
- Runtime guidance documents and templates MUST stay aligned when this constitution changes.

**Version**: 1.0.0 | **Ratified**: TODO(RATIFICATION_DATE): original adoption date unknown | **Last Amended**: 2026-05-27
