---
slug: "/System_SystemEvents"
title: 'SystemEvents'
---

`SystemEvents` is a [system module](System_modules.md) that declares the platform's launch-event handlers, the platform-version properties, the per-environment appearance settings, and the system journals that record exceptions, server launches, client connections, change sessions, and computer pings. It also collects the push-notification and client-interaction actions and the navigator entries for the logo, the appearance dialog, and the log forms. It is pulled in via `REQUIRE SystemEvents` (`System`, `Reflection`, and `Time` are pulled in automatically).

### Launch events

These abstract action lists are the [launch-event](Launch_events.md) handlers — a developer plugs initialization logic into them, and the platform runs the matching list at the corresponding moment of a server or client start. `onStarted[]` is the application-server start handler; the full set of handlers and the moments they fire are described in [`Launch events`](Launch_events.md).

| Handler                          | When it runs                                                                                  |
|----------------------------------|------------------------------------------------------------------------------------------------|
| `onInit[]`                       | early server initialization, before the main start; runs version synchronization              |
| `onStarted[]`                    | application-server start, before the server accepts client connections                         |
| `onFirstStarted[]`               | the very first application-server start only (when `firstStart[]` holds)                        |
| `onFinallyStarted[]`             | after `onStarted[]`, at the end of the start sequence                                          |
| `onClientStarted[]`              | a client connecting; dispatches to the desktop or web handler by client type                  |
| `onDesktopClientStarted[]`       | a desktop client connecting                                                                    |
| `onWebClientStarted[]`           | a web client connecting                                                                        |
| `onWebClientInit[STRING]`        | per-resource list keyed by a CSS / JS resource path, run when the web client initializes       |
| `onLoginInit[STRING]`            | per-resource list run on the login page                                                        |

`notFirstStart[]` is the stored flag that the server has started before; `firstStart[]` is its negation and selects the first start. Each handler has an `…Apply` wrapper (`onInitApply[]`, `onStartedApply[]`, `onFinallyStartedApply[]`, `onClientStartedApply[]`) that runs the list and commits with `APPLY`; `onStartedApply[]` additionally runs `onFirstStarted[]` on the first start and sets `notFirstStart[]`.

### Version info

| Property                  | What it holds                                                                 |
|---------------------------|-------------------------------------------------------------------------------|
| `platformVersion[]`       | platform version as `TEXT`                                                     |
| `apiVersion[]`            | API version as `INTEGER`                                                       |
| `revisionVersion[]`       | source revision as `INTEGER`                                                   |
| `synchronizeVersions[]`   | reads the running platform / API / revision values into the properties above; run from `onInit[]` |

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
| `mobileMode[DesignEnv]` / `mobileMode[]` | forces mobile layout on or off                                              |
| `suppressOnFocusChange[DesignEnv]` | suppresses the on-focus-change apply                                            |
| `contentWordWrap[DesignEnv]` / `contentWordWrap[]` | wraps content text                                                |
| `highlightDuplicateValue[DesignEnv]` / `highlightDuplicateValue[]` | highlights duplicate cell values                  |
| `userFiltersManualApplyMode[DesignEnv]` / `userFiltersManualApplyMode[]` | applies user filters manually rather than as typed      |
| `dontShowCloseButtonOnInactiveTab[DesignEnv]` / `dontShowCloseButtonOnInactiveTab[]` | hides the close button on inactive tabs           |

`ColorTheme` (light / dark / auto) is toggled by `toggleColorTheme[]`, which cycles light → dark → auto for the current environment, applies, and refreshes the form. The navigator pin mode is cycled by `toggleNavigatorPinMode[]`.

The `design` form edits the current environment's appearance (`captionTheme[DesignEnv]`, `captionSize[DesignEnv]`, `captionNavbar[DesignEnv]`, and the toggles above) and reloads the client on apply when a setting that needs a reload changed; `showDesign[]` opens it as a floating window. In the navigator's `system` window the module adds the `showDesign`, `toggleNavigatorPinMode`, and `toggleColorTheme` entries.

On web-client initialization `onWebClientInit[STRING]` registers the CSS and JS resources for the client: the Bootstrap bundle and size-specific padding / font stylesheets when `useBootstrap[]` holds, or the plain table stylesheets otherwise, followed by the shared widget scripts and styles. Resources placed under `/onStarted/` are picked up automatically.

### Exceptions

The `Exception` class hierarchy records server- and client-side errors. `Exception` is the abstract root, split into `ServerException` and `ClientException`; the client branch refines further:

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
| `revision[Launch]`    | the platform version, API version, and revision of the start   |

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

`limitPing[]`, `limitMaxTotalMemory[]`, and `limitMaxUsedMemory[]` set the alert thresholds; the `…Sum` properties total the time spent above each threshold and the `average…DateFrom` properties give the time-weighted averages over a range. The `pings` form shows the per-computer readings, the per-computer hardware taken from the last connection, and the thresholds and averages. `countDaysClearPings[]` sets how many days of ping records to keep; the `clearApplicationLog[]` extension deletes the older ones.

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

`customize[STRING, STRING]` opens the `customizeForm` dialog for adjusting a form: the base code and the `EXTEND FORM` code held in `dataExtendCode[Form]` (for all users) and `dataExtendCode[Form, User]` (for the current user). `formCustomizeBackground[]` and `formCustomizeShowIf[]` tint and gate the customization entry.

### Logo

`logo[]` is the navigator logo image; `logoAction[]` is the logo navigator entry, which shows the current version and user. The module places `logoAction` in the `logo` window of the navigator.

### Language

- [Module header](../language/Module_header.md) — the `MODULE` / `REQUIRE` syntax that pulls the module in.
- [`ABSTRACT` operator](../language/ABSTRACT_action_operator.md) — the abstract action lists the launch-event handlers are declared as.
- [`WHEN` statement](../language/WHEN_statement.md) — the event handlers that react to appearance changes and trigger a reload or a client resize.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Launch events`](Launch_events.md) — the launch-event concept; the lifecycle handlers (`onStarted`, `onWebClientStarted`, …) declared by this module live there.
- [`Journals and logs`](Journals_and_logs.md) — the system journals this module fills (exceptions, launches, connections, sessions, pings).
- [`Process monitor`](Process_monitor.md) — monitoring running connections and server activity.
- [`Navigator`](Navigator.md) — the navigator entries this module adds (logo, appearance, logs).
- [`Service`](System_Service.md) — service actions and database monitoring.
- [`Reflection`](System_Reflection.md) — metadata about the navigator, forms, and properties.
- [`Authentication`](System_Authentication.md) — users, contacts, and sign-in.
