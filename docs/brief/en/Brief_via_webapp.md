---
slug: "/Brief_via_webapp"
title: 'Brief: browser WebMCP tools'
---

## Tools in the user's client

This kind extends [`via_app`](Brief_via_app.md): the application's page gives the agent the user's client. `lsfusion_client` changes what the user sees — filters, sortings, the current row, values — and `lsfusion_eval` / `lsfusion_exec` run where the user works, in the navigator or in a form's session, as their descriptions say. `Authentication.currentUser[]` is the client's user, and that user's access rights apply. A dialog shown to the user keeps the call waiting for the answer.

## Recreating a form

[Saved customization](Brief_via_app.md) of a form shows when the form is created again, of the application when the client starts again. `SystemEvents.formRecreate[]`, called through `lsfusion_exec` with `form` addressing a form, closes and recreates the form it runs in. `System.reload[]` reloads the whole page, which starts the client again, without its leave confirmation, so unsaved edits in every open form, the current one included, are lost.
