# Engineering Guidelines

Portable, language- and framework-agnostic. Drop this into any repo as the single source of *how we
build* — or hand it to an AI agent / new contributor as context. Stack-specific patterns (frameworks,
folder names, examples) go in a project-local companion, not here.

**Using this as an agent brief:** paste it in at the start of a project as the operating standard. The
agent is expected to follow it **without being re-asked** — most importantly **§7 (no code before a
requirement)** and **§6 (a test for every function)**. These are gates, not suggestions.

---

## 0. Priorities — when they conflict, decide in this order

1. **User experience** — what you ship is clean, fast, obvious (§1).
2. **Performance** — fast execution, low latency, efficient algorithms (§2).
3. **Correctness & safety** — guard edge cases; fail safe; never lose or corrupt data (§12).

This order is for **where to invest polish**, not a license to ship bugs — correctness is the
baseline. A correct-but-clunky feature still loses to a polished one; a broken one ships to no one.

---

## 1. User experience comes first (UI · API · CLI · DX)

Whatever you ship, the surface a human or caller touches **is** the product.

- **Perceived speed beats raw speed.** Respond instantly: render the shell first, load data
  asynchronously, show skeletons/spinners — never a blank or frozen screen.
- **Never block the interaction.** Keep the main/UI thread free; heavy work goes off it (§3). Give
  visible feedback within ~100 ms of any action.
- **Design every state:** loading, empty, error, partial, success. No dead ends; errors are
  actionable, not stack traces.
- **Feel instant:** optimistic updates where safe (apply now, reconcile with the server), debounce/
  throttle noisy input, paginate/virtualize long lists, avoid layout shift.
- **Consistent & minimal:** one visual/interaction language, the fewest steps to the goal, sensible
  defaults, keyboard-friendly, accessible.
- **Non-GUI = DX:** for libraries/CLIs/APIs the same bar applies — obvious interfaces, fast start-up,
  clear errors, good `--help`/docs, sane defaults.

---

## 2. Performance & time complexity

Fast is a feature. Make it **measured**, not guessed.

- **Know the complexity of every hot path.** Prefer `O(1)`/`O(log n)`/`O(n)` over `O(n²)`; choose the
  right data structure — hash/set/index lookups over linear scans, a heap over a full sort, a single
  pass over repeated passes.
- **Measure before optimizing.** Profile to find the *real* hot spot; never micro-optimize on a hunch.
  Optimize the path that is actually slow or hot — leave the rest readable.
- **Don't repeat work:** cache with TTLs, memoize pure functions, compute once and reuse, batch I/O,
  kill N+1 queries/calls, lazy-load what isn't needed yet.
- **Mind the constants in hot loops:** minimize allocations and copies, stream large data instead of
  loading it whole, free what you no longer need.
- **Set a budget** (e.g. p95 latency / response time / throughput) and keep a benchmark or timing
  test so regressions are caught, not discovered in production.

---

## 3. Concurrency & parallelism — stay responsive, cut wall-clock time

Use threads / async / processes both to keep the experience snappy **and** to finish work faster.

- **Get slow work off the critical path.** Anything blocking — network, disk, DB, heavy compute —
  runs in a **background thread / async task / worker / queue** so the UI or request returns
  immediately. The user never waits on the main thread. *This is the main lever for a responsive UX.*
- **Parallelize independent work** to cut total time: a thread pool or `async` gather over independent
  items finishes in ≈ `max(parts)` instead of `sum(parts)`.
- **Pick the model for the workload:**
  - **I/O-bound** (network/disk/db) → threads or async — cheap, many in flight.
  - **CPU-bound** → real parallelism across cores/processes; threads alone won't help under a global
    lock (e.g. Python's GIL).
  - **High-concurrency I/O** → an async event loop.
- **Background loops** handle periodic work (polling, refresh, watchdog) on their own cadence so the
  foreground stays free.
- **Shared mutable state is the danger:** keep it minimal; protect it with locks, or prefer immutable
  data / message passing. Guard against **races and deadlocks**; make operations idempotent and
  reconcilable.
- **Make concurrency testable & deterministic:** isolate state per worker, prove no cross-talk, and
  run the suite repeatedly — a flaky test is usually a real race.

---

## 4. Organize by feature, not by type — and go deep

- Group code by **what it does** (feature / domain), not by technical layer. A reader finds code by
  *behavior*, not by guessing which `controllers/`, `models/`, `services/` it was scattered across.
- Each feature is a **self-contained package**: entry point, logic, sub-modules, and assets together.
- **Prefer deep, fine-grained hierarchies.** Split a feature into sub-feature folders, and those into
  smaller units. **More folders and subfolders is good** — a folder per cohesive concern beats a flat
  folder of big files. When in doubt, add a folder.
- Only genuinely shared utilities go in `common/` (or `shared/`), grouped by area — and nothing
  feature-specific leaks in.

```
src/
  <feature-a>/
    <sub-feature-1>/     # split features into sub-features — deeper is better
      <unit>.<ext>       # one small, single-purpose unit
      <unit>.<ext>
    <sub-feature-2>/
    index.<ext>          # the feature's composition / entry
  <feature-b>/
  common/
    <area>/              # shared utilities, grouped by area
tests/                   # mirrors src/ exactly — folder for folder
docs/requirements/       # the WHAT and WHY, numbered
```

**Anti-pattern:** top-level `controllers/ models/ services/ utils/` that interleave every feature; or a
flat feature folder with a few 300-line files instead of many small ones in subfolders.

---

## 5. Small files are the default — name = contents

- **Small is preferred, always.** `≤ 300` lines is a **hard ceiling, not a target** — aim well under
  it. A unit that does one thing is often 20–150 lines. If a file approaches the ceiling, split it
  into a subfolder of smaller units **before** it gets there.
- **The name is a contract.** A file's contents must match its **file name**; a function's body must
  match its **function name** — nothing more. If you can't name it precisely, it's doing too much →
  split it until each piece has an exact, honest name.
- One clear responsibility per file/module/function. Prefer many small, composable, well-named units
  over a few large ones — every time.

---

## 6. A test for every function

- Every function ships with test(s). **No untested logic.** Writing the test is part of the change,
  not a follow-up.
- The **test tree mirrors the source tree**: `src/<feature>/<file>::fn` → `tests/<feature>/test_<file>`.
- Cover the **contract and the edges** — happy path, boundaries, failure modes, and concurrency
  (prove no race / no shared-state bleed where it applies).
- Tests are **deterministic** (set preconditions explicitly — never rely on order, time, or leftover
  state) and **fast**.
- A change isn't *done* until its tests pass. An environment-gated **skip** is fine; a **failure** isn't.
- The **whole suite must be green before merge**, on **every deployment target**. Loop it **N×** to
  flush flakiness — a flaky test is a real bug (usually a race or shared state), never noise to ignore.

---

## 7. Requirements first — no code without a doc

**Do not write any code before a requirements document for it exists.** This is a hard gate, for
humans and AI agents alike. If you are asked to build or change something and there is no requirement
for it, **write the requirement first** (or ask for it), get it agreed, *then* implement. "Just code
it" is not the process — an instruction in chat is the trigger to *write the requirement*, not to skip
it.

**One folder per module — every module owns its requirements.** Requirements are organized by **what
they belong to**, not as a flat pile of sequentially numbered files. Each module/feature has its own
folder and at least one requirement doc:

```
docs/requirements/
  README.md                    # the process + a by-module index
  <module>/
    <feature>.md               # Context · Requirement · Acceptance criteria · Notes
    <another-feature>.md
```

- Each doc states **Context · Requirement · Acceptance criteria · Notes**.
- **Append-only:** a change to a requirement is a *new* doc that supersedes the old (mark the old
  `Superseded by <path>`).
- **Name for what it is** — `<module>/<feature>.md`, not opaque numbers. The path *is* the id.
- Code and commits **reference the requirement** they implement (by path).

**Collecting a requirement (turn an instruction into a doc):** a chat instruction is raw input — the
doc is the agreed contract. Never build straight from the instruction; first write:

1. **Context** — the problem, who's asking, why now.
2. **Requirement** — what must be true when done, in plain terms (the *what*, not the *how*).
3. **Acceptance criteria** — checkable conditions that prove it's done.
4. **Notes** — constraints, risks, decisions, links.

Ask to fill any gap, confirm, *then* implement. If a request is ambiguous, the requirement doc is
where you resolve it — not the code.

---

## 8. Incremental delivery

- **One change at a time** — small, reviewable, and `main` stays releasable after each.
- No big-bang rewrites. Build behavior piece by piece, keeping it working throughout.

---

## 9. Naming & style

- Consistent casing per language; **descriptive** names; **namespace by feature**.
- Public surfaces (APIs, routes, files, events) named **predictably** (`<feature>/<verb>`).
- Use the language's standard formatter + linter; new code reads like the code already there.

---

## 10. Comments & docs

- Comment the **why** — intent, trade-offs, gotchas, failure modes — not the obvious *what*.
- Match the surrounding density. Every module/file states its responsibility in one line.
- Keep docs next to code and current; a stale doc is worse than none.

---

## 11. Config & secrets

- **All config from the environment** (or a config service). Never hard-code or commit secrets.
- **Runtime/generated state** (caches, tokens, build output, local data) stays **out of version control**.
- Defaults are explicit and documented. Changing a *default* usually only affects *fresh* state —
  existing/persisted instances keep their old values until migrated.

---

## 12. Errors & safety

- **Fail safe:** on partial failure, leave the system consistent — no orphaned or half-applied effects.
  Reconcile against the real external state before acting on it.
- Validate inputs at boundaries; guard edge cases.
- Irreversible or outward-facing actions are **deliberate**, logged, and reversible/recoverable.

---

## 13. Version control & releases

- Work on a **branch**; merge to `main` only when the suite is green; **tag** releases (semver-ish).
- **One logical change per commit** — atomic. Never bundle a fix with a refactor.
- **Automate deploy + test** so shipping is one repeatable command that verifies every target.

### Commit messages — Linux-kernel style

The kernel convention is the gold standard; use it everywhere.

**Subject:** `area: imperative summary`
- `area:` is the subsystem / feature touched — `auth:`, `cache:`, `deploy:`, `docs:` (nest if it
  helps: `net: tcp: ...`).
- **Imperative mood** — `Add`, `Fix`, `Remove`, `Move`; never `Added`/`Adds`/`Fixing`/`this commit`.
  It must complete: *"If applied, this commit will ___."*
- **≤ 50 characters** ideal, **~72 hard cap.** **No trailing period.**

**Body** — after exactly one blank line:
- **Wrap at 72 columns.**
- Explain **what** and **why** — the problem and the rationale — **not how** (the diff shows how).
- Imperative, present tense. Paragraphs are fine. No "I"/"we"/"this patch".

**Trailers** — at the end, after a blank line, one per line (machine-parseable):
- `Signed-off-by: Name <email>` — Developer Certificate of Origin (`git commit -s`).
- `Fixes: <12-hex> ("subject of the buggy commit")` — when fixing a specific earlier commit.
- `Reported-by:` · `Reviewed-by:` · `Tested-by:` · `Acked-by:` · `Co-developed-by:` · `Link:`.

```
cache: Evict entries past their TTL on read

Stale values were served indefinitely: the cache enforced only its size
cap, never age. Under steady load it never expired an entry, so a value
changed upstream stayed wrong until the next restart.

Drop an entry whose age exceeds the TTL when it is read, and emit a
metric so expiry is observable.

Fixes: a1b2c3d4e5f6 ("cache: Add in-memory store with size cap")
Signed-off-by: Dev Name <dev@example.org>
```

Subject = *what changed*; body = *why it was needed*; trailers = metadata. Keep it scannable in
`git log --oneline`.

### Automated deploy + test across targets

When the same software runs on more than one target (servers, accounts, tenants, environments),
deploying and testing each by hand doesn't scale and drifts.

- Keep a **registry** — one row per target (host, path, runtime, method, service). **Onboarding a new
  target is one row**, never a script edit.
- A **single command deploys to every target and runs the full suite on each**, with a clear
  **GREEN / non-zero-exit gate** (CI-usable) and **N× repeats** per target.
- The driver handles heterogeneity (local vs remote, pull vs copy, per-target runtime) so the *intent*
  ("ship and verify everywhere") is one action.
- **Release flow:** branch → green → merge to `main` → `deploy-test N` (ship + prove every target) →
  tag the release.

---

## Definition of done

- [ ] Requirement doc exists and is referenced.
- [ ] Built incrementally; `main` still works.
- [ ] No source file > 300 lines; code organized by feature (deep, small units).
- [ ] Every new function tested; suite green and deterministic.
- [ ] UX reviewed; hot paths measured; slow/blocking work off the critical path.
- [ ] Errors fail safe; config/secrets via env; nothing sensitive committed.
- [ ] Commit follows the kernel format.

## Adding a feature — generic checklist

1. Write the requirement doc (and index it).
2. Create the feature package — logic **and** its mirrored tests, together (small units, subfolders).
3. Wire it into the app's composition root; keep blocking work async/background.
4. Suite green; files ≤ 300; UX reviewed; hot paths measured.

## Working with legacy / reference code

- Treat the old system as **read-only**: study its behavior, then **re-implement cleanly**. Never copy
  files wholesale — carry the behavior, not the mess.

---

*Stack-specific patterns for this repository (framework, exact layout, examples) live in a project
companion (e.g. `PROJECT-PATTERNS.md`). This file stays generic so it travels to any project unchanged.*
