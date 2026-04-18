# jvm-doctor PRD

Last updated: `2026-04-18`

## Product Statement

`jvm-doctor` is an incident triage layer for Spring Boot and JVM services.

It ingests operational artifacts such as `thread dump`, `actuator metrics`, and `application logs`, then produces:

- deterministic findings
- ranked hypotheses
- next investigation actions
- reproducible reports

On the server path, it can also add optional AI-generated narrative guidance through an OpenAI-compatible API while keeping deterministic findings as the source of truth.

## Problem

Existing JVM tooling is strong but fragmented:

- Arthas is excellent for live inspection
- JFR and JMC are deep but expert-heavy
- Actuator is easy to expose but low-level
- profilers show hotspots, not incident narratives

Teams still spend the first 30 to 60 minutes of an incident manually stitching together raw evidence.

## Target Users

- Java backend engineers on Spring Boot
- on-call developers and tech leads
- JVM-focused SRE or platform engineers
- consultants or delivery engineers who inherit unfamiliar systems

## Success Criteria for v0

- produce a first-pass diagnosis from local artifacts
- cover common JVM/Spring failure modes with deterministic rules
- provide both CLI and HTTP entry points
- ship with a reproducible sample corpus and benchmark runner

## v0 Scope

### Inputs

- `thread dump`
- `actuator metrics`
- `application logs`

### Outputs

- structured findings
- ranked hypotheses
- next action list
- JSON and Markdown reports
- optional server-side AI summary block

### Findings Included

- HTTP worker pool exhaustion
- database connection pool exhaustion
- deadlock detection
- downstream I/O blocking
- full GC pressure
- OOM or leak suspicion
- CPU hot but not GC-led
- logging overhead suspicion

## Architecture

### `jvm-doctor-domain`

Shared DTOs and report structures.

### `jvm-doctor-parser`

Artifact parsing for:

- thread dumps
- actuator metrics
- application logs

### `jvm-doctor-rules`

Deterministic rule set with evidence references and next actions.

### `jvm-doctor-ai`

Currently a rule-based hypothesis layer used by the deterministic engine.

### `jvm-doctor-engine`

Builds analysis context, runs rules, sorts findings, and generates the deterministic report.

### `jvm-doctor-cli`

Local entry point for engineers and demos. It stays deterministic in this iteration.

### `jvm-doctor-server`

HTTP upload and actuator snapshot API for shared workflows.

This module now also performs optional AI post-processing through an OpenAI-compatible API. The AI layer is additive and advisory only.

### `jvm-doctor-bench`

Runs the incident corpus against expectation files to validate deterministic behavior.

## Non-Goals in v0

- no full APM replacement
- no automatic remediation
- no JFR parsing yet
- no heap dump support yet
- no requirement for a vendor-specific LLM in the default path

## Next Version Candidates

1. JFR ingestion
2. larger incident corpus
3. MCP server exposure
4. provider-agnostic AI integration beyond the current OpenAI-compatible mode
