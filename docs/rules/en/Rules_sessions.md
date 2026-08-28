---
slug: "/Rules_sessions"
title: 'Rules: change sessions (NEWSESSION, APPLY)'
---

## Change session rules (`NEWSESSION`, `NESTEDSESSION`, `APPLY`)

1. Before introducing `NEWSESSION`, the assistant MUST decide
   which session behavior is required. None of the choices below
   applies during an `APPLY` transaction — inside a global event
   handler or an applied action — where no session is created at
   all: the inner action is deferred and runs in the current
   session, inside the same transaction. The assistant MUST NOT
   expect an independent commit there.
   - isolated independent unit -> `NEWSESSION`
   - isolated unit that must also see selected local properties
     from the upper session -> `NEWSESSION NESTED (...)`
   - isolated unit that must see all local properties
     from the upper session -> `NEWSESSION NESTED LOCAL`
   - child dialog or editor that must work with unsaved upper-session
     objects and return its changes to that upper session
     -> `NESTEDSESSION`

2. For actions added to forms,
   there are two main patterns:

   - readonly form pattern:
     the form is effectively browse-only, so actions added to it
     SHOULD run in a new session by default
   - editable form pattern:
     the form has editable properties, so any action added to it
     that uses `NEWSESSION` MUST either:
     `APPLY;`
     `IF canceled() THEN RETURN;`
     before `NEWSESSION`, or be fully independent
     from unsaved changes in that form

3. Plain `NEWSESSION` is the default
   for isolated work that must not accidentally apply
   the caller's pending form changes.

   Typical patterns in the source:
   - readonly list forms with
     `PROPERTIES(...) NEWSESSION NEW, EDIT, DELETE`
   - status transitions or dependent document creation
     after a preceding `APPLY`
   - external or integration actions that isolate HTTP calls
     and persist their own results
   - small immediate UI updates with
     `NEWSESSION { APPLY { ... } }`

4. If inner logic depends on upper-session local state
   such as selections, marks, or import buffers,
   the assistant MUST carry that state explicitly
   through `NESTED (...)` or `NESTED LOCAL`
   on the operator, or declare the property itself
   `DATA LOCAL NESTED`, which carries it over
   without being listed on the operator.
   Neither route works under `NEWSQL`: on a connection
   of its own it migrates nothing, so the assistant
   MUST NOT combine `NEWSQL` with a dependency on
   upper-session local state.

5. A successful `APPLY` clears the session, and with it
   every `LOCAL` property in it by default: after such an
   `APPLY` returns, a plain `LOCAL` is empty again. An
   `APPLY` that fails or is cancelled leaves the session
   as it was, locals included — which is why the assistant
   MUST NOT read a `LOCAL` after `APPLY` to tell success
   from failure; `canceled()` is what tells them apart.
   Inside a nested session there is no clearing at all:
   the changes are copied to the parent session and the
   nested one is left standing, locals and all.

   Outside a nested session, a `LOCAL` value survives
   a SUCCESSFUL `APPLY` when EITHER:
   - the `LOCAL` is declared as `NESTED` at declaration
     time (`LOCAL NESTED name = Type ();` or
     `name = DATA LOCAL NESTED Type (...);`), OR
   - the `APPLY` explicitly preserves it via
     `APPLY NESTED (name1, ..., nameN)`
     or `APPLY NESTED LOCAL` for all locals.

   The assistant MUST NOT rely on a plain `LOCAL` value
   computed before a SUCCESSFUL `APPLY` to still be readable
   after it. Two cases keep it: a nested session, which clears
   nothing at all, and an apply that failed or was cancelled,
   which leaves the session as it was.
   If a staged value must outlive `APPLY` — for example,
   an import buffer read during post-apply follow-up —
   the assistant MUST either declare it with `NESTED`,
   or list it in `APPLY NESTED (...)` (or use
   `APPLY NESTED LOCAL`) at the call site.

6. When using `NEWSESSION NESTED (...)` or
   `NEWSESSION NESTED LOCAL`, the assistant SHOULD preserve
   the same nested local properties on `APPLY`
   if the result must be copied back to the upper session,
   for example with `APPLY NESTED (...)`
   or `APPLY NESTED LOCAL`.

7. The assistant MUST NOT replace `NESTEDSESSION`
   with plain `NEWSESSION` for child forms or dialogs
   attached to a parent object that may still be unsaved
   in the current form session.

8. Before opening a fresh `NEWSESSION` from an action
   started on an edit form, the assistant SHOULD decide
   whether current form changes must be saved first.

   The common pattern is:
   `APPLY;`
   `IF canceled() THEN RETURN;`
   `NEWSESSION { ... }`

   This pattern is used before status changes,
   document generation, and other isolated follow-up actions.

9. After `APPLY`, the assistant MUST check `canceled()`
   only when later logic depends on whether the save
   succeeded — to early-return, skip a follow-up side
   effect, or roll back staged work.

   `APPLY` in an interactive context shows the constraint
   message to the user on its own. The assistant MUST NOT
   add `IF canceled() THEN MESSAGE applyMessage()` after
   `APPLY` in interactive actions solely to report the
   failure — it duplicates the message the platform
   already shows. Explicit surfacing via `applyMessage()`
   or `throwException(applyMessage())` is required only
   for non-interactive callers (API endpoints, background
   integrations) where no dialog is shown.

   If `APPLY` fails because of a constraint, the changes
   remain unsaved in the current session, and any following
   `APPLY` in the same session will also fail until the
   offending data is fixed or the changes are discarded
   (for example with `CANCEL`).

10. The assistant SHOULD keep `NEWSESSION` blocks small
    and purpose-specific: isolate one unit of work,
    apply it if needed, and exit.

    The assistant MUST NOT introduce `NEWSESSION`
    merely to hide session-visibility bugs.
    If upper-session changes must remain visible,
    nested session semantics are required.

11. The body of `APPLY` may run more than once. The apply
    transaction MAY be retried automatically after an update
    conflict, a deadlock or a timeout — whether it is depends
    on the failure and on the attempt limit — and the applied
    action and the synchronous global handlers are inside what
    a retry repeats.

    So they MUST be safe to repeat. An irreversible external
    side effect — sending mail, calling an HTTP API, printing,
    writing a file — MUST NOT be done there: it belongs after
    the apply has succeeded, where `canceled()` says whether
    it did.
