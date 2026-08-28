---
slug: "/Brief_navigator"
title: 'Brief: navigator'
---

## Navigator structure

The [navigator](../paradigm/Navigator.md) is the tree the user starts working with the application from. Its elements come in three types: a *folder* groups other elements, a *form element* opens a [form](Brief_forms.md) in the interactive view, and an *action element* runs an action that takes no arguments. The root is the `System.root` folder. **Analogy**: the application menu together with its routing.

The navigator is filled by the [`NAVIGATOR` statement](../language/NAVIGATOR_statement.md) as nested blocks: `NEW FOLDER`, `NEW FORM` and `NEW ACTION` create an element as a child of the current one, `MOVE` moves an existing one, and an element name with a block edits it; the position is `FIRST`, `LAST`, `BEFORE`, `AFTER`. The element options are: `WINDOW` — the [window](../paradigm/Navigator_design.md) for its children (with `PARENT`, for the element itself as well), `HEADER` — a caption from a property, `SHOWIF` — visibility, `IMAGE` / `NOIMAGE` — the [icon](../paradigm/Icons.md), `CHANGEKEY` and `CHANGEMOUSE` — a hot key and a mouse binding, `CLASS` — a CSS class. The `SCHEDULE PERIOD` statement creates a scheduler that runs an action with the given period in seconds.

```lsf
NAVIGATOR {
    NEW FOLDER catalogs 'Catalogs' WINDOW toolbar {
        NEW items;
        NEW FORM stocksNavigator 'Stocks' = stocks;
    }
    NEW FOLDER documents 'Documents' WINDOW toolbar {
        NEW ACTION recalculate;
    }
}
```

## Windows and placement

The [navigator design](../paradigm/Navigator_design.md) is a set of *windows*, areas of the screen each of which shows its own navigator elements. A window is declared by the [`WINDOW` statement](../language/WINDOW_statement.md): `VERTICAL` or `HORIZONTAL` sets the orientation of the toolbar, `POSITION(x, y, width, height)` sets the place on the `100` by `100` point desktop, and `LEFT`, `RIGHT`, `TOP` and `BOTTOM` pin the window to an edge. There are also `HIDETITLE`, `HIDESCROLLBARS`, the alignments `HALIGN`, `VALIGN`, `TEXTHALIGN`, `TEXTVALIGN`, `CLASS`, and `CUSTOM` — a custom view of the window, drawn by a React component or an HTML template. Only the desktop web client draws it: the mobile web client and the desktop client keep the standard menu.

Which window an element is drawn in is set by the `WINDOW` option of its parent folder. An element that ended up in a window other than the window of its folder is shown only when that folder is the one [selected](../paradigm/Navigator_design.md#selectedfolder) by the user in its own window — this is how a folder switches the content of a neighbouring window.

The [system windows](../paradigm/Navigator_design.md#systemwindows) are created by the platform: `System.forms` — the window forms open in, `System.log` — messages to the user, `System.root` and `System.toolbar` — the horizontal and vertical navigator toolbars, `System.system` — the system buttons, `System.logo` — the logo. The `EXTEND WINDOW ... CUSTOM` statement changes the renderer of an already declared window, and `HIDE WINDOW` hides it. Among the `NATIVE` windows only `System.forms` and `System.log` take a renderer, and only a React component — one that is handed what the application put into the window instead of navigator elements; the rest hold no navigator elements and take neither a component nor a template.
