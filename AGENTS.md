# AGENTS.md — lsFusion platform

General engineering and process notes for AI coding agents (Codex, Cursor, Claude Code, Gemini CLI, GitHub Copilot). Feature-level lore stays in commit messages, code, and per-agent memory — this file is for durable cross-cutting process knowledge.

For documentation-only tasks under `docs/`, additional rules apply — see `docs/AGENTS.md`. That file **complements** the process rules here (commit conventions, issue policy, risk discipline still apply to docs commits); it does not replace them.

**Workspace assumption.** Several sections reference sibling directories (`../plugin-idea/`, `../docusaurus/`, `../mcp/`, application projects like `../mycompany/`). They assume the canonical `lsfusion-aggregate/` super-workspace where `platform/` sits next to those repos. In a standalone clone the sibling-path instructions won't resolve — adjust paths or skip those steps as needed.

## Repo layout

Maven multi-module project. Top-level modules: `api/`, `ai/`, `build/`, `desktop-client/`, `server/`, `web-client/`, `docs/`.

- `server/` — Java server, core platform logic.
  - `server/src/main/java/lsfusion/server/...` — Java sources.
  - `server/src/main/lsfusion/` — system `.lsf` modules shipped with the platform (`Service.lsf`, `SystemEvents.lsf`, `Chat.lsf`, etc.).
  - `server/src/main/antlr3/lsfusion/server/language/LsfLogics.g` — ANTLR3 grammar source. Generated parser/lexer is written into `src/main/java` (path configured in `server/pom.xml`); never hand-edit generated files.
- `web-client/` — GWT web client.
- `desktop-client/` — Swing/Java desktop client.
- `docs/<type>/{en,ru}/` — primary documentation, type-first (type ∈ `language`, `paradigm`, `how-to`, `brief`, `rules`). The sibling `../docusaurus/` consumes it — see the Documentation section for the rules.

## Sister repository

The IntelliJ plugin lives next door at `../plugin-idea/`. Grammar changes in `LsfLogics.g` typically require parallel updates to the plugin's `LSF.bnf` (and sometimes `LSF.flex` if new keywords are introduced). Coordinate commits across the two repos (cross-link in commit messages, or close the same GitHub issue from both sides).

## Build and run

The platform is a framework — runtime needs to be paired with an lsFusion application from a sibling repo (`../mycompany/`, `../erp/`, …).

### Build commands

```
# Build (skip tests). Add -am to also rebuild upstream deps.
mvn -pl server install -DskipTests              # server only (deps already built)
mvn -pl server -am clean install -DskipTests    # server + ai/api/build/base
mvn -pl web-client gwt:compile -q               # GWT client only
# Append -o for offline mode (deps must already be cached).

# Run JUnit tests (live in server/src/test/ and api/src/test/).
mvn -pl <module> test                           # or -am test to rebuild deps first
```

`-DskipTests` is a build verification, not a test run — don't claim "tests passed" in a commit body unless a `test` target actually ran (and say which one).

ANTLR3 sources regenerate during `generate-sources` via `antlr3-maven-plugin`. A clean build (or just `generate-sources`) is required after `LsfLogics.g` changes before new keywords reach downstream code; no regen is needed for `.lsf`-only edits.

### When to rebuild and restart

A "verification" with the wrong restart verifies the old code. Use this matrix:

| Change | Rebuild | Restart |
|---|---|---|
| Server Java | `mvn -pl server -am install -DskipTests` | BLB |
| ANTLR grammar (`LsfLogics.g`) | `mvn -pl server -am clean install -DskipTests` | BLB |
| Platform's own `.lsf` under `server/src/main/lsfusion/` | `mvn -pl server resources:resources` (or a full server rebuild) | BLB |
| Application `.lsf` in a sibling app (`../mycompany/`, `../erp/`, …) | none | BLB |
| Web-client Java / GWT | `mvn -pl web-client gwt:compile` | Jetty (or page reload if hot-deploy is on) |
| `api/` protocol | rebuild `api/` + dependent server/client modules together | BLB **and** Jetty |
| Docs only (`docs/`) | none | none |

### Topology

Three cooperating processes:

- **BLB** (`lsfusion.server.logics.BusinessLogicsBootstrap`) — lsFusion server. Loads `.lsf` modules from the application project (e.g. `../mycompany/`, also its working directory), connects to Postgres, exposes RMI. Classpath built from `server/target/`; needs `--add-opens=java.base/java.{util,lang}=ALL-UNNAMED`.
- **Jetty** — serves the GWT web client, proxies HTTP/RMI to BLB. Working dir `platform/`; listens on `http://localhost:8080/main`.
- **Postgres** — local; `compose.yaml` defines the canonical docker stack.

Normally started from IDE run configurations. For headless automation, capture each command line once via `cat /proc/<pid>/cmdline | tr '\0' '\n'` and re-exec; shape is stable across sessions. Redirect stdout/stderr to logs and wait for the readiness markers below.

### Smoke tests

- BLB ready: tail its log for `Application '<name>' is ready` / `Server started` / `Ready for connections`.
- Jetty ready: `curl -sI http://localhost:8080/main` returns a 2xx/3xx status — a 3xx to a login URL is normal. A 200 alone does not confirm the lsFusion UI loaded; that requires the browser/CDP check below.
- Poll with a bounded loop (e.g. 30 iterations × 5s sleep) until both signals show up before driving the UI.

### UI / visual verification (headless Chrome via CDP)

For GWT-rendered forms, popovers, screenshots, layout — anything a log message can't confirm — run Chrome headlessly:

```
google-chrome --headless=new --remote-debugging-port=9333 \
  --user-data-dir=/tmp/chrome-profile-headless --window-size=1280,800 &
```

Persistent `--user-data-dir` keeps a one-time login across sessions. Open a tab (modern Chrome requires `PUT`):

```
curl -s -X PUT "http://localhost:9333/json/new?http://localhost:8080/main"
```

Or attach to an existing tab via `GET /json/list` and drive `Page.navigate`. The response carries a per-tab `webSocketDebuggerUrl`; drive JS evaluation, clicks, and screenshots over that websocket via CDP (`Runtime.evaluate`, `Page.captureScreenshot`, or in-page `html-to-image` / `html2canvas`).

A check must produce **evidence**: a saved screenshot, a DOM assertion (expected element with expected text), a console-error count, or a confirmed visible form name. Opening a tab without an obvious error is **not** verification — the page may show a login screen, stale cache, or a calm-looking error component. Record evidence (or its absence) in the commit body. Use only when the change is genuinely UI-shaped; for backend-only changes, log-based smoke tests are enough.

## Commits

- Short imperative subject in English, ≤72 chars. Suffix `(closes #NNNN)` when the commit fully resolves a tracked issue.
- Body explains **why** — motivation, design constraints, alternatives rejected. The diff already shows what.
- Multi-paragraph bodies are normal for non-trivial changes.
- Never amend or force-push commits that may have been pulled by others. Always create a new commit on top.
- Never pass `--no-verify` or otherwise skip hooks unless the user explicitly asks. If a pre-commit hook fails, fix the underlying issue and create a **new** commit — not `--amend`, because the failed commit never landed.
- When the change is AI-assisted, include a `Co-Authored-By:` trailer naming the model.
- Stage new files (`git add <path>`) at creation, not at commit time. Pending work then shows as `A`/`M` in `git status` instead of `??`, and can't silently fall out of the commit.
- Before each commit, run `git status`, `git diff`, and `git diff --cached`. Stage only files relevant to the change; never sweep up unrelated working-tree edits.
- For non-trivial changes, record in the commit body which verifications ran (compile, smoke test, CDP render, click-through) and which were skipped and why. Be precise: `install -DskipTests` is a build verification, not a test run — don't write "tests passed" unless the suite actually executed.

Use a HEREDOC so newlines and trailers are preserved:

```
git commit -m "$(cat <<'EOF'
Subject line (closes #NNNN)

Body paragraph explaining the why.

Co-Authored-By: <model name> <agent-noreply-email>
EOF
)"
```

## Branches and backports

- `master` is the active development line.
- The two `vN` branches immediately preceding `master` are the supported LTS maintenance branches; older `vN` branches are end-of-life.
- **A bugfix is a backport candidate — check it alongside plugin and docs impact.** Just as a change prompts you to ask whether the plugin (`LSF.bnf`/`LSF.flex`) or `docs/` need a parallel update, a fix to a defect prompts you to ask whether it should land on the supported `vN` maintenance branches too, not just `master`. If the bug predates the current development line and is present in one or more maintained releases, name the oldest still-supported branch it affects and **suggest** backporting from there. Surface it as a recommendation, not an action — a backport changes what ships in a maintenance release, so confirm with the user before touching a `vN` branch. New features, syntax additions, and intentional behavior changes normally stay on `master` only.
- Backport pattern is **merge-up**: cherry-pick or land the fix on the oldest still-supported version branch, then merge that branch forward through each successive `vN` up to `master`. Don't apply the same commit independently to multiple branches; that creates divergent histories that bite during the next merge.
- Never force-push to `master` or to any `vN` branch.

## GitHub issues

Significant, user-visible platform changes get a GitHub issue on `lsfusion/platform` **before** the commit lands. The commit that resolves it ends its subject with `(closes #NNNN)` — **release notes are generated from these closing references**, so missing or vague issues directly degrade the changelog.

A `closes #NNNN` (or `fixes` / `resolves`) reference in a commit message makes GitHub **close the issue automatically** once the commit lands on the default branch (`master`) — it happens on push, so don't close the issue by hand; just land the commit with the reference. Keep the resolving change and its documentation in that **one** commit (carry the `closes #NNNN` there) rather than splitting the docs into a separate follow-up commit.

Issue creation is a remote publishing action — it requires explicit user authorization (see Risk discipline). When publishing isn't authorized but the change warrants an issue, prepare the `github_issue*.md` draft locally and either commit the change without a `closes #NNNN` reference or leave a TODO for the user to wire it up post-publish. **Never fabricate `closes #NNNN` against an issue that hasn't actually been opened** — the changelog generator will follow it to a broken link.

Create an issue when the change is one of:

- a new language operator, keyword, option, or syntax form;
- a behavioral change visible to lsFusion application developers (semantics, defaults, deprecations);
- a server-side feature with user-facing impact (logging, scheduling, integrations, security, API);
- a non-trivial fix to externally observable behavior (wrong query results, broken form rendering, race conditions affecting users).

Don't create an issue for:

- internal refactors that don't change behavior;
- typo fixes, comment edits, dependency bumps without behavior change;
- build/devtool tweaks invisible to platform users;
- migrating the platform's own system `.lsf` modules to a new spelling of a syntax already covered by an earlier issue.

Issues are drafted as local `github_issue*.md` files before posting. Standard format:

```
### Title
<short imperative title>

### Description
<context, developer-facing behavior or syntax spec, with a short example>

### Reason
<motivation: why this matters, what it unblocks or fixes>
```

Bug reports may add a `### Fix` section, but stated in developer-facing terms (what observable behavior changes) — not the affected files, classes, or methods.

### Audience and content

The issue is read by **application developers using the platform**, not by maintainers debugging internals. Lead with the developer-facing surface (new syntax, option, default, behavior). By default an issue carries **no implementation specifics** — file/class/method names, internal grammar-rule or collection names, refactoring shape — those belong only in the commit body, never in the issue.

- **Include a short `.lsf` example** (3–10 lines, fenced) whenever the change introduces, modifies, or deprecates syntax. For deprecations show old vs new side-by-side; for bug fixes show the minimal reproducer.
- Use developer-facing vocabulary (`operator`, `form`, `property`, `action`, `session`) — not platform internals (`aspect`, `post-process visitor`, `ImMap`).

## Pull requests

Workflow is master-direct (commits push to `master` after review). When a PR is opened, title and body follow the same conventions as commits: short imperative title, body explaining the why, and `closes #NNNN` to link the driving issue.

## Documentation and MCP guidance

A user-visible platform change is not done when the code compiles — the documentation under `docs/` must reflect the new behavior, **either in the same commit or in an immediate follow-up before the change is published**. As a rule of thumb: if the change warrants a GitHub issue (see above), it warrants a docs update too.

- The docs layout is type-first: `docs/<type>/{en,ru}/` (type ∈ `language`, `paradigm`, `how-to`, `brief`, `rules`). Update both language versions of a page in lockstep — they are translations of the same content, not separate documents.
- `docs/sidebars.js` (in this repo) is the navigation source of truth; update it when adding pages or restructuring sections. The sibling `../docusaurus/` is a derived copy — don't edit it; its own `sidebars.js` is just a loader for `docs/sidebars.js`.
- Docs-specific structural rules live in `docs/AGENTS.md`, and per-type rules in `docs/<type>/AGENTS.md`.

The AI guidance is published as documentation pages — `docs/brief/{en,ru}/Brief.md` (the concise capability map) and `docs/rules/{en,ru}/Rules.md` (the task rules) — and the MCP `lsfusion_get_guidance` tool serves them by fetching the published pages (`../mcp/` no longer ships separate `brief.md`/`rules.md`). Touch these pages only when a platform change materially alters how assistants or developers reason about, structure, or write `.lsf` — a new core operator/paradigm with broad applicability, a deprecation of something the guidance recommends, or a default change that breaks existing examples. Niche options, isolated bug fixes, internal plumbing, anything confined to a subsystem most app developers never touch — leave alone. When in doubt, leave alone; the guidance is deliberately compact and noise hurts more than missing edges. The guidance distills from the docs, not the reverse.

## Code conventions

- Don't add error handling, fallbacks, or validation for conditions that can't happen. Trust internal contracts; validate only at system boundaries (user input, external APIs).
- Don't introduce abstractions beyond what the task requires. Three similar lines is fine — a premature helper is not.
- Minimize call-tree depth and inter-function coupling. A helper called from only one place, doing one obvious thing, is usually clearer inlined; delegation chains where each level just forwards arguments make code harder to follow than one self-contained function. Exception: genuinely primitive or broadly-reused utilities (collection operations, formatting, well-known infrastructure helpers) — coupling to those is cheap and welcome.
- Don't keep backwards-compatibility shims for unreleased changes. Don't rename unused identifiers to `_x` — just delete the dead code.
- Don't comment what the code already says. Comments belong only where **why** is non-obvious: a hidden constraint, a workaround, a subtle invariant.
- No emojis in source, commits, or comments unless the user explicitly asks.
- Prefer editing existing files to creating new ones.
- Module-specific code/process rules live in the module's own `AGENTS.md`, auto-loaded when working there: language-syntax deprecation and the system `.lsf` deprecation-warning policy in `server/AGENTS.md`; API-version bumping in `api/AGENTS.md`.

## Risk discipline

Treat the following as requiring explicit user authorization each time, even if a similar action was authorized earlier in the session:

- `git push` (especially to remote branches); force-pushes are never automatic.
- Destructive operations: `git reset --hard`, `git clean -fd`, branch deletion, dropping database tables, `rm -rf` of working trees.
- Anything that publishes outside the local machine: opening or commenting on GitHub PRs and issues, releases, deployments, messages to external services.

Local, reversible work — editing files, running tests, regenerating parsers, creating local commits — doesn't need per-action confirmation.

## Per-agent extension files

- `CLAUDE.md`, `GEMINI.md` at the repo root are thin wrappers that import this file (Claude Code and Gemini CLI don't auto-read `AGENTS.md` out of the box).
- Module subdirs with their own scoped rules carry a local `AGENTS.md` plus `CLAUDE.md`/`GEMINI.md` wrappers, auto-loaded by ancestry when working in that subtree: `api/` (API-version bumping), `server/` (language-syntax deprecation, system `.lsf` policy), `docs/` (documentation rules, with its own per-type split).
- `.github/copilot-instructions.md` — if added, GitHub Copilot in IDEs that don't read `AGENTS.md` will pick it up. Keep canonical guidance here, and let that file be a short reference or a generated mirror.
- `.cursor/rules/*.mdc` — if added, lets Cursor apply scoped rules. Cursor also reads `AGENTS.md` natively, so most guidance can stay here.
