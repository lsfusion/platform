# AGENTS.md — lsFusion platform server

Module-specific rules for `server/`. The cross-cutting process, commit, issue,
verification, rebuild, sister-repository, and documentation rules in `../AGENTS.md`
still apply.

## Running the server in development mode

When starting BLB for local development or debugging, pass `-Dlsfusion.server.devmode=true`
(the `lsfusion.server.devmode` system property, exposed as `SystemProperties.inDevMode`).
The IDE's lsFusion server run configuration sets it automatically; for headless or manual
launches add it to the JVM args yourself. When unset it falls back to
`lsfusion.server.plugin.enabled`; an explicit `=false` disables dev mode even with the plugin enabled.

In dev mode:

- Some periodic system tasks are skipped (the `if (!inDevMode)` block in `getSystemTasks`), so
  maintenance/background jobs are less likely to interfere with breakpoints.
- Anonymous access to the API and UI is enabled and runs as the admin user (`enableAPI`/`enableUI`
  are forced to `2`).
- Report designs can be edited from the interactive `PRINT` view, and the cache for reading reports
  from resources is turned off, so edits are picked up without a restart.
- The client auto-reconnects when the connection is lost.

Don't enable it in production — it allows anonymous API/UI access as the admin user.

## Deprecating language syntax

When introducing a keyword rename, an alternative form, or removing a syntax construct:

1. Update `LsfLogics.g`: accept both forms during a deprecation window, or drop the old form for an outright removal.
2. **Deprecation only.** In `ScriptingLogicsModule.java`, desugar the old form into the new internal representation and append a migration hint to `warningList`. (For removals, skip this — the parser rejects the old form directly, and step 4 must land in the same commit so the platform doesn't fail to start.)
3. Update the IntelliJ plugin per the Sister repository checklist (grammar + lexer + regeneration + annotator).
4. Migrate the platform's own system `.lsf` modules (under `server/src/main/lsfusion/`) off the old form so the platform doesn't warn about itself on startup.
5. Update documentation per the Documentation section.
6. Land the platform change and self-migration together; coordinate the plugin commit via cross-link.

## System `.lsf` modules

- System `.lsf` modules shipped under `server/src/main/lsfusion/` must not emit deprecation warnings against their own code on startup. When a syntax form is deprecated, migrate those modules off the old form in the same change set (see Deprecating language syntax above).
