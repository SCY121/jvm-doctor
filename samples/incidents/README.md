# Incident Corpus

Each subdirectory under `samples/incidents` is a reproducible incident case.

Recommended layout:

```text
samples/incidents/<case-id>/
  expectations.json
  thread-dump.txt
  actuator-metrics.json
  app.log
```

Only `expectations.json` is required for benchmark discovery. Artifact files are optional and are loaded if present.

The benchmark runner validates each case against:

- `requiredFindingIds`
- `forbiddenFindingIds`
- `maxFindingCount`
