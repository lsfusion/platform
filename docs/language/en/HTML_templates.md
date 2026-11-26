---
slug: "/HTML_templates"
title: 'HTML templates'
---

An HTML template is markup that lays out a form container's children in the web client or a navigator window's elements in the desktop web client. It is supplied through the [`custom` design attribute](DESIGN_statement.md#containerprops) for a container or the [`CUSTOM` window option](WINDOW_statement.md#custom) for a navigator window.

### HTML markup {#markup}

The platform inserts the application's HTML as supplied, with special handling for template places. Event handler attributes such as `onclick` and `onerror` run when their events occur. The browser leaves `<script>` elements inert.

The template runs without React and receives no data. It can place views by name. Any other markup is supplied by the application.

A literal template can contain at most 65,535 bytes in modified UTF-8. The platform rejects a longer literal at its declaration. Use a template computed by a property for larger markup.

### HTML template places {#template-places}

`<Platform:name>` marks where the platform puts the view with that name. The platform replaces the tag with the view itself, without a wrapper for each view. A child component or navigator element with no place in the template is omitted.

Write each place as a single opening tag with a name. Closing and self-closing tags for places are errors. The `Platform:` prefix can use any letter case. Write the name after it in its exact case. Give each name at most one place.

#### Form container places {#form-container-places}

Use the design's [component identifier](DESIGN_statement.md#selector) as the name, for example `PROPERTY(qty)` for a property component. A place for a component elsewhere on the form stays empty.

The template has no access to child or container captions and images as data. The platform draws an inline property's caption and comment in their own places, for example `<Platform:PROPERTY(qty).caption>` and `<Platform:PROPERTY(qty).comment>`. The template can put these places in its own markup, such as a section heading. With `inline = FALSE`, the caption and comment are part of the property view. Other child views include their own captions. The platform draws the container's own caption and image next to the container, outside the template.

Adding a child to a template requires editing its string. A template string belongs to the module that declared it and can only be replaced whole.

#### Navigator window places {#navigator-window-places}

The platform puts the navigator element's standard button in its place. Placement uses that button regardless of the element's `NOCUSTOM` option.

In a literal, the platform resolves the name in the module's [namespaces](../paradigm/Naming.md#namespace), so the navigator element must already be declared. In a template computed by a property, write the element's [canonical name](../paradigm/Naming.md#canonicalname) directly.

This template places the buttons for the previously declared navigator elements `items` and `partners` in `System.toolbar`, the window that draws them.

```lsf
EXTEND WINDOW System.toolbar CUSTOM
    '<div class="menu"><div class="menu-head">Master data</div><Platform:items><Platform:partners></div>';
```

### Place errors {#place-errors}

The platform checks the places in literal templates when it builds the form or processes `WINDOW` or `EXTEND WINDOW`. It rejects places with a missing name, a repeated name or a closing or self-closing tag. It also rejects a name that identifies no component on the form or resolves to no navigator element, respectively.

The web client checks a template computed by a property as it draws it. It reports a missing name, a repeated name or an unknown name both at the place and in the browser console. It reports closing and self-closing tags only in the console, because the browser has already processed those tags. A place for a navigator element assigned to a different window is also reported at the place and in the console.
