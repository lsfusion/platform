---
slug: "/Brief_design"
title: 'Brief: form design'
---

## Containers

The [form design](../paradigm/Form_design.md) describes how a form looks in the [interactive view](../paradigm/Interactive_view.md). It is a hierarchy of *containers* and of the *base components* the platform creates from the form structure: the table / tree (`GRID`), the system toolbar (`TOOLBARSYSTEM`), the user filter (`FILTERS`), the column calculations (`CALCULATIONS`), the filter group (`FILTERGROUP`), the property panel (`PROPERTY`).

The hierarchy is changed by the [`DESIGN` statement](../language/DESIGN_statement.md): `NEW` creates a container, `MOVE` moves a component, `REMOVE` takes it out, a component name with a block edits it, and `propertyName = value` sets a property. A component is picked by its name or by `PROPERTY(...)`, `GRID(...)`, `BOX(...)`, `PANEL(...)`, `TOOLBARBOX`, `GROUP(...)`, `PARENT(...)`; the insertion position is `FIRST`, `LAST`, `BEFORE`, `AFTER`.

The layout of the children is set by the container options `horizontal`, `tabbed`, `lines` (together with `grid`), and its look by `caption`, `image` ([icons](../paradigm/Icons.md)), `border`, `collapsible` (from code such a container is collapsed by the [`EXPAND` and `COLLAPSE`](../paradigm/Container_visibility_EXPAND_COLLAPSE.md) actions), `popup`, `showIf`, and `custom` — a React component or an HTML template, web client only.

```lsf
DESIGN order {
    NEW header FIRST {
        horizontal = TRUE;
        MOVE PROPERTY(date(o));
        MOVE PROPERTY(number(o)) { charWidth = 5; }
    }
    MOVE BOX(d) { fill = 1; }
    REMOVE TOOLBARLEFT;
}
```

## Sizes and alignment

A component is given a *base size* in pixels (`size`, `width`, `height`). Beyond that, [along the main direction](../paradigm/Form_design.md#components) of a container the free space is divided between the children in proportion to their *extension coefficient* `flex` (the `fill` option sets it together with the alignment), and across that direction the *alignment* `align` applies — `START`, `CENTER`, `END`, `STRETCH`. **Analogy**: CSS Flexible Box Layout, where `flex` is `flex-grow` and the base size is `flex-basis`; in the web client the layout is implemented through it.

For a property, the size of the [value cell](../paradigm/Form_design.md#valueWidth) is set separately from the whole component: `valueWidth` and `valueHeight` in pixels, `charWidth` and `charHeight` in characters. It is also what sets the column width when the property is shown in a table. The `autoSize` option fits the base size to the content, and applies to text components only.

## The default design

The platform builds a [default design](../paradigm/Form_design.md#defaultDesign) from the form structure — the form's `BOX` with a ready container inside it for every object group and tree and for their tables, toolbars and panels — and `DESIGN` edits exactly that; the `CUSTOM` keyword in the statement header builds a design from scratch.

Which container a property component lands in is determined by its view on the form (`GRID`, `PANEL`, `TOOLBAR`, `POPUP`) and by its display group — the form's top-level `PANEL` or `TOOLBARBOX` when it has none — which is why a design usually comes down to moving ready containers around rather than creating components.
