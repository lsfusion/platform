---
title: 'Access from an external system'
---

## Action API

The platform allows external systems to access an lsFusion-based system using various network protocols. The interface of such interaction is a call for an action with specified parameters and, if necessary, the return of certain property values (without parameters) as *results*. It is assumed that all parameter and result objects are objects of [built-in classes](Built-in_classes.md).

### Defining an action {#actiontype}

An action being called can be defined in one of the three ways:

-   `EXEC` – the name of the action is specified.
-   `EVAL` – code in the lsFusion language is specified. It is assumed that this code contains a declaration of an action named `run`. This is the action that will be called.
-   `EVAL ACTION` – action code in the lsFusion language is specified. To access a parameter, the special character `$` and the parameter number (starting from `1`) are used.

### Protocols

The platform currently supports the following network protocols:

#### HTTP {#http}

Communication over this protocol is supported both by the application server (port `7651`) and by the web server, if one is set up (see [Servers](#servers) for details).

The URL format, depending on the method of [action definition](#actiontype), looks as follows:

-   `EXEC` - `http://server address:port/exec?action=<action name>` or `http://server address:port/exec/<action name>`. Either the `action` parameter or the action name in the URL path must be specified.
-   `EVAL` - `http://server address:port/eval?script=<code>`. If the `script` parameter is not specified, it is assumed that the code is passed in the first BODY parameter.
-   `EVAL ACTION` – `http://server address:port/eval/action?script=<action code>`. If the `script` parameter is not specified, it is assumed that the code is passed in the first BODY parameter.

For `EXEC`, the action name can be either the action's [compound name](IDs.md#cid) or its [`EXTID`](Action_options.md) (the compound name is tried first). In the path form, the platform looks up the action greedily: it first tries the full path as an action name (with any `/` replaced by `_` for the compound name lookup), and if no such action exists, drops the last path segment and retries, until a match is found or `404` is returned. The part of the path left after the matched action name is passed to the `System.actionPathInfo[]` property. For example, for `/exec/df/fdf/dffd` the platform tries actions `df_fdf_dffd`, then `df_fdf`, then `df`; if only `df` matches, `fdf/dffd` is written to `System.actionPathInfo[]`.

##### Servers {#servers}

The HTTP API is served by two kinds of servers:

-   **Application server** - a lightweight HTTP server built into the application server, listening on port `7651` by default. It serves the Action API at `/exec` / `/eval` / `/eval/action`, with `Authorization: Basic` / `Authorization: Bearer` authentication and stateful sessions via the `session` parameter. [Interactive actions](#interactive) here are accepted only through the `Need-Notification-Id` header.
-   **Web server** - a separate web application (deployed in a servlet container such as Tomcat), listening on its own port. It serves the lsFusion web client (the browser-based user interface) and, on the same port, the HTTP API: the same Action API as the application server, plus the [Form API](#form) at `/form` and the browser-redirect variant for [interactive actions](#interactive).

A request is addressed as `http://<server address>:<port><endpoint>` for the corresponding server.

##### Parameters {#url}

Parameters can be passed both in the request string (by appending constructs like `&p=<parameter value>` to the end of the string), as well as in the request body (BODY). It is assumed that URL parameters are substituted (in the order of their appearance in the request) for the executed action before BODY parameters.

If the called action has named interface parameters (for example, `run(INTEGER no, DATE date)`), a request parameter whose name matches an action parameter name is bound to it by name. The positional substitution above then fills any remaining slots only from unused `p=` parameters (URL or `application/x-www-form-urlencoded` body) and non-urlencoded BODY parameters (multipart parts or a single body); other named URL/urlencoded parameters that did not match an interface name are ignored.

When processing BODY parameters, parameters with the content type from the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties) are considered files and are passed to the action parameters as objects of the file class (`FILE`, `PDFFILE`, etc.). During this process, the corresponding file extension is taken from the table mentioned above. If a particular content type is not found in the table, but it starts with `application`, the parameter is still considered a file, and the file extension is taken from the right part of the content type (for example, it will be `abc` for the `application/abc` content type). Parameters with the `application/null` content type are considered to be equal to `NULL`.

When such a BODY parameter is bound to an action interface parameter whose class is not a file class, its content is parsed as a string and automatically converted to that parameter's class; empty strings become `NULL`. This conversion applies only to the action-parameter binding described above - in the [request properties](#request) below, the same parameter is always stored as a file value.

##### Request properties {#request}

[Headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) of an executed request are automatically saved to the `System.headers[TEXT]` property. The name of the header is written to the only parameter of this property, and the value of the header is written to the property's value.

[Cookies](https://en.wikipedia.org/wiki/HTTP_cookie) of an executed request are similarly saved to the `System.cookies[TEXT]` property. The name of the cookie is written to the only parameter of this property, and the value of the cookie is written to the property's value.

Individual request parameters can also be accessed by name. For URL parameters the name is taken from the query string (`&<name>=<value>`); for an `application/x-www-form-urlencoded` body, from the key of each key-value pair; for parts of a `multipart/form-data` body, from the `name` field of each part's `Content-Disposition` header. For any other body a single name is taken from the `name` field of the request's own `Content-Disposition` header (defaulting to `body` if the header is absent); parts of a non-`form-data` `multipart/*` body inherit this name - their own `Content-Disposition` is not read - unless a part is itself `application/x-www-form-urlencoded`, in which case its keys become parameter names as described above.

The split between `System.params[TEXT, INTEGER]` and `System.fileParams[TEXT, INTEGER]` is decided by content type at ingest, applied recursively to parts of a `multipart/*` body. URL parameters and any `application/x-www-form-urlencoded` body or part are read as `TEXT` from `System.params[TEXT, INTEGER]`. Any other body or part (including plain text fields of `multipart/form-data`, which have no content type of their own) is read as `NAMEDFILE` (preserving the original file name if any) from `System.fileParams[TEXT, INTEGER]`. A part with neither a content type nor any body bytes is skipped entirely. The first parameter of both properties is the parameter name, the second is the 0-based index among parameters sharing that name (multiple `p=` parameters or same-named body parts become accessible under one name with indexes `0`, `1`, and so on); the short forms `System.params[TEXT]` and `System.fileParams[TEXT]` give the first occurrence.

The platform also fills the following properties with information about the current request:

-   `System.method[]` - HTTP method of the request.
-   `System.contentType[]` - content type of the request body.
-   `System.body[]` - raw body of the request (on the web server, an `application/x-www-form-urlencoded` body may be reconstructed from the parsed parameter map if the servlet has already consumed the stream, so the exact bytes / order / encoding are not guaranteed to match the original).
-   `System.query[]` - raw query string of the request.
-   `System.scheme[]`, `System.webHost[]`, `System.webPort[]`, `System.contextPath[]`, `System.servletPath[]`, `System.pathInfo[]` - parts of the request URL. The convenience properties `System.origin[]` (scheme, host and port), `System.webPath[]` (origin plus context path), and `System.url[]` (scheme through path, without the query string) return the corresponding composed URLs.

##### Results {#httpresult}

Properties whose values must be returned as the result are passed in the request string by adding strings like `&return=<property name>` to its end. It is assumed that the values of specified properties are returned in the order of their appearance in the request string. By default, if no result properties are specified, the resulting property is the first one with a non-`NULL` value from the following [list](Built-in_classes.md#export). 

If the result of a request is a file (`FILE`, `PDFFILE`, etc.), the response [content type](https://en.wikipedia.org/wiki/Media_type) , depending on the file extension, is determined in accordance with the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties). If the file extension is not found in this table, the content type is set to `application/<file extension>`.

The file extension in this case is determined automatically, similarly to the [`WRITE` operator](WRITE_operator.md).

In all of the three cases above, if the result value is `NULL`, a `null` string (for example, `application/null`) is substituted for the file extension in the content type, and an empty string is returned as the response itself.

Request results different from files are converted into strings and are passed as a `text/plain` content type. `NULL` values are returned as empty strings.

The values of the `System.headersTo[TEXT]` property are automatically written to the [headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) of the request result. So, the header name is read from the only parameter of this property, and the header value is read from the property value.

The values of the `System.cookiesTo[TEXT]` property are similarly written to the response as [cookies](https://en.wikipedia.org/wiki/HTTP_cookie) (one `Set-Cookie` header per cookie). The cookie name is read from the only parameter of this property, and the cookie value is read from the property value.

The HTTP [status code](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes) of the response is read from the `System.statusHttpTo[]` property (`200` by default).

##### Several results / parameters in BODY

If the type of request BODY is `multipart/*` or `application/x-www-form-urlencoded`, it will be split into parts, and each part will be considered a separate request parameter. In this case, the order of these parameters is equal to the order in corresponding parts of the request BODY.

At the same time, if the number of results being returned is more than one, then the following happens:

-   If the request has a `returnmultitype=bodyurl` parameter, the response content type on transmission is set to `application/x-www-form-urlencoded` and the results are encoded as if they were [passed in the request string](#url).
-   Otherwise, the response content type during transmission is set to `multipart/mixed`, and the results are passed as internal parts of this response. 


:::info
Note that the processing of parameters and request results is largely similar to their processing during [access to an external system](Access_to_an_external_system_EXTERNAL.md) over the HTTP protocol (in this case, parameters are processed as results and vice versa, results are processed as parameters).
:::

##### Stateful API

The API described above is a REST API. Accordingly, the [change session](Change_sessions.md) is created when a call is initiated, and closes immediately after the call ends. However, there are situations where such behavior is undesirable, and you need to accumulate changes for a certain period of time (for example, while the user is inputting data), which means that the session must be saved and handed over between sessions. In order to do this, you can add a string of the following format to the end of the query string: `&session=<session ID>`, where `<session ID>` is any non-empty string. In this case, the session will not be closed after the call, but will be associated with a previously passed ID, so that all subsequent calls with this ID will be executed in this session. In order to close a session (after the end of a call), you need to add the `_close` postfix (for example,`&session=0_close`) to its ID in the request string.


:::info
On the web server the container that keys sessions by the `session` parameter lives inside the HTTP session, so stateful calls must stay within the same HTTP session - usually maintained through the session cookie that browsers and HTTP client libraries send automatically. On the application server the container is global to the server process, and no cookie is required.
:::


:::info
The current implementation of the platform assumes that if sessions are used, the elements of the system (for example, local properties) created in the current call are deleted — that is, they are not visible in subsequent calls.
:::

##### Authentication

When executing an http request, it is often necessary to identify the user on whose behalf the specified action will be executed. At the moment, two types of authentication are supported by the platform:

-   [Basic identification](https://en.wikipedia.org/wiki/Basic_access_authentication) - the user name and password are passed in an encoded form in the `Authorization: Basic <credentials>` heading.
-   Token-based authentication consists of two stages:
    -   At the first stage, you need to execute the `Authentication.getAuthToken[]` action with basic authentication. The result of this action will be an authentication token with a fixed lifetime (one day [by default](Working_parameters.md#authTokenExpiration)). An example of a request:  `http://localhost/exec?action=getAuthToken`.
    -   The token you receive can be used for authentication during its lifetime by passing it in the `Authorization: Bearer <token>` header (similarly to JWT which is used in the current implementation of the platform for generating authentication tokens).

Whether a request is accepted also depends on the [`enableAPI`](Working_parameters.md) setting, which allows disabling the API entirely, restricting it to authenticated requests, or additionally allowing anonymous requests. An individual action can be annotated with [`@@noauth`](Action_options.md) to bypass both the authentication check and `enableAPI`, or with [`@@api`](Action_options.md) to allow it at `enableAPI=0` (still requiring an authenticated user).

##### Interactive actions {#interactive}

An external call may need to trigger an action that interacts with the user - opening a [form](Forms.md), showing a message, asking for input, and so on. Since the HTTP response itself has no UI, the platform routes such an action to a running lsFusion client of the user: the server creates a *notification*, and the client picks it up and executes the action locally.

When an action is considered interactive:

-   [`@@ui`](Action_options.md) - always interactive.
-   [`@@noui`](Action_options.md) - never detected as interactive, so the action runs synchronously (unless the client explicitly requests a notification ID via the `Need-Notification-Id` header, which is always honoured).
-   Neither annotation - interactive automatically when the request comes from a browser navigation (signalled by the `sec-fetch-mode: navigate` header) and the action body uses any interactive feature; otherwise synchronous.

How the caller obtains the notification:

-   **Browser redirect (default)** - HTTP `302` to `/push-notification?notification_id=<notification id>`. Following the redirect in a browser delivers the notification to the lsFusion tab already open there, through its service worker.
-   **Notification ID (when the request has the `Need-Notification-Id` header)** - HTTP `200` with the notification ID (as `INTEGER`) in the response body. Intended for non-browser callers that need to deliver the ID to a running lsFusion client through some other channel.

Interactive actions additionally require the [`enableUI`](Working_parameters.md) setting to permit the call, on top of the regular [`enableAPI`](Working_parameters.md) check.

##### Errors

On failure, the response uses a specific HTTP status code:

-   `404` - the action specified via `?action=` or in the URL path was not found.
-   `401` - authentication is required or has failed. On the web server, an anonymous [interactive](#interactive) request is redirected to `/login` instead.
-   `500` - any other unhandled exception raised during request processing, including when the API is disabled by the [`enableAPI`](Working_parameters.md) setting.

For `404`, `500`, and other server-side exception statuses, the response body is `text/html` with the error message; for most exceptions it also includes Java and lsFusion stack traces, except for a `RemoteMessageException` (a user-facing platform message) which returns only the message. A `401` body carries only the short error message. On the web server, the redirect to `/login` has no body - the exception is stored in the HTTP session and the original request is cached for retry after login.

## Form API {#form}

Apart from executing actions, the platform also supports an API (similar to JSON API) for working with [forms](Forms.md), or specifically, their [interactive views](Interactive_view.md). Since it's a stateful API designed for the asynchronous mode (which means that the HTTPS interface itself has a number of system parameters, such as a request index, index of the latest received response, etc.), it's easier to use this API with the help of special libraries for specific languages/platforms that you want to integrate with:

### Protocols

#### JavaScript

The JavaScript library is available in the central npm-repository under the name  [@lsfusion/core](https://www.npmjs.com/~lsfusion).

The key concept in this API is the concept of *state*. A state is a JS object with a structure corresponding to form elements in the following way:

-   [An object group](Form_structure.md#objects) corresponds to a JS object that is stored in the js field of the state object. The name of the field matches the name of the object group. Each JS object from the object group, in turn, stores an array of JS objects (with [filters](Form_structure.md#filters) and [orders](Form_structure.md#sort) taken into account) in the `list` field. The JS object of the object group corresponds to the [current](Form_structure.md#currentObject) object collection. Also, each JS object of an array (including the JS object of the object group) in the `value` field stores the value of objects – only values if there is just one object in the object group or, if there are multiple objects, a JS object with fields whose names are equal to object names and values are equal to object values.
-   [Properties](Properties.md) correspond to a value stored in a field (the name of the field is equal to the property name) of a JS object which is determined in the following way depending on the existence of parameters and [its view](Interactive_view.md#property):
    -   A property has parameters:
        -   The property view is equal to `GRID` of each JS object in the `list` array of the JS object of this property's [display group](Form_structure.md#drawgroup).
        -   The property's view is equal to `PANEL`, `TOOLBAR`  of the JS object of this property's display group
    -   A property has no parameters - of a JS state object.

The task of the library is to automatically keep this state described above up to date, both during form creation and during its subsequent modification (this behavior is often called reactivity).

The library exports the following functions:

-   `create` - creates a new form. Parameters:
    -  `setState` - a state change request function. This function is supposed to take a single parameter – a state change function (which, in turn, has just one parameter, the previous state, and outputs the next state as the result) and as a result of execution add this function to the state change queue (or, for example, apply it right away depending on the implementation of the view logic). This state management logic is fully identical to the state management logic in React and, as a rule, if used inside a React component, the `setState` parameter is passed as `updateState => this.setState(updateState)`.
    -  `baseUrl` - the URL of the lsFusion web server - a string, for example `'https://demo.lsfusion.org/hockeystats'`.
    -  `formData` - an object describing the form. Must contain either a name field with the name of the form (for example `{name: "MainForm"}`) or a script field with the form code (for example, `script:"OBJECTS i = Invoice PROPERTIES (i) date, stock"`)
-   `change` - changes the form data. Parameters:
    -  `setState` - a state change request function.
    -  `changes` - a JS object containing what exactly needs to be changed. The structure of a JS change object is the same as that of the JS state object, except that a JS object of an object group does not have/is not supposed to have a `list` field – that is, all changes are supposed to be made for current object collection. However, if necessary, the `value` can be set to a single-element array, which will mean that there is no need to change the current object, but property values should be changed for the specified, not the current object. For example, `change(setState, {game:{value:[30], hostGoals:40, guestGoals:30}})` will change the number of goals to `40` and `30`, but not for the current game, but for one with an object id `30`. You can also specify actions in a JS change object (there are no actions in JS state objects). The value of the corresponding field in this case can be arbitrary. For example, `change(setState, {game: {doSmthWithGame : true}})`
    -  `currentState` - a JS object of the current state. Optional parameter. In order to ensure the best UX in the asynchronous mode, it is advised that the user change values only for the objects that he sees during the change and not when the change is processed (the state can change at this moment and so can current objects). Therefore, when calling this function, it is recommended to pass the state that was used to render the view in which the user initiated this change as the `currentState` parameter.
-   `close` - closes the form. Parameters:
    -  `setState` - a state change request function.
-   `formCreated` - checks whether the form has been initialized (and, accordingly, whether the state has been filled). Returns a boolean value. Parameters:
    -  `state` - a JS state object
-   `numberOfPendingRequests` - show how many change requests are currently queued. Returns a long type value. Parameters:
    -  `state` - a JS state object

As the names of object groups and properties, not names on the form are used, but [export/import](Structured_view.md#extid) names (which, however, match the names on forms if not explicitly defined). While working with a form via Form API, actions created using operators for [object operations](Interactive_view.md#objectoperators) `NEW` and `DELETE` automatically get export/import names `NEW` and `DELETE`, respectively (that is you can call `change(setState, {game : {NEW:true}})` for adding an object, for example). Also, when Form API is used, it automatically adds a property called `logMessage` to the form to which all dialog messages are written (including those generated when [constraints](Constraints.md) were violated).

## Examples

### Action API (Python)
```python
import json
import requests
from requests_toolbelt.multipart import decoder

lsfCode = ("run(INTEGER no, DATE date, FILE detail) {\n"
           "    NEW o = FOrder {\n"
           "        no(o) <- no;\n"
           "        date(o) <- date;\n"
           "        LOCAL detailId = INTEGER (INTEGER);\n"
           "        LOCAL detailQuantity = INTEGER (INTEGER);\n"
           "        IMPORT JSON FROM detail TO detailId, detailQuantity;\n"
           "        FOR imported(INTEGER i) DO {\n"
           "            NEW od = FOrderDetail {\n"
           "                id(od) <- detailId(i);\n"
           "                quantity(od) <- detailQuantity(i);\n"
           "                price(od) <- 5;\n"
           "                order(od) <- o;\n"
           "            }\n"
           "        }\n"
           "        APPLY;\n"
           "        EXPORT JSON FROM price = price(FOrderDetail od), id = id(od) WHERE order(od) == o;\n"
           "        EXPORT FROM orderPrice(o), exportFile();\n"
           "    }\n"
           "}")

order_no = 354
order_date = '10.10.2017'
order_details = [dict(id=1, quantity=10),
                 dict(id=2, quantity=15),
                 dict(id=5, quantity=4),
                 dict(id=10, quantity=18),
                 dict(id=11, quantity=1),
                 dict(id=12, quantity=3)]

order_json = json.dumps(order_details)

url = 'http://localhost:7651/eval'
payload = {'script': lsfCode, 'no': str(order_no), 'date': order_date,
           'detail': ('order.json', order_json, 'text/json')}

response = requests.post(url, files=payload)
multipart_data = decoder.MultipartDecoder.from_response(response)

sum_part, json_part = multipart_data.parts
sum = int(sum_part.text)
data = json.loads(json_part.text)

##############################################################

print(sum)
for item in data:
    print('{0:3}: price {1}'.format(int(item['id']), int(item['price'])))

##############################################################
# 205
#   4: price 5
#  18: price 5
#   3: price 5
#   1: price 5
#  10: price 5
#  15: price 5
```

### Form API (JavaScript)
