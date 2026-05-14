@AGENTS.md

## Claude Code specifics

- Persistent cross-session notes live under `.claude/memory/` (per-project, in this repo) and `~/.claude/memory/` (user-global). Use them for feature-specific lore that doesn't belong in the abstract `AGENTS.md`.
- During normal sessions only this file is loaded automatically; `AGENTS.md` is pulled in via the `@`-import above. If you edit `AGENTS.md`, no further wiring is needed.
- For documentation-only tasks, the additional rules in `docs/AGENTS.md` also apply.
