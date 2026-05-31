---
slug: "/System_System"
title: 'System'
---

`System` is the root [system module](System_modules.md) of the platform. It is pulled in automatically — every other module depends on it without an explicit `REQUIRE`. The module declares the root [classes](User_classes.md), the standard [groups](Groups_of_properties_and_actions.md) of properties and actions, the change-session control surface, the incoming HTTP-request context, the form-lifecycle actions, the application-identity properties, and the local buffers that the export, import, and value-request operators write to.

### Root classes

| Class                 | Purpose                                                                                  |
|-----------------------|------------------------------------------------------------------------------------------|
| `Object`              | the base class of all user objects; parent of all user-defined classes                   |
| `StaticObject`        | the base class for static objects (values declared in the module's source)               |
| `CustomObjectClass`   | meta-class object: every user class is represented here by one static object that carries statistics and skeleton properties |

### Standard groups

`root` / `public` / `private` are the roots of the group hierarchy. `base` (inside `public`), `id` (inside `base`), `uid` (inside `id`) are the standard subgroups for key properties. `drillDown` and `propertyPolicy` are separate roots for drill-down properties and access-policy properties. `objects` is a group typically used for properties that collect JSON structures in interactive forms.

### Object metadata

`class[Object]` — the class of an object (as a `CustomObjectClass`). `className[Object]` — the class name as `STRING`. `prevClassName[Object]` — same, for the class value at the start of the session. For static objects: `name[StaticObject]` (name), `caption[StaticObject]` (display caption), `order[StaticObject]` (sort order), `image[StaticObject]` (picture). For a class as a whole: `stat[CustomObjectClass]` — estimated object count.

### Change-session control

| Property / action                                             | What it does                                                                                |
|---------------------------------------------------------------|----------------------------------------------------------------------------------------------|
| `apply[]` / `cancel[]`                                        | wrappers over the `APPLY` and `CANCEL` operators                                            |
| `canceled[]`                                                  | flag set when the last `APPLY` was cancelled by a constraint                                |
| `applyMessage[]`                                              | error message of the last `APPLY` (filled in by the platform itself)                        |
| `ApplyFilter`                                                 | a class with filter shortcuts: `onlyCalc`, `onlyCheck`, `onlyData`, `session`, `withoutRecalc` |
| `applyFilter[]` / `applyOnlyCalc[]` / `applyOnlyCheck[]` / `applyOnlyData[]` / `applySession[]` / `applyOnlyWithoutRecalc[]` / `applyAll[]` | set / clear the filter for the next `APPLY` |
| `check[]`                                                     | run `APPLY` under the `onlyCheck` filter with guaranteed filter restoration                 |
| `sessionOwners[]` / `manageSession[]`                         | the count of session owners and the flag for being a standalone session                     |
| `throwException[TEXT]`                                        | raises an exception with the given message                                                  |
| `setNoCancelInTransaction[]` / `dropNoCancelInTransaction[]`  | forbid / re-allow rollback inside a transaction                                             |
| `setNoEventsInTransaction[]` / `dropNoEventsInTransaction[]`  | forbid / re-allow events inside a transaction                                               |
| `executeLocalEvents[TEXT]` / `executeLocalEvents[]`           | force-run local events (optionally by filter)                                               |
| `beforeCanceled[]` / `requestCanceled[]` / `requestPushed[]`  | request-lifecycle flags                                                                     |
| `logMessage[]`                                                | message emitted into the system log on certain events                                       |
| `empty[]` / `empty[Object]`                                   | no-op actions                                                                               |

### HTTP request context

The platform fills the following `NESTED LOCAL` properties on an incoming HTTP request and reads them on the outgoing response.

**Incoming**: `headers[TEXT]`, `cookies[TEXT]`, `params[TEXT, INTEGER]` (and the shortcut `params[TEXT]`), `fileParams[TEXT, INTEGER]` / `fileParams[TEXT]`, `actionPathInfo[]`, `contentType[]`, `body[]`, `appHost[]`, `appPort[]`, `exportName[]`, `method[]`, `scheme[]`, `webHost[]`, `webPort[]`, `contextPath[]`, `servletPath[]`, `pathInfo[]`, `query[]`, `insecureSSL[]`, `timeoutHttp[]`.

**Outgoing**: `headersTo[TEXT]`, `cookiesTo[TEXT]`, `statusHttp[]`, `failedHttp[]` (computed as `statusHttp() < 200 OR statusHttp() >= 300`), `statusHttpTo[]`.

**Derived URLs**: `origin[]` (`scheme://webHost:webPort`), `webPath[]` (`origin + contextPath`), `url[]` (`webPath + servletPath + pathInfo`), `apiOriginUrl[STRING]` / `apiOriginUrl[LINK]` (API URL relative to `origin`), `apiContextUrl[STRING]` (API URL relative to `webPath`).

**TCP**: `responseTcp[]`, `timeoutTcp[]`.

### Caught-exception state

Inside a `CATCH` block, `messageCaughtException[]`, `javaStackTraceCaughtException[]`, `lsfStackTraceCaughtException[]` give the message, the Java stack trace, and the lsFusion stack trace of the most recently caught exception.

### Opening files and links

`open[STRING]` / `open[FILE]` / `open[NAMEDFILE]` / `open[RAWFILE]` / `open[LINK]` / `open[RAWLINK]` (with shorter overloads for the file name and the `noWait` flag) open the given value on the client in the associated application or browser.

`htmlLinkInTab[HTMLLINK]` opens an HTML link in a separate tab; the `htmlLinkInTab` form hosts the HTML viewer itself.

### Form lifecycle

| Property / action                                 | What it does                                                  |
|---------------------------------------------------|----------------------------------------------------------------|
| `formApply[]`                                     | apply (save) the form's changes (Alt+Enter)                    |
| `formCancel[]`                                    | cancel the form's changes (Shift+Esc)                          |
| `formRefresh[]`                                   | re-read the form's data (F5)                                   |
| `formOk[]`                                        | confirm and close a dialog (Ctrl+Enter)                        |
| `formClose[]`                                     | close the form (Esc)                                           |
| `formDrop[]`                                      | drop the current form object (Alt+Delete)                      |
| `formEditReport[]`                                | edit the form's report template (Ctrl+E)                       |
| `formShare[]` / `formCustomize[]`                 | share the current state / open the form customization          |
| `formCustomizeBackground[]` / `formCustomizeShowIf[]` | abstract properties (`ABSTRACT COLOR()` / `ABSTRACT BOOLEAN()`) used to customize the form-customization dialog |
| `formApplied[]`                                   | abstract list of actions called after a successful form `APPLY` (the default shows a success notification) |
| `navigatorRefresh[]`                              | refresh the navigator                                          |
| `forceUpdate[STRING]`                             | forcibly re-read a group of objects                            |
| `seek[Object]`                                    | locate and activate an object on the current form              |
| `sleep[LONG]`                                     | pause for the given number of milliseconds                     |

### Form context flags

`isActiveForm[]`, `isDocked[]`, `isEditing[]`, `isAdd[]`, `isManageSession[]`, `isExternal[]`, `showOk[]`, `showDrop[]`, `isDataChanged[]` reflect the state of the current form and its interaction with the session. `isEditable[]` / `isReadonly[]` track whether the form is editable.

### Polymorphic edit and delete

`edit[Object]` and `delete[Object]` are declared as abstract actions with default implementations (`SHOW EDIT` and `DELETE`) and are available for extension by specific user classes. `formEdit[Object]` is a direct pass-through to `edit[Object]`. `formEditObject[Object]` wraps `formEdit[Object]` in `NEWSESSION` only when the object already exists in the database (`PREV(o IS Object)`), and calls `formEdit[Object]` directly otherwise. `formDelete[Object]` calls `delete[Object]` directly when `sessionOwners[]` is set; otherwise it shows a confirmation prompt and on confirmation runs `delete[Object]` followed by `APPLY`.

### Local buffers

The platform provides three parallel sets of local properties for different exchange stages:

- **`export…`** — where `EXPORT` writes its intermediate result. One property per built-in class: `exportObject[]`, `exportInteger[]`, `exportLong[]`, `exportDouble[]`, `exportNumeric[]`, `exportString[]`, `exportText[]`, `exportRichText[]`, `exportHTMLText[]`, `exportDate[]`, `exportTime[]`, `exportDateTime[]`, `exportZDateTime[]`, `exportYear[]`, `exportBoolean[]`, `exportTBoolean[]`, `exportInterval<Type>[]` for every interval class, `exportColor[]`, `exportJSON[]` / `exportJSONText[]`, `exportXML[]`, `exportHTML[]`, and the matching `export<Class>File[]` / `export<Class>Link[]` for every file and link class.
- **`requested…`** — the same buffers declared as `NESTED` for batched value prompts (`INPUT` / `REQUEST`).
- **Import**: `importFile[]`, `imported[INTEGER]` and `importedString[STRING[10]]` — the flag that the corresponding "row" arrived from a flat file. `inputList[INTEGER]` / `displayInputList[INTEGER]` — the buffer of list values and captions. `readFile[]`, `readDialogPath[]`, `showResult[]` — the results of file reading, path selection, and message display.

### Application identity

`logicsName[]` (from `dataLogicsName`), `logicsCaption[]`, `topModule[]`, `displayName[]` (computed as `dataDisplayName ?? logicsCaption ?? topModule`) — the application's name, caption, and display name. `hashModules[]` — the hash of the module set the server sees as initialized.

Graphics: `logicsLogo[]` (logo image), `logicsIcon[]` (application icon), `PWAIcon[]` (PWA icon, expected 512×512). Each comes with a triple `load<Name>[]` / `open<Name>[]` / `reset<Name>[]`.

### UI defaults

| Property                                        | Purpose                                                                 |
|-------------------------------------------------|--------------------------------------------------------------------------|
| `defaultBackgroundColor[]` / `defaultOverrideBackgroundColor[]` | base background color; the OVERRIDE variant returns yellow (`RGB(255,255,0)`) when empty |
| `defaultForegroundColor[]` / `defaultOverrideForegroundColor[]` | base text color; the OVERRIDE variant is red                            |
| `selectedRowBackgroundColor[]` / `selectedCellBackgroundColor[]` / `focusedCellBackgroundColor[]` / `focusedCellBorderColor[]` / `tableGridColor[]` | user-defined colors for rows, cells, and grid |
| `customReportCharWidth[]` / `reportCharWidth[]` | character width in reports (default `8`)                                |
| `customReportRowHeight[]` / `reportRowHeight[]` | row height in reports (default `18`)                                    |
| `reportNotToStretch[]` / `reportToStretch[]`    | report content stretching control                                       |

### Runtime and checks

`checkIsServer[]` / `isServer[]` — checks and the flag that the current execution runs on the server. `random[]` — `DOUBLE` between `0` and `1`, `randomUUID[]` — `md5` of a pair of random values, `randInt[INTEGER]` — an integer in `[1; max]`. `notEmpty[STRING]` — converts an empty string to `NULL`. `upper[STRING]` / `lower[STRING]` — case.

`requestCanceled[]` — a native property, flag of the current request being cancelled. `noSystemToolbarCaptions[]` — abstract flag for hiding system-toolbar captions. `isHTMLSupported[]` — a native property indicating whether the client supports HTML.

### File helpers

`file[RAWFILE, STRING]` — assemble a `FILE` from raw bytes and an extension. `file[FILE, STRING]` — clone a `FILE` with a different extension. `namedFile[…]` — same for `NAMEDFILE` (with a name). `name[NAMEDFILE]`, `extension[FILE]`, `extension[NAMEDFILE]` — file components. `md5[FILE]` — MD5 hash. `resourceImage[STRING]` — a resource path as a file image for the UI.

### Client-side loading

`loadLibrary[STRING]` / `loadFont[STRING]` — load a JS library or font on the client. `reload[]` — reload the client.

### Windows and navigator

The module fixes the main [windows](Form_views.md): `logo`, `root`, `system` (the top horizontal bar), `toolbar` (the left vertical strip), `forms` (the central area), `log` (the right notification strip). Each window has an abstract CSS class (`logoWindowClass[]`, …) for customization.

The navigator gets a system folder `Administration` with subfolders `Application` (options / integration / migration) and `System` (performance, notifications, scheduler, logs).

### Language

- [Module header](../language/Module_header.md) — the `MODULE` / `REQUIRE` syntax; the `System` module is pulled in automatically.
- [`APPLY` operator](../language/APPLY_operator.md) — underlies the session-control actions.
- [`CANCEL` operator](../language/CANCEL_operator.md) — cancels session changes.
- [`EXPORT` operator](../language/EXPORT_operator.md) — writes into the `export…` local buffers.
- [`IMPORT` operator](../language/IMPORT_operator.md) — fills `imported`, `importedString`, and related local properties.

### See also

- [`System modules`](System_modules.md) — the general inventory of platform modules.
- [`Time`](System_Time.md) — the separate Time module pulled in via `REQUIRE Time`.
- [`Utils`](System_Utils.md) — the helper-property collection.
- [`Change sessions`](Change_sessions.md) — the session concept and its commit.
- [`Forms`](Forms.md) — what a form is and how its lifecycle relates to the system actions.
