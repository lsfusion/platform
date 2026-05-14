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
- `docs/{en,ru}/` — primary documentation. The sibling `../docusaurus/` consumes it — see the Documentation section for the rules.

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

- Short imperative subject, ≤72 chars. Suffix `(closes #NNNN)` when the commit fully resolves a tracked issue.
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
- `v4`, `v5`, `v6` are LTS maintenance branches.
- Backport pattern is **merge-up**: cherry-pick or land the fix on the oldest still-supported version branch, then merge that branch forward — `v4 → v5 → v6 → master`. Don't apply the same commit independently to multiple branches; that creates divergent histories that bite during the next merge.
- Never force-push to `master` or to any `vN` branch.

## GitHub issues

Significant, user-visible platform changes get a GitHub issue on `lsfusion/platform` **before** the commit lands. The commit that resolves it ends its subject with `(closes #NNNN)` — **release notes are generated from these closing references**, so missing or vague issues directly degrade the changelog.

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

Bug reports add a `### Fix` section naming the affected files/methods and the resolution direction.

### Audience and content

The issue is read by **application developers using the platform**, not by maintainers debugging internals. Lead with the developer-facing surface (new syntax, option, default, behavior); keep implementation details (class names, refactoring shape) in the commit body.

- **Include a short `.lsf` example** (3–10 lines, fenced) whenever the change introduces, modifies, or deprecates syntax. For deprecations show old vs new side-by-side; for bug fixes show the minimal reproducer.
- Use developer-facing vocabulary (`operator`, `form`, `property`, `action`, `session`) — not platform internals (`aspect`, `post-process visitor`, `ImMap`).

## Pull requests

Workflow is master-direct (commits push to `master` after review). When a PR is opened, title and body follow the same conventions as commits: short imperative title, body explaining the why, and `closes #NNNN` to link the driving issue.

## Documentation and MCP guidance

A user-visible platform change is not done when the code compiles — the documentation under `docs/` must reflect the new behavior, **either in the same commit or in an immediate follow-up before the change is published**. As a rule of thumb: if the change warrants a GitHub issue (see above), it warrants a docs update too.

- Update both `docs/en/` and `docs/ru/` in lockstep — the two language versions are translations of the same content, not separate documents.
- The sibling `../docusaurus/sidebars.js` is the navigation source of truth; update it when adding pages or restructuring sections.
- The rest of `../docusaurus/` is a derived copy — don't edit it.
- Docs-specific structural rules (Language → Paradigm → Guide → How-to ordering, cross-link direction, etc.) live in `docs/AGENTS.md`.

The MCP server (`../mcp/`) exposes `brief.md` and `rules.md` (the "guidance") that prime AI assistants writing `.lsf` code. Touch only when a platform change materially alters how developers reason about, structure, or write `.lsf` — a new core operator/paradigm with broad applicability, a deprecation of something the guidance recommends, or a default change that breaks existing examples. Niche options, isolated bug fixes, internal plumbing, anything confined to a subsystem most app developers never touch — leave alone. When in doubt, leave alone; the guidance is deliberately compact and noise hurts more than missing edges. If updating, mirror the change in `docs/` first (the guidance distills from docs, not the reverse).

## Code conventions

- Don't add error handling, fallbacks, or validation for conditions that can't happen. Trust internal contracts; validate only at system boundaries (user input, external APIs).
- Don't introduce abstractions beyond what the task requires. Three similar lines is fine — a premature helper is not.
- Minimize call-tree depth and inter-function coupling. A helper called from only one place, doing one obvious thing, is usually clearer inlined; delegation chains where each level just forwards arguments make code harder to follow than one self-contained function. Exception: genuinely primitive or broadly-reused utilities (collection operations, formatting, well-known infrastructure helpers) — coupling to those is cheap and welcome.
- Don't keep backwards-compatibility shims for unreleased changes. Don't rename unused identifiers to `_x` — just delete the dead code.
- Don't comment what the code already says. Comments belong only where **why** is non-obvious: a hidden constraint, a workaround, a subtle invariant.
- No emojis in source, commits, or comments unless the user explicitly asks.
- Prefer editing existing files to creating new ones.
- System `.lsf` modules shipped under `server/src/main/lsfusion/` must not emit deprecation warnings against their own code on startup. When a syntax form is deprecated, migrate those modules off the old form in the same change set (see Deprecating language syntax).

## Deprecating language syntax

When introducing a keyword rename, an alternative form, or removing a syntax construct:

1. Update `LsfLogics.g`: accept both forms during a deprecation window, or drop the old form for an outright removal.
2. **Deprecation only.** In `ScriptingLogicsModule.java`, desugar the old form into the new internal representation and append a migration hint to `warningList`. (For removals, skip this — the parser rejects the old form directly, and step 4 must land in the same commit so the platform doesn't fail to start.)
3. Update the IntelliJ plugin per the Sister repository checklist (grammar + lexer + regeneration + annotator).
4. Migrate the platform's own system `.lsf` modules (under `server/src/main/lsfusion/`) off the old form so the platform doesn't warn about itself on startup.
5. Update documentation per the Documentation section.
6. Land the platform change and self-migration together; coordinate the plugin commit via cross-link.

## Bumping the API version

`BaseUtils.getApiVersion()` in `api/src/main/java/lsfusion/base/BaseUtils.java` gates the RMI handshake between BLB and every RMI client (desktop JVM, web-client servlet JVM, external Java integrations). `BaseUtils.checkClientVersion()` compares `ServerSettings.apiVersion` against client-supplied `NavigatorInfo.apiVersion` via `.equals()` — mismatch hard-rejects the session.

The gate protects the **RMI wire only**. GWT RPC between the browser and the web-client servlet is a separate protocol governed by GWT codegen and the servlet's redeploy; pure GWT-layer changes (`web-client/.../gwt/`) that don't touch `api/` don't need a bump.

### Bump (+1, in the same commit) when

**Remote interface signature changes.** Any method add/remove/rename, parameter/return type change, or `throws` clause change in one of: `RemoteLogicsInterface`, `RemoteLogicsLoaderInterface`, `RemoteConnectionInterface`, `RemoteFormInterface`, `RemoteClientInterface`, `RemoteNavigatorInterface`, `RemoteSessionInterface`, `RemoteRequestInterface`, `RemoteInterface` (all under `api/.../interop/`).

**Serializable wire-format change.** Any change to the serialized form of a class reachable — directly or transitively — from a Remote method parameter or return type. Usually under `api/.../interop/`, but a Serializable field can reach elsewhere in `api/` (e.g. `lsfusion.base.*`); reachability matters, not path. Triggers: add/remove non-`transient` field, change field type, add/remove/rename an `enum` constant, introduce or change `serialVersionUID`, change `extends`/`implements`, or modify `readObject` / `writeObject` / `readResolve` / `writeReplace`.

**New polymorphic subclass sent over RMI.** Java serialization writes the runtime class name; an old peer without the class hits `ClassNotFoundException`. Hierarchies that flow this way: `ClientAction` in `interop/action/` (server returns subclasses inside `ServerResponse`), `FormEvent`/`InputEvent` in `interop/form/event/`, `Authentication` in `interop/connection/authentication/`, and any sealed-by-convention hierarchy where the server picks a subclass and the client must deserialize it.

**Protocol-semantics change.** Same bytes, different meaning — flipped default, previously-optional field now required, ignored value now interpreted, repurposed exception code. Both peers deserialize fine and then disagree on behavior.

### Don't bump

- Refactors that don't touch `api/`.
- Changes inside an RMI implementation that don't change the interface or any Serializable wire type.
- `transient` / `static` fields, caches, lazy holders. Plain `private` is **not** exempt — only `transient` is.
- Private methods, non-serialized inner classes, helpers outside the Remote API and serialized form.
- GWT-layer changes (`web-client/.../gwt/`) that don't touch `api/`.
- `.lsf`, build, test, docs.

**Quick fallback** when transitive reachability isn't obvious in 30 seconds: if the changed class is used by or contained in a known wire payload (`ServerResponse`, `ClientAction` subtree, `FormClientData`, `NavigatorInfo`, `ServerSettings`), assume reachable and bump unless you can prove otherwise. Call out the bump in the commit body so the reviewer can confirm the trigger.

## Risk discipline

Treat the following as requiring explicit user authorization each time, even if a similar action was authorized earlier in the session:

- `git push` (especially to remote branches); force-pushes are never automatic.
- Destructive operations: `git reset --hard`, `git clean -fd`, branch deletion, dropping database tables, `rm -rf` of working trees.
- Anything that publishes outside the local machine: opening or commenting on GitHub PRs and issues, releases, deployments, messages to external services.

Local, reversible work — editing files, running tests, regenerating parsers, creating local commits — doesn't need per-action confirmation.

## Per-agent extension files

- `CLAUDE.md`, `GEMINI.md` at the repo root are thin wrappers that import this file (Claude Code and Gemini CLI don't auto-read `AGENTS.md` out of the box).
- `.github/copilot-instructions.md` — if added, GitHub Copilot in IDEs that don't read `AGENTS.md` will pick it up. Keep canonical guidance here, and let that file be a short reference or a generated mirror.
- `.cursor/rules/*.mdc` — if added, lets Cursor apply scoped rules. Cursor also reads `AGENTS.md` natively, so most guidance can stay here.
