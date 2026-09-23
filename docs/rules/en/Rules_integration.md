---
slug: "/Rules_integration"
title: 'Rules: integration'
---

- [Data import (IMPORT)](#data-import-import)
- [Data export (EXPORT)](#data-export-export)

## Data import (IMPORT)

1. The assistant MUST choose the import style intentionally:
   - flat files (`CSV`, `XLS`, `DBF`, `TABLE`)
     -> prefer `IMPORT ... TO` or `FIELDS`
   - nested `JSON` / `XML`, parent-child structures,
     namespaces, or `EXTID` mapping
     -> prefer form import
   - row-at-a-time integration responses
     -> prefer `FIELDS ... DO`

2. For flat imports that need validation, deduplication,
   multi-pass processing, or post-processing,
   the assistant SHOULD stage data into `LOCAL` properties
   first, usually by `INTEGER` row,
   then process it in a separate
   `FOR imported(INTEGER i)` pass.

3. The assistant SHOULD use `FIELDS ... DO`
   when imported values are consumed only once
   and introducing reusable local properties
   would add noise.

4. The assistant SHOULD specify column mappings explicitly
   when the external template is fixed or sparse.

   Sequential mapping without explicit column IDs
   is acceptable only when column order itself
   is the agreed interface.

5. For form import, the assistant MUST declare
   a dedicated import form before use.

   The form MUST use one object per object group
   with numeric or concrete user classes.

   The form SHOULD mirror the external structure with:
   - `FILTERS` for parent-child links
   - `EXTID`, `FORMEXTID`, groups, and `ATTR`
     only where the external schema requires them —
     a root-level `JSON` array is one such place (rule 6)

   The assistant MUST remember that importing into a form
   cancels pending changes to imported form properties
   in the current session.

6. For a root-level `JSON` array the assistant MUST give
   the object group of the import form
   the export/import name `value`
   (`OBJECTS receipts = INTEGER EXTID 'value'`):
   the platform reads such a file as `{ "value" : [ ... ] }`
   (see [predefined value](../paradigm/Structured_view.md#value)).

   An object group whose
   [export/import name](../paradigm/Structured_view.md#extid)
   matches no key of the file imports zero records
   with no error or warning, so the assistant MUST check
   a new import form on a non-empty sample.

7. After a form import the assistant MUST NOT iterate
   by `imported[INTEGER]` unless the form has a filter with it
   (`FILTERS imported(receipts)`): form import writes only to the
   [properties and filters of the form](../paradigm/In_a_structured_view_EXPORT_IMPORT.md#importForm).
   Without the filter, a staging property
   that is always filled plays that role.

   The assistant MUST NOT use one mark property in the filters
   of several object groups: each group numbers its records
   from 0, so the marks get mixed. Every further group
   needs a `LOCAL` mark property of its own.

8. The assistant MUST choose format options explicitly
   when the external contract depends on them:
   - `HEADER` / `NOHEADER`
   - `SHEET`
   - `CHARSET`

   The assistant SHOULD prefer `HEADER`
   for stable `CSV` / `XLS` templates,
   because `NOHEADER` can silently map missing
   or mistyped columns to `NULL`.

9. The assistant MUST validate referenced business keys
   before creating or updating persistent objects.

   Typical keys are `id`, `number`,
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

10. The assistant SHOULD separate raw import
    from domain resolution:
    - first parse the file or payload
      into locals or an import form
    - then check references such as
      item, partner, status, type, or other lookups
    - only then create or update domain objects

11. For user-started batch imports and external integrations,
    the assistant SHOULD isolate persistence in `NEWSESSION`,
    and SHOULD `APPLY;` after the domain writes of one import.

    Which session the import runs in, how the upper-session
    buffer reaches it and what follows `APPLY;` are governed
    by the change-session rules of the domain-logic article
    (`lsfusion_get_guidance(rules='logic')`).

12. The assistant MUST NOT partially persist
    a failed import silently. For failures the assistant
    detects on its own (missing references, malformed
    payload, pre-`APPLY` validation), it SHOULD use
    `MESSAGE`, `RETURN`, `throwException`, or an explicit
    failure flag, consistent with the caller:
    - interactive import -> `MESSAGE`
    - API or background integration
      -> exception or explicit failure state

13. For create-or-update synchronization imports,
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

14. `LOCAL` staging properties used by a single import action
    MUST be declared inside that action; module scope is for
    a `LOCAL` that an import form uses or several related
    actions share.

## Data export (EXPORT)

### Choosing the export source

Data is exported with the [`EXPORT` operator](../language/EXPORT_operator.md).

1. Exporting a list of properties (`EXPORT FROM ...`) SHOULD be used when the result is a single flat table of columns and its structure matches no existing form.

2. Exporting a form (`EXPORT formName ...`) SHOULD be used when the export repeats an already existing form or when the result needs the object group hierarchy. The hierarchy is preserved only in **JSON** and **XML**; in the flat formats each object group produces a separate file, so for them the destinations are listed per group in the `TO` block — only for the groups that are wanted, since a group left out of the list is simply not exported.

3. A form created solely for an export SHOULD be declared next to the export action and SHOULD NOT be added to the navigator.

### Stating explicitly what shapes the result

1. The format SHOULD be stated explicitly even when **JSON** is intended: relying on the default makes the export depend on the reader remembering that default.

2. The `WHERE` condition SHOULD be stated explicitly. Without it the condition is the disjunction of all exported properties, so the export includes every object set with at least one field filled — which almost never matches the intended set of rows.

3. Column identifiers SHOULD be given explicitly (`columnId = expr`). The default `expr1`, ..., `exprN` ties the field names in the external format to the order of the expressions, so inserting a column in the middle of the list silently changes the export contract.

4. `ORDER` SHOULD be stated explicitly whenever the receiving side depends on the row order. Its expressions are arbitrary and need not be among the exported ones — a sorting expression is added to the internal query as a hidden column and does not reach the result — so a column SHOULD be added to the export only when the recipient needs it, not merely in order to sort by it.

5. In the hierarchical formats a property with a `NULL` value is omitted from the record (in **JSON** the key is absent, in **XML** the element), while the flat formats (**CSV**, **XLS**, **XLSX**, **DBF**) keep the column and write an empty cell. So in **JSON** a missing key means `NULL`, not a failed export (for form properties with `SHOWIF` inclusion follows the `SHOWIF` value instead: a non-`NULL` value can be omitted and a `NULL` one emitted).

6. When several scalar values are returned in one export (probe results, diagnostics), separate columns `EXPORT FROM a = ..., b = ...` SHOULD be preferred to one concatenated string: a `NULL` drops only its own key, while in a `+` concatenation it nulls the whole result. To force the key's presence, wrap the value in `OVERRIDE ..., <default>` (when exporting a form, the property option `EXTNULL` MAY be used instead). An export of parameterless expressions keeps its single record even when every value is `NULL`; for rows generated by export parameters the default `WHERE` (disjunction) drops an all-`NULL` record — the `WHERE` SHOULD then be stated explicitly or a constant column added.

### Format options

1. Options whose default differs between formats SHOULD be set explicitly: the presence of a header row (`HEADER` / `NOHEADER`) in **CSV**, **XLS**, **XLSX**, the **CSV** separator (`;` by default) and the encoding (`CHARSET`, `UTF-8` by default and `CP1251` for **DBF**).

2. `NOESCAPE` in **CSV** MAY be used only when the separator is guaranteed not to occur in the data; otherwise `ESCAPE` SHOULD be kept.

3. The encoding SHOULD be determined by the receiving side's requirements rather than by the default: recipients of **DBF** files usually expect a single-byte encoding other than `UTF-8`.

### Result destination

1. The destination property in `TO` SHOULD be declared local to the export action and of a file class (`FILE`, `RAWFILE`, `JSONFILE`) rather than being a shared property: one property shared by several exports makes the result depend on the execution order.

2. The `System.exportFile` default SHOULD be used only for debugging and one-off exports.

3. When a form is exported to a flat format, destinations SHOULD be listed for every exported object group; the group of objects without a name is called `root`.

4. To return a value from an action called by an external system, the assistant MUST use `RETURN`, not `EXPORT`: `RETURN` delivers the value from any place in the action, including after `APPLY` and from inside a `NEWSESSION` block, whereas the result of `EXPORT` stays in the session in which it ran and the response comes back empty.

### Delivering the result

1. The action SHOULD be split into preparing the data, the `EXPORT` itself, and delivering the file to the recipient — writing it to the file system, sending it to an external system, or storing it in a property. This split allows the same export to be reused with different delivery methods.

2. For regular exports, building the file SHOULD be done in a separate action with no user interaction, so that it can be called both from a form and on a schedule.

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
