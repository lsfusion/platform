# AGENTS.md — lsFusion platform server

Module-specific rules for `server/`. The cross-cutting process, commit, issue,
verification, rebuild, sister-repository, and documentation rules in `../AGENTS.md`
still apply.

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
