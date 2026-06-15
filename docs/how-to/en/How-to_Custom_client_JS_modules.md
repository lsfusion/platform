---
slug: "/How-to_Custom_client_JS_modules"
title: 'How-to: Custom client JS modules'
---

When an application needs its own browser JavaScript — a custom view for an object or property, a function bound by an [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action, or any other client-side code — that code can be kept as ordinary source files in the project and compiled into the web client at build time. The platform bundles each entry file and registers what it exports, so the application does not write any explicit loading or wiring for it. This applies to the web client only.

### Where the files go

Browser JavaScript is placed under _src/main/web_ in the logic module, for example _src/main/web/OrderBoard.jsx_ or _src/main/web/util.js_. The files may be _.js_, _.jsx_, _.ts_, or _.tsx_.

During the build, each file under _src/main/web_ (outside the _lib_ subfolder) is bundled (with [esbuild](https://esbuild.github.io/)) into a single _web/.compiled/&lt;name&gt;.js_ file on the classpath, where _&lt;name&gt;_ comes from the source path (a file in a subfolder becomes _&lt;subfolder&gt;\_&lt;name&gt;.js_). The bundle is loaded automatically when a page opens — no entry in the _onWebClientInit_ action is needed for it.

A _src/main/web/lib_ subfolder is treated as shared helper code: its files are not compiled into bundles of their own, but they can be imported from the entry files and are bundled in.

### Without the build

A project with no build set up — no `node`, no esbuild, no `org.mvnpm` dependencies — can still ship custom client JS as a plain file under _src/main/resources/web_, used as written: no bundling, no JSX, no third-party-library resolution. (This is also the path `eval` uses.) A React view is therefore written with `React.createElement` against the platform-provided `window.React` instead of JSX, and the component is exposed on the global `window` (the fallback described below) instead of as a named export. A `custom` name matching `[A-Z][A-Za-z0-9_$]*` is still inferred as React:

```js
function HelloBoard(props) {
    var React = window.React;
    var rows = (props.data.o || {}).list || [];
    return React.createElement("div", { className: "hello-board" }, rows.length + " orders");
}
```

Because it is not bundled, a no-build file cannot `import` other local modules — it shares code only through globals on `window` (the platform provides `window.React` and `window.ReactDOM`). Importing local helpers and npm packages is what the build adds.

Such a file is loaded one of two ways — the same split that distinguishes the build path's auto-loaded bundles from anything an application lists in `onWebClientInit`.

**Auto-loaded — put it in _resources/web/init_.** A file under _src/main/resources/web/init_ is registered automatically when a page opens, with no _onWebClientInit_ entry — the no-build counterpart of how _src/main/web_ bundles auto-load after the build. Use it for a self-contained component or stylesheet:

```lsf
DESIGN orders {
    BOX(o) { custom = 'HelloBoard'; }   // from resources/web/init/helloBoard.js — nothing else to wire
}
```

Files in _web/init_ must be **load-order-independent**: the scan gives them all one load order, so each must register or define at load and use any other library lazily (at render or on an event), never reach into another script at load time.

**Registered explicitly — list it in _onWebClientInit_.** Any file under _src/main/resources/web_ outside _web/init_ is loaded by naming it in the [`onWebClientInit`](../language/INTERNAL_operator.md) action with an integer order. Use this when load order matters — a third-party library that must load before the component using it — or to load a file conditionally:

```lsf
onWebClientInit() + {
    onWebClientInit('helloBoard.js') <- 1;
}
```

The build and no-build paths compare as follows:

| | Build (_src/main/web_) | No-build, auto (_resources/web/init_) | No-build, explicit (_resources/web_) |
| --- | --- | --- | --- |
| Loading | bundled to _web/.compiled_, auto-loaded | auto-loaded by folder scan | listed in `onWebClientInit` |
| Source | _.js_/_.jsx_/_.ts_/_.tsx_, JSX allowed | plain _.js_ | plain _.js_ |
| Registration | named export | name on `window` | name on `window` |
| Load order | one order (bundles are self-contained) | one order (files must be order-independent) | explicit integer order |
| Third-party libraries | bundled via `org.mvnpm` | loaded separately, used from `window` | loaded separately, used from `window` |

### Named exports and auto-registration

Each module exposes its components and functions as **named exports**. At load time every named export is registered into the `window.lsfusion.custom` registry under its export name, and the client resolves a custom name against this registry first. So a `DESIGN` `custom = 'OrderBoard'`, a `CUSTOM 'orderBoard'` object view, or an `INTERNAL CLIENT 'formatSum'` action finds the export with the matching name:

```js
export function OrderBoard(element, controller, list) {
    // render the list of objects into element
}
```

A name placed directly on the global `window` object still works as a fallback, so existing scripts that define `window.OrderBoard = ...` keep working, but named exports are the preferred form.

React and ReactDOM are **provided by the platform**: a single vendored production build is loaded before any custom script, and `react`, `react-dom`, and `react-dom/client` imports in a module resolve to it. An application must not bundle its own copy of React or ReactDOM.

### Adding a third-party library

A third-party browser library is added as an ordinary **offline Maven dependency**, using the [mvnpm](https://mvnpm.org/) coordinates (`org.mvnpm:*`, the Maven Central mirror of npm) — there is no npm or yarn step. The dependency is resolved from the local Maven repository like any other, and the library, together with the npm packages it depends on, is bundled into the modules that import it:

```xml
<dependency>
    <groupId>org.mvnpm</groupId>
    <artifactId>apexcharts</artifactId>
    <version>3.54.1</version>
</dependency>
```

A scoped npm package (`@scope/name`) uses the group `org.mvnpm.at.<scope>` and the bare name as the artifact id. The module then imports the library by its npm name:

```js
import ApexCharts from "apexcharts";
```

### Adding a third-party library without the build

Without the build there is no `org.mvnpm` bundling, so a third-party library is loaded as a separate browser script and used from the global name it defines — a build that assigns itself to `window` (a UMD or plain-script build). The component reads it from there (`window.confetti`, `window.dayjs`, …) instead of importing it. There are two ways to supply that script.

Vendored, for an offline project. Place the library's browser build (the _.js_ that assigns the global) as a static file under _src/main/resources/web/init_ next to the component. Both auto-load, and the component works because it reads the global lazily, at render or on an event — so it does not matter which of the two scripts the scan injects first. Only the committed files are needed — no internet, no `org.mvnpm` dependency, no build:

```js
function ConfettiBoard(props) {                        // resources/web/init/confettiBoard.js
    var React = window.React;
    function celebrate() { window.confetti({ particleCount: 200, spread: 120 }); }
    return React.createElement("button", { onClick: celebrate }, "Celebrate");
}
// resources/web/init/confetti.umd.js sets window.confetti — both files auto-load, no onWebClientInit
```

From a URL, when the runtime is allowed to reach the internet. A URL is not a file to drop in _web/init_, so it is passed straight to `onWebClientInit`: a value that is not a local _web/_ resource and is an absolute URL is loaded as a `<script src>` on the page (the resolution rule is described under [`INTERNAL`](../language/INTERNAL_operator.md)), before the component that uses it:

```lsf
onWebClientInit() + {
    onWebClientInit('https://cdn.jsdelivr.net/npm/canvas-confetti@1.9.3/dist/confetti.browser.min.js') <- 1;
    onWebClientInit('confettiBoard.js') <- 2;
}
```

### React Compiler

By default the source is bundled as written. The optional **React Compiler** pass is general automatic memoization of React components — it stands in for hand-written `useMemo` / `useCallback` / `React.memo`. It is enabled per application by configuring the build plugin:

```xml
<plugin>
    <groupId>lsfusion.platform.build</groupId>
    <artifactId>web-compile-maven-plugin</artifactId>
    <configuration>
        <reactCompiler>true</reactCompiler>
    </configuration>
</plugin>
```

Unlike plain bundling — which uses the esbuild native binary and needs no Node — this pass runs through Node, which the build **acquires automatically**: it uses Node from `PATH` if present, otherwise it downloads a pinned, checksum-verified Node once and caches it (under _~/.m2_, so later builds and a CI _~/.m2_ cache reuse it). So enabling the flag is enough — no manual Node install on developer machines or CI. The exception is an **offline build** (`mvn -o`), which never downloads: if Node is then neither on `PATH` nor cached, it fails with guidance (install Node, or run one online build to seed the cache). Node is a build-time dependency only — a runtime / deploy box never needs it. (The plugin's `nodeVersion`, and `nodeDownloadRoot` for a mirror, are overridable if needed.)

It is **off by default, and most applications do not need it**: the common large-grid performance case — re-rendering only the rows that actually changed — is already handled by [`window.lsfusion.List`](How-to_Custom_React_views.md), independently of this pass, and `List` stays the right tool for it whether or not the compiler is on. Turn `reactCompiler` on when a custom React view itself has enough derived values, callbacks, or nested subtrees to benefit from general auto-memoization.

### Example

Put a tiny module in _src/main/web/orderUtil.js_ that exports a formatting function:

```js
export function formatSum(amount, currency) {
    return new Intl.NumberFormat("en-US", { style: "currency", currency }).format(amount);
}
```

Bind it to a client-side action and call it:

```lsf
formatOrderSum 'Format' (Order o) {
    INTERNAL CLIENT 'formatSum' (sum(o), 'USD');
}
```

The build compiles _orderUtil.js_ into _web/.compiled/orderUtil.js_ and registers `formatSum`; when the form opens, the action resolves the name from the registry and calls the function. No `onWebClientInit` entry is written for the module.

### Styles

A module can `import` CSS. esbuild gathers all CSS reachable from the module — its own `import "./styles.css"` and the CSS of any third-party library it imports — into a sibling _web/.compiled/&lt;name&gt;.css_, which is loaded automatically together with the bundle (no `onWebClientInit` entry, and no separate registration for a library's CSS). Fonts and images that the CSS references through `url()` are inlined into the compiled stylesheet as data URLs, so it is self-contained; load large images separately (for example through `onWebClientInit`) to keep the stylesheet from growing.

Recommended styling:

- **CSS modules** (`import styles from "./Component.module.css"`, used as `className={styles.root}`) for a component's own styles — the class names are scoped per module, so styles of different components on the same form do not collide.
- **Inline `style={{ ... }}`** for values computed from data (per-row colors and sizes, conditional styling).
- A plain `import "./Component.css"` (or a third-party library's CSS) is global; use it for vendor or deliberately global styles, and give the class names a namespace prefix. Do not register the compiled _.css_ manually — it is already auto-loaded.

For a full styling system beyond static class names, a **runtime CSS-in-JS** library (such as `styled-components` or `@emotion`) works as an ordinary `org.mvnpm` dependency: it is bundled with the module and injects its styles at runtime. Use the `styled` API or `className={css(...)}`; Emotion's `css` *prop* (`<div css={...} />`) needs a JSX transform that the build does not run, so it is not available.

CSS preprocessors (Sass/SCSS, Less, Stylus) and utility frameworks that generate CSS from a build step (Tailwind, UnoCSS) are **not** part of this build — plain bundling runs the esbuild binary only, with no Node or plugin phase (the optional React Compiler above is the only step that uses Node). Native CSS (nesting, custom properties) and CSS modules cover most of what a preprocessor was used for; if you do need one of these tools, generate the CSS with a separate step and ship the result as a plain stylesheet through `onWebClientInit`.

A standalone stylesheet that is not part of the build can still be shipped as a plain file and loaded through the [`onWebClientInit`](../language/INTERNAL_operator.md) action, like the CSS of a classic custom component (see [How-to: Custom Components (objects)](How-to_Custom_components_objects.md)).

This page covers the generic packaging of any custom browser JavaScript. For the React-specific views and the controller calls a custom view makes back into the server, see [How-to: Custom React views](How-to_Custom_React_views.md) and [How-to: Custom view controller API](How-to_Custom_view_controller.md).
