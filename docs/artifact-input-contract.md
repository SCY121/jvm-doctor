# Artifact Input Contract

Last updated: `2026-04-18`

This document defines the artifact contract used by the CLI, the server, and the benchmark corpus.

## Supported Artifact Types

### 1. Thread Dump

Purpose:

- extract thread names
- extract thread states
- detect deadlock markers
- detect blocking stack patterns

Accepted forms:

- standard JVM `jstack` output
- Spring Boot Actuator thread dump saved as text

Naming convention:

- `thread-dump.txt`
- `actuator-thread-dump.txt`

### 2. Actuator Metrics

Purpose:

- parse available metric names
- parse metric measurements
- feed rule evaluation for pools, CPU, and GC

Accepted JSON forms:

- `/actuator/metrics` root response
- `/actuator/metrics/{name}` single metric response
- merged custom snapshot with:
  - top-level `names`
  - top-level `metrics`

Naming convention:

- `actuator-metrics.json`

### 3. Application Log

Purpose:

- detect timeout, OOM, GC overhead, and deadlock-related signals

Accepted form:

- UTF-8 plain text log file

Naming convention:

- `app.log`
- any `*.log` file in a sample case directory

## CLI Contract

Current CLI flags:

```powershell
jvm-doctor ^
  --thread-dump .\thread-dump.txt ^
  --actuator-metrics .\actuator-metrics.json ^
  --log .\app.log ^
  --format markdown
```

Notes:

- `--thread-dump` is repeatable
- `--actuator-metrics` is repeatable
- `--log` is repeatable
- `--format` supports `json` and `markdown`
- `--output` is optional

## Sample Corpus Contract

Recommended layout for each case:

```text
samples/incidents/<case-id>/
  expectations.json
  thread-dump.txt
  actuator-metrics.json
  app.log
```

Only files present in a case are loaded. Missing artifact types are allowed.

## `expectations.json`

Supported fields:

```json
{
  "name": "Case title",
  "requiredFindingIds": ["DB_POOL_EXHAUSTED"],
  "forbiddenFindingIds": ["DEADLOCK_DETECTED"],
  "maxFindingCount": 3
}
```

Rules:

- every `requiredFindingIds` entry must be present
- every `forbiddenFindingIds` entry must be absent
- `maxFindingCount` is optional
