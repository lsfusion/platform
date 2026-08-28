---
slug: "/Rules_import"
title: 'Rules: data import (IMPORT)'
---

## Import rules (`IMPORT`)

1. Before working with `IMPORT`, the assistant MUST identify
   elements in this order:
   - module and namespace that own the import flow
   - target classes that will be created or updated
   - staging properties used during import
   - import actions
   - import forms, if the payload is hierarchical

2. The assistant MUST choose the import style intentionally:
   - flat files (`CSV`, `XLS`, `DBF`, `TABLE`)
     -> prefer `IMPORT ... TO` or `FIELDS`
   - nested `JSON` / `XML`, parent-child structures,
     namespaces, or `EXTID` mapping
     -> prefer form import
   - row-at-a-time integration responses
     -> prefer `FIELDS ... DO`

3. For flat imports that need validation, deduplication,
   multi-pass processing, or post-processing,
   the assistant SHOULD stage data into `LOCAL` properties
   first, usually by `INTEGER` row,
   then process it in a separate
   `FOR imported(INTEGER i)` pass.

4. The assistant SHOULD use `FIELDS ... DO`
   when imported values are consumed only once
   and introducing reusable local properties
   would add noise.

5. The assistant SHOULD specify column mappings explicitly
   when the external template is fixed or sparse.

   Sequential mapping without explicit column IDs
   is acceptable only when column order itself
   is the agreed interface.

6. For form import, the assistant MUST declare
   a dedicated import form before use.

   The form MUST use one object per object group
   with numeric or concrete user classes.

   The form SHOULD mirror the external structure with:
   - `FILTERS` for parent-child links
   - `EXTID`, `FORMEXTID`, groups, and `ATTR`
     only where the external schema requires them

   The assistant MUST remember that importing into a form
   cancels pending changes to imported form properties
   in the current session.

7. The assistant MUST choose format options explicitly
   when the external contract depends on them:
   - `HEADER` / `NOHEADER`
   - `SHEET`
   - `CHARSET`

   The assistant SHOULD prefer `HEADER`
   for stable `CSV` / `XLS` templates,
   because `NOHEADER` can silently map missing
   or mistyped columns to `NULL`.

8. The assistant MUST validate referenced business keys
   before creating or updating persistent objects.

   Typical keys in this project are `id`, `number`,
   partner or item codes, and external references.

   Each reference MUST be checked
   in a separate `FOR`
   using `GROUP SUM 1 BY`
   over the imported key values.

   If possible, the assistant SHOULD NOT write
   resolved references to a separate `LOCAL`
   before the main import logic.

   Missing master data or malformed payloads
   MUST stop the import or surface a clear error.

9. The assistant SHOULD separate raw import
   from domain resolution:
   - first parse the file or payload
     into locals or an import form
   - then check references such as
     item, partner, status, type, or other lookups
   - only then create or update domain objects

10. For user-started batch imports and external integrations,
    the assistant SHOULD isolate persistence in `NEWSESSION`,
    and SHOULD `APPLY;` after the domain writes of one import.

    Three of the change-session rules bite on every import,
    so they are stated here rather than left to a second lookup:
    - a buffer filled in the upper session reaches the new one
      only through `NESTED`, on the operator or on the
      `DATA LOCAL NESTED` declaration itself, and does not
      reach it at all under `NEWSQL`, which migrates nothing;
    - after `APPLY;` the assistant MUST check `canceled()`
      before treating the import as done;
    - a failure raised by `APPLY` itself is already shown to an
      interactive user, so an interactive import MUST NOT report
      it twice; an API or background import MUST surface it
      itself, through `applyMessage()` or an exception.

    The rest of them are in the change-session article:
    `lsfusion_retrieve_docs(type='rules', query='change sessions')`.

11. The assistant MUST NOT partially persist
    a failed import silently. For failures the assistant
    detects on its own (missing references, malformed
    payload, pre-`APPLY` validation), it SHOULD use
    `MESSAGE`, `RETURN`, `throwException`, or an explicit
    failure flag, consistent with the caller:
    - interactive import -> `MESSAGE`
    - API or background integration
      -> exception or explicit failure state

12. For create-or-update synchronization imports,
    the assistant MUST separate object creation
    from property updates.

    The assistant MUST make one separate pass
    that only creates the missing objects. A `FOR` is one way
    to write it; the bulk `NEW ... WHERE ... TO` form creates
    an object per matching set in a single operation and is
    the better one wherever it fits.

    If imported key values may be non-unique,
    the creation pass SHOULD iterate by grouped keys
    using `GROUP SUM ... BY`
    rather than by raw imported rows.

    The assistant MUST then update the properties of the
    matched objects in a second separate pass — a direct
    `<- ... WHERE` changes every matching set at once, and a
    `FOR` is needed only where the body does something a
    set-based change cannot.

    The assistant MUST NOT mix object creation
    and property updates in the same pass
    for synchronization imports.

    If full synchronization is required,
    the assistant SHOULD add an explicit delete step.

13. If `LOCAL` staging properties
    are used only in one import action,
    the assistant MUST declare them
    inside that action.

    The assistant SHOULD NOT lift such `LOCAL` properties
    to module scope without need.

    Exception:
    a `LOCAL` property may be declared outside the action
    only when it must be used by an import form
    or reused by several related actions.
