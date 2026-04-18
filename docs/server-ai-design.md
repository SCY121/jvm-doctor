# Server AI Augmentation Design

Last updated: `2026-04-18`

## Goal

Add optional AI augmentation to the HTTP server without changing the deterministic core pipeline used by the CLI and benchmark modules.

## Scope

This iteration only affects `jvm-doctor-server`.

Included:

- optional OpenAI-compatible API integration
- evidence-constrained summary generation
- server-side configuration surface
- graceful fallback when AI is disabled or fails
- repository cleanup for content that should not stay public

Excluded:

- CLI AI support
- engine-level model dependency
- JFR ingestion
- heap dump analysis

## Architecture Decision

The deterministic analysis flow stays unchanged:

`artifacts -> parsers -> rules -> findings -> hypotheses -> report`

The server adds a post-processing layer:

`deterministic report -> AI augmentation service -> response.ai`

This keeps the benchmark path stable and makes AI strictly additive.

## AI Boundaries

The model only receives structured evidence:

- analysis overview
- matched findings
- ranked hypotheses
- deterministic next actions

The model does not receive raw thread dumps, raw logs, or raw actuator payloads in this iteration.

The model is responsible for:

- concise narrative summary
- prioritized next actions
- missing evidence suggestions

The model is not responsible for:

- parsing artifacts
- matching rules
- inventing unsupported root causes

## Configuration

AI is disabled by default and enabled only through external configuration.

Planned properties:

- `jvm-doctor.ai.enabled`
- `jvm-doctor.ai.base-url`
- `jvm-doctor.ai.api-key`
- `jvm-doctor.ai.model`
- `jvm-doctor.ai.timeout`
- `jvm-doctor.ai.temperature`
- `jvm-doctor.ai.max-findings`

The public repository must never contain a real API key.

One verified real-provider example as of `2026-04-18`:

- base URL: `https://api.longcat.chat/openai`
- model: `LongCat-Flash-Chat`
- validated path: `POST /api/v1/analyses`
- observed result: `response.ai.status = COMPLETED`

## API Shape

`AnalysisResponse` gains an optional AI block with:

- status
- provider
- model
- summary
- recommended actions
- missing evidence
- failure reason

The deterministic report remains the source of truth. AI output is advisory only.

## Failure Handling

- If AI is disabled, return deterministic report plus `ai.status = DISABLED`
- If AI call fails, return deterministic report plus `ai.status = FAILED`
- AI failures must not fail the whole analysis request

## Public Repository Cleanup

These items should not remain public:

- internal process docs
- internal handoff notes

These internal files add little value for users and expose process-specific noise.
