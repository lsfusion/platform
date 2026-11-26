---
slug: "/CSS_view_styles"
title: 'Styles of platform views'
---

An application's CSS styles the HTML elements that the web client draws for forms, navigator elements, and windows. Selectors can use classes supplied by the application or attributes set by the web client.

### CSS classes {#classes}

A class string can contain several CSS class names separated by spaces.

Use `name=value` tokens with `class`, `captionClass`, `valueClass`, `footerClass`, and the `CLASS` options in the table. The client sets an inline style if `name` is a property of the element's `style` object and sets an HTML attribute otherwise.

| Option | HTML element |
|---|---|
| [`class`](DESIGN_statement.md#component-properties) | The element of a form component placed in a container. For a property or action in a panel, it is the component around the value and caption. For a property in a table, it is the render element, either the cell itself or an element inside it. `class` has no effect on the form's main (root) container. Select it with [`lsfusion-form`](#selector-attributes). |
| [`captionClass`](DESIGN_statement.md#containerprops) | The container's caption element, the caption of a property or action in a panel, or the column header of a property in a table. |
| [`valueClass`](DESIGN_statement.md#containerprops) | The container's content element or the [value element of a property or action](#property-value-classes). |
| [`footerClass`](DESIGN_statement.md) | The footer cell of the property's column. |
| [`commentClass`](DESIGN_statement.md) | The comment element of the property or action in a panel. |
| [`CLASS` in `NAVIGATOR`](NAVIGATOR_statement.md) | The navigator element's button in the standard view. |
| [`CLASS` in `WINDOW`](WINDOW_statement.md) | The window's view element. |
| [`CLASS` in `PROPERTIES`](Properties_and_actions_block.md#options) | The same [value element](#property-value-classes) as `valueClass` for a property or action. |

#### Property value classes {#property-value-classes}

A non-`NULL` value for `valueClass` or `CLASS` in `PROPERTIES` replaces the platform's default value classes, such as `form-control`. The classes go on the input element if the value is rendered with one. For a typical text cell in a table, they go on the cell itself (`td`). A button group or dropdown for a property with a small number of options is drawn inside the value element and keeps its own classes.

### Property value elements {#property-value-elements}

The table lists the default HTML tags for property and action values. The [`tag` design option](DESIGN_statement.md#properties-of-actions-and-properties-on-the-form) overrides this choice.

| HTML tag | When the platform chooses it |
|---|---|
| `a` | A value of the `LINK` class or a link to a file of a specific type, or a property whose change action opens a form, changes no data and has no input. |
| `input` | A value entered as text (a string, number, date or time) in a panel under the [value cell rendering rules](../paradigm/Form_design.md#cellrender), or a logical value. |
| `button` | An action that takes no input value and has an effect. |
| `select` | A value chosen from a dropdown rendered as a native selection element under the value cell rendering rules. |

In the other cases, including a value drawn by a render function (`custom`), the value has no explicit tag. In a panel the client renders it in a `div`. See [property value classes](#property-value-classes) for the value element in a table.

A `NULL` value of a string class displayed in an `a` element has the default placeholder `Not defined`. An explicit `placeholder` takes precedence. With the standard Bootstrap styles, a placeholder in an `a` uses the link color at half opacity and a placeholder in a `div` uses the secondary text color.

### Selector attributes {#selector-attributes}

The web client sets these attributes on the elements it draws. A CSS attribute selector can address a form or a component directly.

| Attribute | Element | Value |
|---|---|---|
| `lsfusion-form` | Root element of the form | [Canonical name](../paradigm/Naming.md#canonicalname) of the form, when it has one. |
| `lsfusion-container` | Form component's element | [Component identifier](DESIGN_statement.md#selector), such as `PROPERTY(name(i))` or the name of a container. |

[WebMCP addresses](../paradigm/MCP_server.md#webmcp) use additional attributes, including `lsfusion-form-label`, `lsfusion-form-window`, `lsfusion-property`, `lsfusion-group`, and `lsfusion-row`. React's `<Platform>` and `usePlatform` have their own [attributes for custom placement](React_platform_components.md#custom-placement).

### Color theme {#color-theme}

While the dark [color theme](../paradigm/User_interface.md#appearance) is on, the page's root element (`<html>`) has the attribute `data-bs-theme` with the value `dark`. The selector `[data-bs-theme="dark"] .board` then matches elements of class `board`. With the light theme the attribute is absent or has the value `light`. The attribute changes as the theme is switched, without a reload of the page.

### Component layout {#layout}

For form containers laid out with [CSS flexbox](https://www.w3.org/TR/css-flexbox-1/#flex-property), the table shows how the [form layout](../paradigm/Form_design.md#components) and the [design options](DESIGN_statement.md#component-properties) correspond to CSS properties.

| Form design | CSS property |
|---|---|
| Extension coefficient (`flex`) | `flex-grow` |
| Base size along the container's main direction | `flex-basis`. The web client uses `auto` and sets the size through `width` or `height` along that direction. |
| Shrink below the base size along the container's main direction (`shrink`) | `flex-shrink` (`1` for `TRUE`, `0` for `FALSE`) |
| Horizontal overflow of a component (`overflowHorz`) or its value cell (`valueOverflowHorz`) | `overflow-x` |
| Vertical overflow of a component (`overflowVert`) or its value cell (`valueOverflowVert`) | `overflow-y` |

For containers laid out with CSS Grid, the web client sets track sizes in `grid-template-columns` and `grid-template-rows`. For tracks corresponding to the container's `lines`, `lineSize` specifies the base size and `lineShrink` controls shrinking. A track with no specified base size uses `min-content`. A positive extension coefficient sets the maximum in `fr`, as in `minmax(min-content, 1fr)`. With shrinking enabled, the minimum is `0px`, and without it the minimum is the base track size. For a track with no positive extension coefficient the client uses `minmax()` with `0px` and the base track size if shrinking is enabled, or the base track size alone otherwise.

:::info
The web client sizes table columns using [`table-layout: fixed`](https://www.w3.org/TR/CSS22/tables.html#fixed-table-layout). For each column, the client sets its base width on a sizing cell. For a stretchable column, a second sizing cell gets a percentage based on the column's share of the total extension coefficient. Header and value cells of a stretchable column span both sizing cells.
:::

### System window and navigator classes {#system-window-and-navigator-classes}

[SystemEvents](../paradigm/System_SystemEvents.md) supplies these class strings for [system windows and navigator entries](../paradigm/System_System.md). It appends the helper classes to the base classes when the corresponding conditions hold.

| Window or navigator entry | Base classes | Helper classes and their conditions |
|---|---|---|
| `logo` | `bg-dark-subtle` | The desktop web client uses these classes when no React component draws the window. A vertical navbar gets `navbar-force-align-start` and also `navbar-text-on-hover` when `navigatorPinMode[]` is `NULL` and no React component lays out the windows. A horizontal navbar gets `navbar-auto-icon-font-size`. |
| `root` | `bg-dark-subtle navbar-icon-large` | The desktop web client uses these classes when no React component draws the window. A vertical navbar gets `navbar-force-align-start`. Either orientation gets `navbar-text-on-hover` when `navigatorPinMode[]` is `NULL` and no React component lays out the windows. |
| `system` | `bg-dark-subtle` | On the desktop web client, `navbar-icon-large` always applies and a horizontal navbar gets `navbar-text-hidden`. For a vertical navbar whose window has no React component, that client adds `navbar-force-align-start` and also `navbar-text-on-hover` when `navigatorPinMode[]` is `NULL` and no React component lays out the windows. |
| `forms` | `bg-body-tertiary` | `dont-show-close-button` on the mobile web client, or when `dontShowCloseButtonOnInactiveTab[]` holds and no React component draws the window |
| `toolbar` | `bg-body-secondary` | `navbar-popup-over-selected-hover` on the desktop web client unless the navigator is pinned entirely (`navigatorPinMode[]` is `TTRUE`) or a React component lays the windows out |
| `log` | `bg-body-secondary` | `navbar-popup-over-selected-hover navbar-always-hide-unpinned` on the desktop web client while the log window is not pinned (`logsPinMode[]`) and no React component lays the windows out |
| `logoAction` | `navbar-icon-large` | `navbar-hidden` on the mobile web client without Bootstrap (`useBootstrap[]` does not hold) |
| `toggleNavigatorPinMode` | — | `navbar-hidden` on the mobile web client |

| CSS class | Effect in the standard view |
|---|---|
| `bg-dark-subtle`, `bg-body-tertiary`, `bg-body-secondary` | Set the window background using the Bootstrap theme. |
| `navbar-icon-large` | Enlarges navigator icons. |
| `navbar-text-on-hover` | Reveals navigator captions on hover while a form is open. |
| `navbar-force-align-start` | Aligns navigator buttons at the start of the cross axis. |
| `navbar-auto-icon-font-size` | Sizes font icons to the height of the horizontal navbar in Chromium and Safari. |
| `navbar-text-hidden` | Hides navigator captions. |
| `dont-show-close-button` | Hides the close button on inactive form tabs. |
| `navbar-popup-over-selected-hover` | Opens the window's navigator items as an overlay on hover or after selection when a form is open. |
| `navbar-always-hide-unpinned` | Keeps the unpinned log window collapsed when idle, including when no form is open. |
| `navbar-hidden` | Hides the navigator button. |

### Stylesheet loading {#stylesheet-loading}

Use a [CSS import in a client JavaScript module](Client_JavaScript_modules.md#styles) for a stylesheet included in the build. A standalone `.css` file in [`resources/web/init`](Client_JavaScript_modules.md#without-the-build) loads automatically when a page opens. You can also register a stylesheet for web-client initialization with `onWebClientInit[STRING]` or load it explicitly with [`INTERNAL CLIENT`](INTERNAL_operator.md).
