# Local RAG: one corpus, one explicit BE–AI path

This guide applies to the HistAR repositories. The separate `BE/BE` checkout brought
from another machine is a **read-only reference**, not a deployment target.

## What is synchronized

PostgreSQL stores accounts, conversations and application content. Python AI uses
its own versioned Chroma index. A count of zero in PostgreSQL `rag_chunks.embedding`
does not describe the Python index. Installing pgvector alone does not ingest PDFs.

Teammates need the matching **BE, AI and FE revisions**, the AI corpus manifest and
all its PDF/Markdown files, locked Python dependencies, and equivalent non-secret
configuration. Each person supplies their own valid provider credentials privately.
Never commit `.env`, keys or an open Chroma database.

## Prepare AI

Follow the Python version and locked-install instructions in AI's README. Create its
`.venv`, install the lock, and configure `AI/.env`. The embedding model revision is
pinned by `corpus-manifest.json`; allow the first run to download that revision.

From AI:

```powershell
.\.venv\Scripts\python.exe -m scripts.ingest --sync
.\.venv\Scripts\python.exe -m scripts.ingest --check
```

`--sync` builds and validates a fresh generation when necessary, then publishes it.
It does not reset PostgreSQL or delete the old Chroma store. `--check` is read-only,
prints readiness JSON and exits nonzero for a missing or mismatched index. Retain
the previous generation for rollback. After publishing a new generation, restart
the AI process so its model/retrieval caches correspond to that generation.

## Start local BE and AI

From the intended HistAR BE checkout:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\docs\scripts\start-local-rag.ps1 -StartAi -SyncCorpus
```

Use `-AiDirectory <absolute-AI-folder>` if AI is not a sibling of BE, and
`-BackendPort 8081` when deliberately comparing with another local instance.
`-PrintConfig` prints the allowlisted launch configuration without starting anything;
`-CheckOnly` verifies the index and running AI without starting BE.

The launcher explicitly selects the `local` profile and Python/Chroma path
(`rag.enabled=false` disables only the optional PostgreSQL retriever). It compares
the running AI's corpus, embedding and effective configuration fingerprints with
the selected checkout. It refuses occupied BE ports or a mismatched AI. It does not
kill existing processes. AI logs started by it are in BE's ignored `target/local-rag`.

BE still reads the database configuration from its own `.env`; verify the intended
local database first. Flyway is **not disabled** by normal launch, and the launcher
does not bootstrap, reset or change the database destination.

For chat UI, enable `PILOT_CHAT_STREAMING=true` in the intended local BE configuration
and restart BE. Set FE's API URL to that BE port. A streaming feature-flag denial is
different from a RAG connectivity failure.

## Health, failures and evidence

- `/ai/health`: process liveness, not proof that the corpus is usable.
- `/ai/ready`: 200 only for a valid matching index; otherwise 503 with safe reasons.
- `RAG_UNAVAILABLE` (503): missing/stale index, unavailable upstream or open circuit.
- `RAG_TIMEOUT` (504): the request exceeded its deadline.
- Successful `no_source`: the index worked but could not support the question. It
  must not contain a fabricated citation or masquerade as a server error.

Budgets: AI total 90s; BE RAG 100s with one attempt; browser SSE 120s; FE 125s.
Other integrations retain their own timeouts. Errors leave the question visible for
retry; they must not award quest progress. A successful stream completes only after
the answer is persisted.

`X-Request-Id` links browser, BE and AI logs. AI records corpus/config fingerprints,
the actual selected model, source IDs and retrieval/generation timing. Never enable
verbose HTTP logging that prints provider URLs containing credentials.

## Acceptance

Compare HistAR with a clean snapshot of the patched repositories, **not** the
unchanged reference BE. Use a disposable database named `timelens_rag_test_*` and
dedicated verified test profiles/conversations. Do not reuse demo chat history.

Required cases on both synchronous and browser SSE APIs: short and full Cedar Falls
questions, Cu Chi location, a follow-up with identical seeded history, no matching
source, and unavailable/slow AI. Repeat after warm-up. Rebuild the corpus in a clean
environment and compare fingerprints and supported facts/source overlap, not exact
generated wording. Provider failures are failures, not permission to substitute a
generic greeting.

Offline checks:

```powershell
# AI
.\.venv\Scripts\python.exe -m unittest discover -s tests -v
# BE
.\mvnw.cmd test
.\docs\scripts\Test-StartLocalRag.ps1
# FE
npm run lint
npm run build
npx playwright test -c tests/chat-retry.config.ts
```

The live comparison harness is `docs/scripts/compare-rag-backends.py`; see its
`--help` for required loopback endpoints and disposable database arguments. Preserve
its report and correlated logs as evidence. Passing offline mocks alone does not
prove that a live provider or another machine's setup works.
