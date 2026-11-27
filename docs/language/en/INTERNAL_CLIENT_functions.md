---
slug: "/INTERNAL_CLIENT_functions"
title: 'INTERNAL CLIENT functions'
---

The web client runs a JavaScript function named in an [`INTERNAL CLIENT`](INTERNAL_operator.md#client) action or action statement. [Client JavaScript modules](Client_JavaScript_modules.md#named-exports-and-auto-registration) describes how the function is exposed and found by name.

### Arguments and result {#arguments-and-result}

The web client passes the call parameters first, followed by the controller. An action running in a form supplies the [form controller](Form_and_navigator_controllers.md#the-form-controller). An action running in the navigator supplies the [navigator controller](Form_and_navigator_controllers.md#navigator-controller).

The client counts the function's declared parameters using its `length` property. Counting stops before the first parameter with a default value or a rest parameter (`...args`). If the count includes a parameter after the controller, the client passes a success callback there. If the count includes a second parameter after the controller, the client passes a failure callback there.

If the function declares callback parameters, it must invoke one callback exactly once to complete the call, including on failure. Pass the result to the success callback or the error to the failure callback. The call remains pending until a callback is invoked.

If the count includes no callback parameters, the client converts the return value as it is. A returned `Promise` is never awaited.

```js
function reload(controller) { controller.exec('refreshData'); }

function readClipboard(controller, resolve, reject) {
    navigator.clipboard.readText().then(resolve, reject);
}
```

### Requests through the controller {#controller-requests}

The form processes requests through its controller one at a time. A request made by the JavaScript function runs after the action that called it finishes. The form can answer value lookups ([`getValues`](Form_and_navigator_controllers.md#looking-up-values)) out of turn.

When the action waits for the function (`WAIT` or `TO`), the function must finish before a queued form request it makes can run. If the function waits for that request, neither the function nor the action can finish.

The web client starts the next `CLIENT` call in the same form or navigator after the current asynchronous function invokes one of its callbacks. If the action waits for that next call, the current function must invoke a callback before a queued form request it makes can run.
