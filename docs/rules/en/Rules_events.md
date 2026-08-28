---
slug: "/Rules_events"
title: 'Rules: events (WHEN)'
---

## Event rules (`WHEN`)

1. A `WHEN` event fires whenever its condition becomes true
   during a session and writes its target property
   unconditionally. If the same target property is also
   changed explicitly elsewhere in the session
   (user input, action assignment, import),
   the event overwrites that explicit change.

2. When the event's purpose is only to derive or default
   a value from other inputs, the assistant SHOULD guard
   the condition with `AND NOT CHANGED(<target>)`
   for each target property the event writes.

   This prevents the event from clobbering an explicit
   change to the target made elsewhere in the same session.

3. The guard SHOULD be omitted only when the event must
   forcibly override any explicit change — for example,
   maintained totals, audit stamps, or invariants
   the user is not allowed to bypass.

4. Rules 1-3 describe the event-action form
   `WHEN <condition> DO <target> <- <expr>`. The calculated
   event form `<target> <- <expr> WHEN <condition>` behaves
   differently: its change is calculated when the target
   property is accessed, and an explicit change of that
   property in the session takes priority over the event's
   change.

   So to default a value while yielding to an explicit
   change, the calculated event form alone is enough —
   no guard is needed. Testing `CHANGED(<target>)` in its
   condition is not possible in any case: the target would
   then depend on its own change, forming a cycle
   `<target>` -> `CHANGED(<target>)` -> `<target>`.

   In the absence of an explicit change the event writes
   the value of the expression even when it is `NULL`.

5. A `WHEN` condition is checked on deleted objects too.
   Deleting an object resets its data properties to `NULL`,
   so a condition that reacts to a value becoming `NULL` is
   satisfied for every deleted object whose value had been
   non-`NULL`, and the handler runs on the object that is
   already gone.

   Which change operators those are is decided by the
   transition each of them covers: `DROPPED`, `CHANGED`,
   `DROPCHANGED` and `SETDROPPED` include non-`NULL` to
   `NULL` and therefore fire on deletion; `SET` and
   `SETCHANGED` require the new value to be non-`NULL` and
   do not.

   Where the condition can fire on the way to `NULL`, and the
   handler must not act on a deletion or on an object leaving
   the class, it MUST be narrowed with `<object> IS <Class>`.

## Where local events actually run

1. A local event handler does not run at the moment the data
   changes. It runs at a point in the session's life: a form
   synchronising, a form opening, an `APPLY` starting, a
   nested session being created, or an explicit
   `System.executeLocalEvents[]`.

   Outside an interactive form — an action called from an
   external system, a scheduler task — the only one of those
   that normally happens is the apply. So reading a property
   right after changing the data it depends on returns the
   value WITHOUT the local handlers applied, unlike a
   calculated property, which is always current.

   The assistant MUST NOT rely on a local handler having run
   in such a place: either let `APPLY` do it, or call
   `System.executeLocalEvents[]` before the read.
