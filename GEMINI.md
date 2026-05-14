@AGENTS.md

## Gemini CLI specifics

- Gemini CLI loads `GEMINI.md` automatically and follows `@`-imports. The canonical guidance lives in `AGENTS.md`, imported above.
- To make Gemini CLI read `AGENTS.md` directly without this wrapper, set in `.gemini/settings.json`:

  ```json
  { "context": { "fileName": ["GEMINI.md", "AGENTS.md"] } }
  ```

- For documentation-only tasks, the additional rules in `docs/AGENTS.md` also apply.
