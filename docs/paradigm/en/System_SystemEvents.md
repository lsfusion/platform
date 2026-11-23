---
slug: "/System_SystemEvents"
title: 'SystemEvents'
---

`SystemEvents` is a [system module](System_modules.md) that declares the platform's launch-event handlers, the platform-version properties, the per-environment appearance settings, and the system journals that record exceptions, server launches, client connections, change sessions, and computer pings. It also collects the push-notification and client-interaction actions and the navigator entries for the logo, the appearance dialog, and the log forms. It is pulled in via `REQUIRE SystemEvents` (`System`, `Reflection`, and `Time` are pulled in automatically).

### Launch events

These abstract action lists are the [launch-event](Launch_events.md) handlers — a developer plugs initialization logic into them, and the platform runs the matching list at the corresponding moment of a server or client start. `onStarted[]` is the application-server start handler. The full set of handlers and the moments they fire are described in [`Launch events`](Launch_events.md).

| Handler                          | When it runs                                                                                  |
|----------------------------------|------------------------------------------------------------------------------------------------|
| `onInit[]`                       | early server initialization, before the main start; runs version synchronization              |
| `onStarted[]`                    | application-server start, before the server accepts client connections                         |
| `onFirstStarted[]`               | the very first application-server start only (when `firstStart[]` holds)                        |
| `onFinallyStarted[]`             | after `onStarted[]` and the [`Reflection`](System_Reflection.md) metadata synchronization stage, at the end of the start sequence                                          |
| `onStopping[]`                   | a graceful application-server stop, before RMI, the scheduler and other services are stopped   |
| `onClientStarted[]`              | a client connecting; dispatches to the desktop or web handler by client type                  |
| `onDesktopClientStarted[]`       | a desktop client connecting                                                                    |
| `onWebClientStarted[]`           | a web client connecting                                                                        |
| `onWebClientInit[STRING]`        | per-resource list keyed by a CSS / JS resource path, run when the web client initializes       |
| `onLoginInit[STRING]`            | per-resource list run on the login page                                                        |

`notFirstStart[]` is the stored flag that the server has started before, and `firstStart[]` is its negation and selects the first start. Each handler has an `…Apply` wrapper (`onInitApply[]`, `onStartedApply[]`, `onFinallyStartedApply[]`, `onStoppingApply[]`, `onClientStartedApply[]`) that runs the list and commits with `APPLY`. `onStartedApply[]` additionally runs `onFirstStarted[]` on the first start and sets `notFirstStart[]`. The web and desktop clients run `onClientStartedApply[]` as they start, and the web client runs `toggleLogsPinMode[]` when the log window's pin button is pressed, both through the navigator's controller. Both actions are marked `@@api`, so a logged-in user runs them whatever `enableAPI` is, over the external API as well.

### Version info

| Property                  | What it holds                                                                 |
|---------------------------|-------------------------------------------------------------------------------|
| `platformVersion[]`       | platform version as `TEXT`                                                     |
| `platformBuildNumber[]`   | platform build number as `INTEGER`: the number of commits in the branch the platform was built from (`NULL` if it was not built from a git repository) |
| `apiVersion[]`            | API version as `INTEGER`                                                       |
| `revisionVersion[]`       | source revision as `INTEGER`                                                   |
| `fullVersion[]`           | the platform version, the API version with the build number, and the revision in one string, for example `7.0-SNAPSHOT (build 399.24289) 80348`; without the build number the API version alone is shown in parentheses: `7.0-SNAPSHOT (399) 80348` |
| `synchronizeVersions[]`   | reads the running platform / build number / API / revision values into the properties above; run from `onInit[]` |

### Appearance

Appearance settings are resolved per `DesignEnv` — a design-environment object representing one client's chosen look. Three enum [classes](User_classes.md) carry the choices:

| Class    | Values                                                                          |
|----------|----------------------------------------------------------------------------------|
| `Theme`  | `excel`, `classic`, `flatly`, `lumen`, `quartz`, `simplex`, `sketchy`, `yeti`     |
| `Size`   | `normal`, `mini`, `tiny`                                                          |
| `Navbar` | `horizontal`, `vertical`                                                          |

Each setting has a per-environment value (`designEnv…`), a server-wide default (`server…`), and a resolved value. `theme[DesignEnv]`, `size[DesignEnv]`, and `navbar[DesignEnv]` resolve by `OVERRIDE`: the per-environment value first, then the server default, then a built-in fallback (`Theme.classic` for the theme, `heuristicSize[]` for the size, `Navbar.horizontal` for the navbar). The current-environment forms `size[]`, `navbar[]` read the resolved value for `currentDesignEnv[]`.

| Property                          | What it controls                                                                |
|-----------------------------------|----------------------------------------------------------------------------------|
| `theme[DesignEnv]` / `serverTheme[]` | the color/widget theme; `nameTheme[]` is its name as `STRING`                 |
| `useBootstrap[DesignEnv]` / `useBootstrap[]` | whether the Bootstrap-based UI is used (true for every theme except `Theme.excel`) |
| `size[DesignEnv]` / `serverSize[]` | the widget size; `isMini[]` / `isTiny[]` flag the compact sizes                 |
| `navbar[DesignEnv]` / `serverNavbar[]` | navbar orientation; `verticalNavbar[]` flags the vertical one                 |
| `navigatorPinMode[DesignEnv]` / `navigatorPinMode[]` | the navigator pin mode; from the per-environment value when `useClientNavigatorPinMode[DesignEnv]` holds, otherwise from the server value `serverNavigatorPinMode[]` |
| `logsPinMode[DesignEnv]` / `logsPinMode[]` | whether the log window is pinned; from the per-environment value when `useClientLogsPinMode[DesignEnv]` holds, otherwise from the server value `serverLogsPinMode[]`; toggled by `toggleLogsPinMode[]` |
| `mobileMode[DesignEnv]` / `mobileMode[]` | forces mobile layout on or off                                              |
| `suppressOnFocusChange[DesignEnv]` | suppresses the on-focus-change apply                                            |
| `contentWordWrap[DesignEnv]` / `contentWordWrap[]` | wraps content text                                                |
| `highlightDuplicateValue[DesignEnv]` / `highlightDuplicateValue[]` | highlights duplicate cell values                  |
| `userFiltersManualApplyMode[DesignEnv]` / `userFiltersManualApplyMode[]` | applies user filters manually rather than as typed      |
| `dontShowCloseButtonOnInactiveTab[DesignEnv]` / `dontShowCloseButtonOnInactiveTab[]` | hides the close button on inactive tabs           |

The color theme (light / dark / auto) is toggled by `toggleColorTheme[]`, which cycles `designEnvColorTheme[DesignEnv]`, declared in [Authentication](System_Authentication.md), light → dark → auto (no theme chosen, which leaves the [system](User_interface.md#appearance) theme) for the current environment, applies, and refreshes the form. The navigator pin mode is cycled by `toggleNavigatorPinMode[]`.

The `design` form edits the current environment's appearance (`captionTheme[DesignEnv]`, `captionSize[DesignEnv]`, `captionNavbar[DesignEnv]`, and the toggles above) and reloads the client on apply when a setting that needs a reload changed. `showDesign[]` opens it as a floating window. In the navigator's `system` window the module adds the `showDesign`, `toggleNavigatorPinMode`, and `toggleColorTheme` entries.

The module implements the class properties of the navigator windows declared in [System](System_System.md) and of its own navigator entries, `logoActionClass[]` for `logoAction` and `navigatorPinModeClass[]` for `toggleNavigatorPinMode`. These abstract properties select the implementation by its value. A project's implementation added by a module requiring `SystemEvents` is checked first and replaces the module's classes whenever it returns a value. The module supplies base classes and adds helper classes according to the appearance settings above, the client type (`isWebDesktop[]` or `isWebMobile[]`), and whether the project supplies React components for the layout (`customNavigator[]`) or the window (`customWindow[STRING[100]]` for its canonical name).

On web-client initialization `onWebClientInit[STRING]` registers the CSS and JS resources for the client: the Bootstrap bundle and size-specific padding / font stylesheets when `useBootstrap[]` holds, or the plain table stylesheets otherwise, followed by the shared widget scripts and styles. Resources placed under `/onStarted/` are picked up automatically.

### Exceptions

The `Exception` class hierarchy records server- and client-side errors. `Exception` is the abstract root, split into `ServerException` and `ClientException`. The client branch refines further:

| Class                      | Place in the hierarchy                                                  |
|----------------------------|-------------------------------------------------------------------------|
| `Exception`                | abstract root of all logged exceptions                                  |
| `ServerException`          | an error on the server                                                  |
| `ClientException`          | an error reported by a client                                           |
| `WebClientException`       | a web-client error (`: ClientException`)                                |
| `RemoteServerException`    | a remote-server error reported to a client (`: ClientException`)        |
| `RemoteClientException`    | abstract base for exceptions raised against a remote client (`: ClientException`) |
| `UnhandledException`       | an unhandled remote-client exception (`: RemoteClientException`)        |
| `HandledException`         | abstract base for handled remote-client exceptions (`: RemoteClientException`) |
| `FatalHandledException`    | a fatal handled exception (`: HandledException`)                        |
| `NonFatalHandledException` | a non-fatal handled exception (`: HandledException`)                    |

| Property                         | What it holds                                                          |
|----------------------------------|------------------------------------------------------------------------|
| `message[Exception]`             | the error message                                                      |
| `date[Exception]` / `fromDate[Exception]` | the moment it occurred as `DATETIME` / its `DATE`             |
| `erTrace[Exception]`             | the Java stack trace                                                   |
| `lsfStackTrace[Exception]`       | the lsFusion stack trace                                               |
| `asyncStackTrace[Exception]`     | the asynchronous stack trace                                          |
| `type[Exception]`                | the exception type name                                               |
| `javaStackTrace[Exception]`      | the message and Java trace joined together                            |
| `client[ClientException]` / `login[ClientException]` | the client computer and the login that raised it    |
| `count[NonFatalHandledException]` | how many times a recurring non-fatal exception was collapsed         |
| `abandoned[NonFatalHandledException]` | whether the recurring exception was abandoned                     |

Exceptions are logged through `@defineLog` and shown on the `exceptions` form, where the foreground and background colors mark the non-fatal and the different client / server kinds.

### Launches

The `Launch` class records each application-server start.

| Property              | What it holds                                                  |
|-----------------------|----------------------------------------------------------------|
| `computer[Launch]`    | the server computer; `hostname[Launch]` is its host name       |
| `time[Launch]`        | the start moment as `DATETIME`; `date[Launch]` is its `DATE`   |
| `revision[Launch]`    | the version of the start, as `fullVersion[]` gives it          |

`currentLaunch[]` holds the launch object of the running server. `onStarted[]` creates a new `Launch` filled from the current computer, time, and version, and sets `currentLaunch[]`. Launches are logged through `@defineLog` and shown on the `launches` form.

### Connections

The `Connection` class records each client connection. `currentConnection[]` is the connection of the running request.

| Property                                                        | What it holds                                                     |
|------------------------------------------------------------------|-------------------------------------------------------------------|
| `computer[Connection]`                                          | the client computer; `hostnameComputer[Connection]` is its host name |
| `remoteAddress[Connection]`                                     | the client's remote IP address                                    |
| `headers[Connection, TEXT]` / `userAgent[Connection]`           | request headers and the `User-Agent` header                       |
| `cookies[Connection, TEXT]` / `sessionId[Connection]`           | cookies and the `JSESSIONID` cookie                               |
| `params[Connection, TEXT, INTEGER]` / `params[Connection, TEXT]` | request parameters, indexed and joined                           |
| `user[Connection]` / `userLogin[Connection]`                    | the connected user and the login                                  |
| `osVersion[Connection]` / `processor[Connection]` / `architecture[Connection]` / `cores[Connection]` | client OS and hardware            |
| `physicalMemory[Connection]` / `totalMemory[Connection]` / `maximumMemory[Connection]` / `freeMemory[Connection]` | client memory characteristics |
| `javaVersion[Connection]` / `is64Java[Connection]`              | the client Java version and 64-bit flag                           |
| `screenWidth[Connection]` / `screenHeight[Connection]` / `screenSize[Connection]` / `scale[Connection]` | screen geometry and scale       |
| `clientType[Connection]` / `nameClientType[Connection]`         | the client type (a `ClientType`) and its caption                  |
| `connectionStatus[Connection]` / `nameConnectionStatus[Connection]` | the connection status (a `ConnectionStatus`) and its caption  |
| `connectTime[Connection]` / `connectDate[Connection]` / `disconnectTime[Connection]` | the connect and disconnect moments               |
| `lastActivity[Connection]` / `lastActivity[CustomUser]`         | the last-activity moment of a connection / of a user              |

`ClientType` distinguishes four client kinds: `nativeDesktop`, `nativeMobile`, `webDesktop`, `webMobile`. The per-kind predicates `isNativeDesktop[Connection]`, `isNativeMobile[Connection]`, `isWebDesktop[Connection]`, `isWebMobile[Connection]` test the type, and the derived `isDesktop[Connection]`, `isMobile[Connection]`, `isNative[Connection]`, `isWeb[Connection]` combine them (desktop = native- or web-desktop, native = native-desktop or -mobile, and so on). Every predicate also has a current-connection form (`isNativeDesktop[]`, `isWeb[]`, …) over `currentConnection[]`.

`ConnectionStatus` tracks the lifecycle: `connectedConnection`, `disconnectingConnection`, `disconnectedConnection`. `shutdown[Connection]` marks a connection as disconnecting and asks its client to shut down; `shutdown[CustomUser]` does the same for every connection of a user; `reconnect[CustomUser]` asks every connected client of a user to reconnect. Connections are logged through `@defineLog` and shown on the `connections` form, which also lists the connection's forms, sessions, headers, cookies, and parameters.

### Connection URLs

| Property                        | What it builds                                                                |
|---------------------------------|--------------------------------------------------------------------------------|
| `origin[Connection]`            | `scheme://webHost:webPort` of the connection                                   |
| `webPath[Connection]`           | `origin` plus the context path                                                 |
| `currentOrigin[]` / `currentWebPath[]` | the origin / web path of `currentConnection[]`, or the request's own when there is no connection |
| `currentOriginUrl[STRING]` / `currentOriginUrl[LINK]` | a URL relative to the current origin                            |
| `currentContextUrl[STRING]`     | a URL relative to the current web path, with the connection query appended     |

### Change session log

The `Session` class records one committed change session.

| Property                                                            | What it holds                                                |
|----------------------------------------------------------------------|--------------------------------------------------------------|
| `user[Session]` / `nameUser[Session]` / `nameContact[Session]`       | the user that ran the session and the names                  |
| `dateTime[Session]`                                                  | the commit moment, set when the session is created           |
| `form[Session]` / `captionForm[Session]`                            | the form the changes came from and its caption               |
| `connection[Session]` / `hostnameComputerConnection[Session]` / `userLoginConnection[Session]` | the originating connection, its host, and its login |
| `quantityAddedClasses[Session]` / `quantityRemovedClasses[Session]` / `quantityChangedClasses[Session]` | how many objects were added, removed, changed |
| `changes[Session]`                                                  | the textual change detail                                    |

Sessions are logged through `@defineLog` and shown on the `changes` form, filtered by a date-time range. The `clearApplicationLog[]` extensions delete sessions older than `countDaysClearSession[]` and clear the change detail older than `countDaysClearSessionDetail[]`.

### Pings

A ping record stores per-computer memory readings over an interval, keyed by `(Computer, DATETIME from, DATETIME to)`.

| Property                                                                 | What it holds                                            |
|--------------------------------------------------------------------------|----------------------------------------------------------|
| `pingFromTo[Computer, DATETIME, DATETIME]`                               | the ping duration over the interval                      |
| `minTotalMemoryFromTo[…]` / `maxTotalMemoryFromTo[…]`                     | the minimum / maximum total memory over the interval     |
| `minUsedMemoryFromTo[…]` / `maxUsedMemoryFromTo[…]`                       | the minimum / maximum used memory over the interval      |

`limitPing[]`, `limitMaxTotalMemory[]`, and `limitMaxUsedMemory[]` set the alert thresholds. The `…Sum` properties total the time spent above each threshold and the `average…DateFrom` properties give the time-weighted averages over a range. The `pings` form shows the per-computer readings, the per-computer hardware taken from the last connection, and the thresholds and averages. `countDaysClearPings[]` sets how many days of ping records to keep, and the `clearApplicationLog[]` extension deletes the older ones.

### Push notifications and client interaction

| Property / action                                       | What it does                                                                |
|---------------------------------------------------------|------------------------------------------------------------------------------|
| `pushPublicKey[]` / `pushPrivateKey[]`                  | the VAPID key pair for web push, seeded on first start                       |
| `subscription[Connection]`                              | a connection's web-push subscription                                         |
| `notify[JSON, JSON]`                                    | shows a notification on the current connection's client                      |
| `push[Connection, JSON, JSON]`                          | pushes a notification to a connection's client                               |
| `pushNotify[Connection, JSON, JSON]`                    | pushes and shows a notification at once                                      |
| `notification[STRING, JSON]` / `notification[STRING]`   | builds the notification `JSON` from a title and options                      |
| `action[INTEGER]` / `action[STRING]`                    | builds the action `JSON` from a notification id or a URL                     |
| `share[STRING, STRING, STRING]` / `shareAction[STRING]` | shares a URL through the client's share dialog, falling back to a copy-link popup |
| `evalServer[TEXT]`                                       | runs the given code on the server                                            |
| `evalInAllCurrentConnections[TEXT, TEXT]`               | runs the given code on every connected client                               |

`customize[STRING, STRING]` opens the `customizeForm` dialog for adjusting a form: the base code and the `EXTEND FORM` code held in `dataExtendCode[Form]` (for all users) and `dataExtendCode[Form, User]` (for the current user). `formCustomizeBackground[]` and `formCustomizeShowIf[]` tint and gate the customization entry. The two code fragments are joined with a line break and compiled on every form open, seeing what the code that opens the form sees: the application's customization code (see below), and a script that opens the form ([`EVAL`](Eval_EVAL.md)). In the code, the form's name refers to the form the code makes: shown by this name — by an action of the code, or by a script run on the form — it opens as the code made it, without compiling the code again, while the form named by its canonical name opens anew. As it is saved, the code is checked with the application's code only, so a code that uses what a script declares is refused. If the result does not compile, the error is reported and the form is opened without the customization, so the dialog stays reachable to fix or drop the code.

`customizeFormUser[STRING, TEXT]` replaces only the current user's customization code for a form by its canonical name, without a dialog. An unknown form causes an error. The code takes effect when the form is reopened. `formRecreate[]` closes and reopens the current form and cannot be called outside a form.

Since `customize[STRING, STRING]` edits the code for all users and the current user's own, someone else's customization is reached from the `forms` metadata form instead: `hasExtendCodeAll[Form]` tells whether the selected form is customized for all users and `countExtendCode[Form]` counts the users who customized it for themselves, while the form's customization tab holds both code fragments and lets them be dropped - for one user or for the form as a whole.

The application as a whole is customized by code as well. The `customize[]` action, placed in the system folder of the administration, opens the same dialog for the code held in `dataExtendCode[]` (for all users) and `dataExtendCode[User]` (for the current user). The code adds elements to the [navigator](Navigator.md), moves and changes the existing ones (`NAVIGATOR`), adds windows, hides them and changes how a window draws the forms opened in it (`WINDOW`, `HIDE WINDOW`, `EXTEND WINDOW ... FORMS`), and declares the forms, actions and properties the added elements need. It also gives a window what draws it and the navigator what lays its windows out (`CUSTOM` of `WINDOW` and `EXTEND WINDOW`, `NAVIGATOR CUSTOM`) - a React component it names is one the pages of the application already have -, and schedules actions (`SCHEDULE` of `NAVIGATOR`), which a client the code customizes runs as it runs those the application schedules. The helper classes this module adds to the windows' classes by `customNavigator[]` and `customWindow[STRING[100]]` ([`System`](System_System.md)) still follow what the application gives, whatever the code changes in what draws and lays out the windows.

The two code fragments are joined with a line break and compiled for a connection of the user as it starts - a client, say - and its navigator statements change a copy of the navigator: the navigator of the application stays as it is, and a change of the code is seen by the connections started after it - the *OK* of `customize[]` saves the code and restarts its client. If the code does not compile, the error is reported and the client gets the navigator without the customization, with the dialog where the application puts it, to fix or drop the code. A code that compiles can still leave the dialog out of reach, by moving its element or hiding the window the element is drawn in: a web client started with the URL parameter `customize=false` (`/main?customize=false`, say) does not compile the code and gets the navigator of the application, with `customize[]` where the application puts it. In both cases the navigator of the application includes what the code of the `LogicsExtendCode` module, described below, does to it: the module is part of the logics. The *OK* of `customize[]` restarts such a client at the same address, so the code saved there takes effect in a client started without the parameter. An element the application declares keeps its access permissions wherever the code moves it; an element the code adds has a name of its own, in a namespace that is new on every compilation unless the code gives its own (`NAMESPACE`), and no permission can be given to it: the roles do not restrict it ([Security policy](Security_policy.md)).

`customizeNavigatorUser[TEXT]` replaces only the current user's application customization code without a dialog. It takes effect on the next connection. `reload[]` restarts the current client.

A code is compiled when it is saved, whoever writes it - the dialog, the `forms` form or an action - as it is compiled when the form opens or the connection starts, without running the code: the code for all users by itself, a user's code after the code for all users, joined with a line break, and a form's code with the application's code it is used with, as that code is stored - the application's code for all users for the form's code for all users, the user's for the user's. If the code does not compile, the apply is canceled with the compiler's error and nothing is saved. A code that is cleared, or written back as it is stored, is not compiled, so a code can be removed even when what remains contains an error.

The dialog edits the codes in a session of its own - the session of the form being customized may hold the user's unsaved changes - and saves them as a form that manages its session does, by its *Save* or *OK* action; only *OK* then opens the customized form anew or restarts the client. Closing the dialog with unsaved edits asks whether to discard them. If a save is refused, the dialog shows why - the compiler's error, or that the user may not write a code - and stays open with the edits.

`customizeFormUser[STRING, TEXT]` and `customizeNavigatorUser[TEXT]` save the code in a session of their own as well, without applying the caller's changes. `NULL` or empty code removes the user's customization. A canceled apply raises an error, with the apply's message if it has one - the compiler's error for a code that does not compile. The actions can be called through `/exec` or MCP.

Every customization code runs as a script does. This includes a user's own code, code for all users or another user, and code for a form or the application. Only a user who may evaluate a script can write it, by the same rule as for `/eval` ([Access from an external system](Access_from_an_external_system.md#authentication)). Anyone else is refused as the code is saved: their code is not compiled, and the apply is canceled as for a code that does not compile. A form is built of the codes as they are saved. Dropping such a code requires no right to evaluate a script.

What the code declares is seen by every script evaluated in a client the code customized: one the `EVAL` operator runs in an action of the navigator or of a form opened in that client, one the code of a page sends, and the code that customizes a form opened there. A name the code declares has no priority over the same name in the application or in a form's code: a script that gives such a name without a namespace is refused as ambiguous. A form's code that uses what this code declares compiles only where this code is in effect; elsewhere - a report the scheduler builds, a client whose code did not compile - the form is opened without its customization.

An action or a property the code declares is also found by its name when the code of a page calls it through the controller (`exec`, `change`). The name is looked for among what the application and the code declare, in every namespace alike, so what the code declares has no priority: a name the application declares as well is ambiguous until its namespace is given. The same holds on a form for what the form's code declares. A call by name is allowed as a call of the application's own action or property is: to those who may evaluate a script, or - where the code marks what is called `@@api` - to every authenticated user. An action of a form's code is opened to everyone who works with the form by the form's `CUSTOMS` list.

The code customizes the calls its user makes through the [Action API](Access_from_an_external_system.md) as well: a script sent to `/eval` sees what the code declares, as does the code that customizes a form the call prints or exports, and `/exec` finds an action the code declares by its name. The name is resolved the same way, and the call is allowed as a call of the application's own action is: `@@api` and `@@noauth` in the code count, so an action the code for all users marks `@@noauth` is called without authentication, whatever `enableAPI` is. A call without `session` takes the code as it is saved at the moment of the call, and a session of the stateful API keeps the code it was opened with, as a client does. If the code does not compile, the calls are made without the customization.

The logics itself is extended by code stored in the database as well: by the code held in `logicsExtendCode[]`, which the `customizeLogics` form, placed in the system folder of the administration next to `customize[]`, edits. The code is the body of the [module](Modules.md) `LogicsExtendCode`, which the platform adds over all the other modules of the logics: the module depends on each of them, so it sees every element they declare. The platform writes the header `MODULE LogicsExtendCode;` before the code, so the code may go on with the modules it depends on (`REQUIRE`), namespace priorities (`PRIORITY`) and a namespace of its own (`NAMESPACE`). A module the code depends on is loaded with all its [dependencies](Modules.md#depends) even when the top module ([`logics.topModule`](Projects.md)) does not depend on it. The code may declare what any module may - classes and their tables, stored properties, events and constraints, an implementation of `onStarted[]` (`onStarted() + { ... }`), navigator elements -, and the database is synchronized with it as with any module. It is read as the logics is built when the server starts, so a change of the code takes effect at the next start. What it declares is named in the `LogicsExtendCode` namespace, or in the one the code gives, and is given permissions as the elements of the other modules are. Being part of the logics, the module is in effect in a client started with `customize=false` as well.

The code is not compiled when it is saved. A code that does not compile stops the start of the server, as an error in any module does: the error names the `LogicsExtendCode` module, and the code can be fixed in the database, in the `SystemEvents_logicsExtendCode` field of the table of the same name. So does a code that no longer declares a table the previous code declared while the table holds data, as long as dropping such tables is denied (`db.denyDropTables`). A code that no longer depends on a module only it brought into the logics stops the start as well, as long as dropping that module is denied (`db.denyDropModules`). Changing the code, dropping it included, is allowed only to a user who may evaluate a script, by the same rule as for `/eval`; anyone else is refused as the code is saved.

### Logo

`logo[]` is the navigator logo image, and `logoAction[]` is the logo navigator entry, which shows the current version and user. The module places `logoAction` in the `logo` window of the navigator. Its header `logoHeader[]` — the application name on the mobile web client and with a vertical navbar — is an abstract property in the same value-based form as the class properties above, so a project overrides it the same way.

### Language

- [Module header](../language/Module_header.md) — the `MODULE` / `REQUIRE` syntax that pulls the module in.
- [`ABSTRACT` operator](../language/ABSTRACT_action_operator.md) — the abstract action lists the launch-event handlers are declared as.
- [`WHEN` statement](../language/WHEN_statement.md) — the event handlers that react to appearance changes and trigger a reload or a client resize.
- The [CSS reference](../language/CSS_view_styles.md#system-window-and-navigator-classes) lists the classes the module supplies for system windows and navigator entries and describes their effects.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Launch events`](Launch_events.md) — the launch-event concept. The lifecycle handlers (`onStarted`, `onWebClientStarted`, …) declared by this module live there.
- [`Journals and logs`](Journals_and_logs.md) — the system journals this module fills (exceptions, launches, connections, sessions, pings).
- [`Process monitor`](Process_monitor.md) — monitoring running connections and server activity.
- [`Navigator`](Navigator.md) — the navigator entries this module adds (logo, appearance, logs).
- [`Service`](System_Service.md) — service actions and database monitoring.
- [`Reflection`](System_Reflection.md) — metadata about the navigator, forms, and properties.
- [`Authentication`](System_Authentication.md) — users, contacts, and sign-in.
