# Bootstrap acceptance

BOOTSTRAP_DEFAULTS_REVIEWED: 18

## Review notes

Reviewed all 18 slugs that classify.py defaulted to `paradigm` (15 via
unrecognized sidebars category, 3 absent from sidebars). See
`bootstrap-report.md` for the full list.

### Manual overrides (applied directly to `docs/manifest.json`)

- `ISCLASS_operator` → `language` (was paradigm by default-not-in-sidebars)
- `Property_signature_ISCLASS` → `language` (was paradigm by default-not-in-sidebars)

Both are operator-reference docs. They aren't in `sidebars.js` because the
sidebar still lists their renamed predecessors `CLASS_operator` and
`Property_signature_CLASS` (which now appear as orphan sidebars-entries in
the report — fix lives in the Docusaurus repo, not here).

### Accepted as-is (`paradigm` fallback OK)

- Install / setup docs (8): `Install`, `Automatic_installation`, `Manual_installation`,
  `Development_auto`, `Development_manual`, `Execution_auto`, `Execution_manual`, `Docker`.
- Learn intro / index leaves (4): `Learn`, `IDE`, `Online_demo`, `Learning_materials`.
- Example walkthroughs (3): `Examples`, `Score_table`, `Materials_management`.
- New doc not yet in sidebars (1): `MCP_server` — leave as paradigm for now;
  reclassify if/when it grows enough content to warrant `how-to`.

Total after overrides: 159 paradigm / 121 language / 65 how-to.
