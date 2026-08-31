---
slug: "/Rules_integration"
title: 'Rules: integration'
---

- [Data import (IMPORT)](#data-import-import)
- [Data export (EXPORT)](#data-export-export)

## Data import (IMPORT)

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

    The rest of them are in the domain-logic article:
    `lsfusion_get_guidance(rules='logic')`.

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

## Data export (EXPORT)

### Choosing the export source

Data is exported with the [`EXPORT` operator](../language/EXPORT_operator.md).

1. Exporting a list of properties (`EXPORT FROM ...`) is used when the result is a single flat table of columns and its structure matches no existing form.

2. Exporting a form (`EXPORT formName ...`) is used when the export repeats an already existing form or when the result needs the object group hierarchy. The hierarchy is preserved only in **JSON** and **XML**; in the flat formats each object group produces a separate file, so for them the destinations are listed per group in the `TO` block — only for the groups that are wanted, since a group left out of the list is simply not exported.

3. A form created solely for an export should be declared next to the export action and should not be added to the navigator.

### Stating explicitly what shapes the result

1. The format should be stated explicitly even when **JSON** is intended: relying on the default makes the export depend on the reader remembering that default.

2. The `WHERE` condition should be stated explicitly. Without it the condition is the disjunction of all exported properties, so the export includes every object set with at least one field filled — which almost never matches the intended set of rows.

3. Column identifiers should be given explicitly (`columnId = expr`). The default `expr1`, ..., `exprN` ties the field names in the external format to the order of the expressions, so inserting a column in the middle of the list silently changes the export contract.

4. `ORDER` should be stated explicitly whenever the receiving side depends on the row order. Its expressions are arbitrary and need not be among the exported ones — a sorting expression is added to the internal query as a hidden column and does not reach the result — so a column should be added to the export only when the recipient needs it, not merely in order to sort by it.

5. In the hierarchical formats a property with a `NULL` value is omitted from the record (in **JSON** the key is absent, in **XML** the element), while the flat formats (**CSV**, **XLS**, **XLSX**, **DBF**) keep the column and write an empty cell. So in **JSON** a missing key means `NULL`, not a failed export (for form properties with `SHOWIF` inclusion follows the `SHOWIF` value instead: a non-`NULL` value can be omitted and a `NULL` one emitted).

6. When several scalar values are returned in one export (probe results, diagnostics), separate columns `EXPORT FROM a = ..., b = ...` are preferable to one concatenated string: a `NULL` drops only its own key, while in a `+` concatenation it nulls the whole result. To force the key's presence, wrap the value in `OVERRIDE ..., <default>` (when exporting a form, the property option `EXTNULL` may be used instead). An export of parameterless expressions keeps its single record even when every value is `NULL`; for rows generated by export parameters the default `WHERE` (disjunction) drops an all-`NULL` record — state the `WHERE` explicitly or add a constant column.

### Format options

1. Options whose default differs between formats should be set explicitly: the presence of a header row (`HEADER` / `NOHEADER`) in **CSV**, **XLS**, **XLSX**, the **CSV** separator (`;` by default) and the encoding (`CHARSET`, `UTF-8` by default and `CP1251` for **DBF**).

2. `NOESCAPE` in **CSV** may be used only when the separator is guaranteed not to occur in the data; otherwise `ESCAPE` should be kept.

3. The encoding should be determined by the receiving side's requirements rather than by the default: recipients of **DBF** files usually expect a single-byte encoding other than `UTF-8`.

### Result destination

1. The destination property in `TO` should be declared local to the export action and of a file class (`FILE`, `RAWFILE`, `JSONFILE`) rather than being a shared property: one property shared by several exports makes the result depend on the execution order.

2. The `System.exportFile` default is acceptable only for debugging and one-off exports.

3. When a form is exported to a flat format, destinations should be listed for every exported object group; the group of objects without a name is called `root`.

### Delivering the result

1. The action should be split into preparing the data, the `EXPORT` itself, and delivering the file to the recipient — writing it to the file system, sending it to an external system, or storing it in a property. This split allows the same export to be reused with different delivery methods.

2. For regular exports, building the file should be done in a separate action with no user interaction, so that it can be called both from a form and on a schedule.

### Examples

The first shows the property-list form: a flat result whose shape matches no
form, with the columns aliased, the selection in `WHERE` and an explicit
`ORDER`. The second shows the form form: an existing form exported to a
hierarchical format, its outer object passed with `OBJECTS`.

```lsf
exportShipments (Store store) {
    LOCAL exportedFile = FILE ();
    EXPORT CSV ';' HEADER FROM number = number(Shipment s), date = date(s), sum = sum(s)
        WHERE store(s) = store AND shipped(s)
        ORDER date(s)
        TO exportedFile;
}
```

```lsf
FORM exportOrders
    OBJECTS st = Store
    OBJECTS o = Order
    PROPERTIES(o) number, date
    FILTERS store(o) = st
;

exportOrders (Store store) {
    LOCAL exportedFile = FILE ();
    EXPORT exportOrders OBJECTS st = store JSON TO exportedFile;
}
```
