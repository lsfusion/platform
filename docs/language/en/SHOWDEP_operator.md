---
slug: "/SHOWDEP_operator"
title: 'SHOWDEP, SHOWREC operators'
---

The `SHOWDEP` and `SHOWREC` operators create diagnostic [actions](../paradigm/Actions.md) over a list of properties and actions: `SHOWDEP` outputs their static dependency graph, `SHOWREC` enables runtime tracking of recursive event firing.

### Syntax

```
SHOWDEP [scope] [propertyId1, ..., propertyIdN]
SHOWREC [propertyId1, ..., propertyIdN]
```

### Description

The `SHOWDEP` operator creates an action that builds a textual representation of the dependency graph for the listed properties and actions and writes it to the system property `System.showResult` (of class `TEXT`).

The `SHOWREC` operator creates an action that turns on runtime tracking for the listed actions: any action in the list is marked, all others are unmarked. While an apply transaction is in progress, the platform tracks the row-set on which a marked action would fire and writes an entry to the system log whenever that set changes — making it easy to see which other change re-triggered the same event. Calling `SHOWREC` again replaces the tracked set; calling it with an empty list switches tracking off completely.

### Parameters

- `scope`

    Keyword choice that selects the scope of the dependency graph. If omitted, `GLOBAL` is used.

    - `GLOBAL` — Keyword that, when specified, includes all dependencies, including those reachable through apply-time events. Used by default.
    - `LOCAL` — Keyword that, when specified, includes only session-local dependencies.

- `propertyId1, ..., propertyIdN`

    A comma-separated list of [property or action IDs](IDs.md#propertyid). The list may be empty. For `SHOWDEP`, an empty list yields an empty dependency dump. For `SHOWREC`, only action IDs have any effect — property IDs in the list are ignored — and an empty list turns off all currently tracked actions.

### Examples

```lsf
// dump the global dependency graph of two properties
diagnoseDeps() {
    SHOWDEP customer, orderSum;
}

// dump only the session-local dependencies of one property
diagnoseLocal() {
    SHOWDEP LOCAL customer;
}

// start tracing two event actions
traceOrderEvents() {
    SHOWREC recalculateBalance, syncStock;
}

// stop all tracing
stopTracing() {
    SHOWREC;
}
```
