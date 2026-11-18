---
slug: "/Rules_via_app"
title: 'Rules: running application'
---

## Application context

1. For a running-the-system task, the assistant SHOULD read `lsfusion_get_guidance(brief='operation')` before choosing a mechanism. It MUST read the area rules its code triggers, temporary scripts included.

## Action execution

1. The assistant MUST treat `lsfusion_eval` and `lsfusion_exec` calls as execution on live data and MUST confirm the target, scope and effects of a change with the user before making it. Before treating a call as read-only, it MUST check the called actions for their own `APPLY` and external effects: discarding the call's session does not undo them.

2. In scripts, the assistant MUST follow the area rules on [non-interactive `APPLY`](Rules_logic.md#change-sessions-newsession-apply), [local events](Rules_logic.md#where-local-events-actually-run) and [raw `r'...'` literals in comparisons](Rules_view.md#internationalization).

## Logic changes

1. The assistant MUST distinguish executing actions, saving supported settings and changing source logic. It MUST use source editing and deployment for durable changes to source logic: eval code cannot change the project's persistent structure.

2. Where saved [customization](../paradigm/System_SystemEvents.md) — of a form, or of the application's navigator and windows — covers the request, the assistant SHOULD prefer it and establish whether it is for one or all users. It MUST verify the result on opening the form or starting the client again and check reported customization errors. It MUST NOT treat a successful write alone as proof that the customization compiled and took effect. Before writing a fragment, it MUST read it and keep the code the change does not concern: a write replaces the whole fragment.

## A call over `/mcp`

1. The assistant MUST NOT report `MESSAGE` output in `messages` as shown to the user, or an empty answer as successful execution of an `@@ui` action: the output only returns to the caller, and the action is queued for the interface instead of running.

2. The assistant MUST obtain needed values and confirmation from the user before calling code that requires them. It MUST NOT use `ASK` for consent: it receives YES without asking anyone.

3. Before user-specific work — a personal setting, a personal customization — the assistant MUST establish whose account it changes: over `/mcp`, `Authentication.currentUser[]` is the token's account, possibly a shared integration account.

## Client actions over `/mcp`

[Client actions](../brief/Brief_via_app.md) in a call over `/mcp` have no client to run on; on the page the user's own client runs them directly, and delivery is left for the rare case of another tab or client.

1. To show something to a user or open a form for them from a call over `/mcp`, the assistant MUST deliver the action to that user's connection with [`NEWEXECUTOR ... CLIENT`](../language/NEWEXECUTOR_operator.md) and a nested plain `NEWTHREAD`: in the call itself a `MESSAGE` only returns to the caller, and a form cannot open. Before delivering, it MUST establish which user and which of the user's connections are meant. It MUST NOT assume the delivered action shares an open form's session: it runs in a separate navigator session, which matters when the task depends on an open form's pending changes.

2. The assistant MUST NOT substitute [`NEWTHREAD ... CLIENT`](../language/NEWTHREAD_operator.md) with a local property for that delivery: it only records a notification id. It MUST NOT treat a returning `NOWAIT` delivery as proof that the action has completed.
