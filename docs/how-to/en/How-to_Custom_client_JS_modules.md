---
slug: "/How-to_Custom_client_JS_modules"
title: 'How-to: Custom client JS modules'
---

When an application needs its own browser JavaScript — a custom view for an object or property, a function bound by an [`INTERNAL CLIENT`](../language/INTERNAL_operator.md) action, or any other client-side code — that code can be kept as ordinary source files in the project and compiled into the web client at build time. The platform bundles each entry file and registers what it exports, so the application does not write any explicit loading or wiring for it. This applies to the web client only.

### Where the files go

Browser JavaScript is placed under _src/main/web_ in the logic module, for example _src/main/web/OrderBoard.jsx_ or _src/main/web/util.js_. The files may be _.js_, _.jsx_, _.ts_, or _.tsx_.

During the build, each file under _src/main/web_ (outside the _lib_ subfolder) is bundled (with [esbuild](https://esbuild.github.io/)) into a single _web/.compiled/&lt;name&gt;.js_ file on the classpath, where _&lt;name&gt;_ comes from the source path (a file in a subfolder becomes _&lt;subfolder&gt;\_&lt;name&gt;.js_). The bundle is loaded automatically when a page opens — no entry in the _onWebClientInit_ action is needed for it.

A _src/main/web/lib_ subfolder is treated as shared helper code: its files are not compiled into bundles of their own, but they can be imported from the entry files and are bundled in.

### Named exports and auto-registration

Each module exposes its components and functions as **named exports**. At load time every named export is registered into the `window.lsfusion.custom` registry under its export name, and the client resolves a custom name against this registry first. So a `DESIGN` `custom = 'OrderBoard'`, a `CUSTOM 'orderBoard'` object view, or an `INTERNAL CLIENT 'formatSum'` action finds the export with the matching name:

```js
export function OrderBoard(element, controller, list) {
    // render the list of objects into element
}
```

A name placed directly on the global `window` object still works as a fallback, so existing scripts that define `window.OrderBoard = ...` keep working, but named exports are the preferred form.

React itself is **provided by the platform**: a single vendored production build of React is loaded before any custom script, and `react` / `react-dom` imports in a module resolve to it. An application must not bundle its own copy of React.

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

### React Compiler

By default the source is bundled as written. The optional **React Compiler** pass (automatic memoization of React components) is enabled by setting the module/plugin option `reactCompiler = true`; it runs every source through the compiler before bundling. Unlike plain bundling, this pass needs `node` available on the build machine. Use it when custom React views benefit from the compiler's auto-memoization; leave it off (the default) otherwise, and plain bundling still works.

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

### Limitations

CSS bundling from these modules is limited: if a module imports a _.css_ file, the styles are extracted to a sibling _web/.compiled/&lt;name&gt;.css_ file that is **not** loaded automatically, and the build warns about it. For now, apply styling from within the component (inline styles or class names against CSS that is loaded separately) rather than relying on a bundled _.css_; a standalone stylesheet can still be loaded the usual way through the _onWebClientInit_ action (see [How-to: Custom Components (objects)](How-to_Custom_components_objects.md)).

This page covers the generic packaging of any custom browser JavaScript. For the React-specific views and the controller calls a custom view makes back into the server, see [How-to: Custom React views](How-to_Custom_React_views.md) and [How-to: Custom view controller API](How-to_Custom_view_controller.md).
