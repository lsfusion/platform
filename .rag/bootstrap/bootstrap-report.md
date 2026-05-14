# RAG Bootstrap Report

## Acceptance Required

This bootstrap classified **18** files as `paradigm` by default (15 via unrecognized sidebars category, 3 absent from sidebars). Misclassification will appear as retrieval bugs later.

**To merge this PR, the reviewer must add to the PR description:**

    BOOTSTRAP_DEFAULTS_REVIEWED: 18

Where the number matches the count shown above. This is an explicit acknowledgement that the defaulted lists were reviewed, not skimmed. Alternatively, add the same line to `.rag/bootstrap/acceptance.md`.

## Statistics

- Total `.md` files in docs/en: **345**
- `paradigm`: 161
- `language`: 119
- `how-to`: 65

## Files defaulted to "paradigm" (sidebars category not Paradigm/Language/How-to)

These files appear in `sidebars.js` but under categories like `install`, `Learning materials`, or single leaves (`IDE`, `Online_demo`) — none of {Paradigm, Language, How-to} ancestor. Classified as `paradigm` by fallback. If any are actually how-to or language reference, edit `manifest.json` before merge:

- `Automatic_installation`
- `Development_auto`
- `Development_manual`
- `Docker`
- `Examples`
- `Execution_auto`
- `Execution_manual`
- `IDE`
- `Install`
- `Learn`
- `Learning_materials`
- `Manual_installation`
- `Materials_management`
- `Online_demo`
- `Score_table`

## Files defaulted to "paradigm" (slug not found in sidebars at all)

These `.md` files exist in docs/en but `sidebars.js` doesn't list them. Classified as `paradigm` by fallback. Check whether they should be added to sidebars (in docusaurus repo) or whether classification needs override here:

- `ISCLASS_operator`
- `MCP_server`
- `Property_signature_ISCLASS`

## Orphan sidebars-entries (in sidebars, no `.md` file)

These slugs appear in `sidebars.js` but have no matching `.md` file in docs/en. Non-blocking; may indicate stale sidebar entries.

- `CLASS_operator`
- `Property_signature_CLASS`

## Sidebars snapshot provenance

```json
{
  "source_repo": "lsfusion/docusaurus",
  "source_path": "sidebars.js",
  "commit_sha": "5a455fa86790399531b08ab3420ab56557a6abda",
  "git_dirty": false,
  "fetched_at": "2026-05-14T13:04:09Z",
  "fetched_from": "/home/alex/IdeaProjects/lsfusion-aggregate/docusaurus"
}
```
