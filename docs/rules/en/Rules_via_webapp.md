---
slug: "/Rules_via_webapp"
title: 'Rules: browser WebMCP tools'
---

## Session context

1. The assistant MUST choose the execution context deliberately: without `form` a call runs in a new navigator session, with `form` in that form's own session together with the user's unsaved edits. Before applying in a form's session, it MUST include the user's unsaved edits in the scope confirmed with the user: `APPLY` saves them along with the script's changes.

2. The assistant MUST NOT treat waiting for the user's answer as proof of a failed call and repeat a changing action: a synchronous dialog keeps the call waiting for that answer.

## Client operations

1. To change the filters, sortings, current row or values the user sees, the assistant MUST use `lsfusion_client`, rather than try to reproduce those interface operations with an eval script. It MUST use `lsfusion_eval` or `lsfusion_exec` for application logic.

For example, showing only unposted invoices is a filter of the form, set with `lsfusion_client`; posting an invoice is an action of the application, run with `lsfusion_exec`, or with `lsfusion_eval` as a script.

## Customization

[Saved customization code](../brief/Brief_via_app.md) is compiled when a form is created, the application's when the client starts.

1. The assistant MUST NOT claim that saving customization code changes an already open form or the running client: the change shows only once the form is created, or the client started, again.

2. To recreate the form it is working in, the assistant SHOULD call `SystemEvents.formRecreate[]` through `lsfusion_exec` with `form` addressing that form: recreating that one form is enough. It MUST NOT use this action to recreate another form from the navigator: it recreates the form in whose context it runs.

3. Before calling `System.reload[]`, the assistant MUST ask the user: it reloads the whole page without the leave confirmation, so unsaved edits in every open form, the current one included, are lost.
