# jvm-doctor Test And UX Report

Date: `2026-04-18`

## Scope

This report covers:

- functional verification
- non-functional spot checks
- onboarding and developer experience review
- issues found and fixed during the run

## Functional Verification

### Automated Test Suite

Executed:

- `mvn -B test`
- `mvn -q -DskipTests package`

Result:

- all module tests passed
- packaged server jar built successfully

Coverage exercised by tests:

- thread dump parsing
- actuator metrics parsing
- application log parsing
- deterministic rule evaluation
- engine orchestration
- CLI rendering
- HTTP analysis upload
- HTTP analysis fetch by id
- rule list endpoint
- actuator snapshot happy path
- AI augmentation success path with mock provider
- AI augmentation fallback path
- validation error path
- unreachable actuator target path
- incident corpus benchmark runner

### Manual Runtime Verification

Validated against a real packaged server process:

- `GET /api/v1/about`
- `GET /api/v1/rules`
- `POST /api/v1/analyses`
- `GET /api/v1/analyses/{id}`
- invalid upload request
- invalid actuator snapshot request
- unreachable actuator snapshot target

Observed results:

- upload analysis returns `COMPLETED`
- deterministic top finding for the sample is `DB_POOL_EXHAUSTED`
- `GET /api/v1/analyses/{id}` returns the same analysis id
- AI-disabled mode returns `ai.status = DISABLED`
- invalid upload returns structured `400` JSON
- validation failure returns structured `400` JSON with `fieldErrors`
- unreachable actuator target now returns structured `502` JSON

### Real Provider AI Verification

Validated against a real external OpenAI-compatible provider:

- base URL: `https://api.longcat.chat/openai`
- model: `LongCat-Flash-Chat`
- validation date: `2026-04-18`

Executed flow:

- minimal `chat/completions` request with a `pong` response check
- packaged server startup with AI enabled via environment variables
- real `POST /api/v1/analyses` upload using the `db-pool-exhausted` sample

Observed results:

- provider authentication succeeded
- both `/chat/completions` and `/v1/chat/completions` returned valid responses for the verified model
- server-side analysis returned `ai.status = COMPLETED`
- returned AI content contained:
  - incident summary
  - recommended actions
  - missing evidence suggestions
  - risk note

Interpretation:

- the current OpenAI-compatible integration works against at least one real third-party provider, not only the local mock server used by automated tests
- the remaining compatibility risk is provider-specific behavior, not a known defect in the current request/response path

### CLI Verification

Executed:

- `mvn -q -pl jvm-doctor-cli exec:java "-Dexec.args=--help"`
- `mvn -q -pl jvm-doctor-cli exec:java`
- `mvn -q -pl jvm-doctor-cli exec:java "-Dexec.args=--thread-dump samples/incidents/db-pool-exhausted/thread-dump.txt --actuator-metrics samples/incidents/db-pool-exhausted/actuator-metrics.json --log samples/incidents/db-pool-exhausted/app.log --format markdown"`

Observed results:

- help output is clear and complete
- missing-input error message is explicit
- sample run produces a readable Markdown report

### Benchmark Verification

Executed:

- `mvn -q -pl jvm-doctor-bench exec:java`

Result:

- corpus cases: `4`
- passed: `4`
- failed: `0`

## Non-Functional Spot Checks

### Startup And Response Time

Measured on the local machine with the packaged server:

- packaged server startup to ready: about `3.1s`
- `GET /api/v1/about`: about `112ms`
- `GET /api/v1/rules`: about `16ms`
- sample upload analysis: about `129ms`
- fetch by id: about `6ms`

Interpretation:

- current local latency is well within acceptable range for a `v0` incident triage tool
- deterministic analysis is fast enough for interactive use on sample-sized inputs

### Error Handling Quality

Initial issue found:

- unreachable actuator snapshot target surfaced as framework default `500`

Fix implemented:

- added `SnapshotFetchException`
- added structured `502` JSON error contract
- added field-level validation details for invalid snapshot requests

Current state:

- user-visible API failures are now more consistent and easier to debug

### Configuration Safety

Verified:

- AI is disabled by default
- API key is only read from environment variables
- no real API key is stored in tracked files

## Developer Experience Review

### README And Onboarding

What worked:

- English and Chinese READMEs both explain the product clearly
- CLI quick start is runnable from repository root
- server AI configuration is documented with placeholders only

Gap found:

- server onboarding originally lacked a concrete upload example

Fix implemented:

- added `curl.exe` examples for `POST /api/v1/analyses`
- documented default server address and AI response states

### Chinese README Encoding

Verified:

- file content is valid UTF-8
- PowerShell default `Get-Content` without `-Encoding utf8` can display mojibake
- `Get-Content -Encoding utf8 README.zh-CN.md` shows correct content

Interpretation:

- repository file is correct
- the mojibake was a local shell display issue, not a committed file corruption issue

## Issues Found And Fixed

1. `POST /api/v1/actuator/snapshot` returned an unhelpful default `500` when the target was unreachable.
   Fixed with a dedicated exception and a structured `502` JSON response.

2. Validation errors for snapshot requests did not expose field-level details.
   Fixed by returning `fieldErrors` in the validation response.

3. README server onboarding was incomplete for first-time users.
   Fixed by adding a direct HTTP API usage example and clarifying the default port.

## Remaining Risks

- only one real external OpenAI-compatible provider was validated end to end in this run
- no large-file or long-duration stability test was executed
- no concurrent load test was executed
- no authentication or multi-user persistence model exists yet
- no JFR, heap dump, or MCP path is in scope for this version

## Overall Assessment

### Functional Readiness

`PASS`

The current version meets its declared `v0` functional scope:

- deterministic artifact analysis works
- CLI is usable
- HTTP API is usable
- benchmark corpus passes
- optional AI path is covered by automated integration tests

### Usability

`PASS WITH MINOR LIMITS`

The project is now easy enough for a first trial:

- root-level commands work
- error messages are materially better than before
- README onboarding is sufficient for CLI and HTTP upload flows

The main remaining usability gap is broader provider coverage. One real provider is now documented and validated, but cross-provider setup is not yet standardized.

### Recommendation

This build is usable as a public `v0` release candidate.

The next highest-value work items are:

1. add a provider-specific AI setup example page
2. add larger incident samples
3. add JFR ingestion
4. add a small persistence layer for server-side analyses
