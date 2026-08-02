---
slug: "/WINDOW_statement"
title: 'WINDOW statement'
---

The `WINDOW` statement - creating a new [window](../paradigm/Navigator_design.md), the `EXTEND WINDOW` statement - changing the renderer of an existing window, the `HIDE WINDOW` statement - hiding an existing window.

### Syntax

```
WINDOW name [caption] [NATIVE] [options];
```

The `options` that appear at the end of the statement can be specified one after another in any order:

```
HIDETITLE 
HIDESCROLLBARS 
orientationType
POSITION(x, y, width, height)
fixedPositionType
HALIGN(alignType)
VALIGN(alignType)
TEXTHALIGN(alignType)
TEXTVALIGN(alignType)
CLASS cssClassExpr
CUSTOM customExpr
```

An existing window can be given a renderer, or hidden, with separate statements:

```
EXTEND WINDOW windowName CUSTOM customExpr;
HIDE WINDOW windowName;
```

### Description

The `WINDOW` statement declares a new window and adds it to the current [module](../paradigm/Modules.md).

By default a window is created that displays [navigator elements](../paradigm/Navigator.md).

The `EXTEND WINDOW` statement gives an already declared window - a standard one included - the React component or HTML template that draws its navigator elements, leaving everything else about the window as it is. The elements keep the window they were placed in, so the navigator's structure, its selection and its startup behavior stay as they were, and only the renderer changes. The window must already exist. Of the `NATIVE` windows only `System.forms` and `System.log` can be given a component, and no `NATIVE` window can be given a template. When several modules extend one window, the last literal is the markup the window starts with and the last property is the one that recomputes it.

The `HIDE WINDOW` statement hides the specified window, making it invisible. A hidden window draws nothing, so a window given a `CUSTOM` component and then hidden does not draw it, and the messages of a hidden `System.log` are logged and not shown anywhere.

### Parameters

- `name`

    Window name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](../paradigm/Naming.md#namespace).

- `caption`

    Window caption. [String literal](Literals.md#strliteral). If caption is not specified, the window's name will be used as the caption.  

- `NATIVE`

    Keyword specifying that the window is filled by the client rather than by the navigator: navigator elements cannot be placed into it. This is how the predefined `System.forms` window, where forms open, and `System.log` window, where user messages appear, are defined. For such a window only the `POSITION`, `CLASS`, `HIDETITLE`, and `HIDESCROLLBARS` options apply, while orientation and alignment are ignored.

- `windowName`

    Name of the window to hide. [Composite ID](IDs.md#cid) of an existing window.

### Options

- `HIDETITLE`

    Keyword specifying that no caption should be displayed in the user interface.

- `HIDESCROLLBARS`

    Keyword specifying that no scrollbars should be displayed for this window.

- `orientationType`

    Specifying the vertical or horizontal orientation of the toolbar or panel being created. Specified by one of the keywords:

    - `VERTICAL` - vertical orientation (default value).
    - `HORIZONTAL` - horizontal orientation.

- `POSITION (x, y, width, height)`

    Specifying the size and location of the window. 

    - `x`

        The left window coordinate. [Integer literal](Literals.md#intliteral) ranging from `0` to `100`.

    - `y`

        Top window coordinate. Integer literal ranging from `0` to `100`.

    - `width`

        Window width. Integer literal ranging from `0` to `100`.

    - `height`

        Window height. Integer literal ranging from `0` to `100`.

- `fixedPositionType`

    Specifying a fixed location of the window on the desktop, which does not allow the user to change its position and size. Here the window size is automatically determined based on the preferred dimensions of the component. The window will be located to the left, right, top, and bottom of the desktop, respectively. This option cannot be used simultaneously with the `POSITION` option. Specified by one of the keywords:

    - `LEFT`
    - `RIGHT`
    - `TOP` 
    - `BOTTOM`

- `HALIGN(alignType)`

    Specifying the horizontal alignment of the buttons in a vertical toolbar.

    - `alignType`

        Alignment type. This is specified using one of these keywords:

        - `START` - left alignment (default value).
        - `CENTER` - center alignment.
        - `END` - right alignment.

- `VALIGN(alignType)`

    Specifying the vertical alignment of the buttons in a horizontal toolbar.

    - `alignType`

        Alignment type. This is specified using one of these keywords:

        - `START` - top alignment (default value).
        - `CENTER` - center alignment.
        - `END` - bottom alignment.

- `TEXTHALIGN(alignType)`

    Specifying the horizontal alignment of text on the buttons. 

    - `alignType`

        Alignment type. This is specified using one of these keywords:

        - `START` - left alignment (default value).
        - `CENTER` - center alignment.
        - `END` - right alignment.

- `TEXTVALIGN(alignType)`

    Specifying the vertical alignment of text on the buttons. 

    - `alignType`

        Alignment type. This is specified using one of these keywords:

        - `START` - top alignment.
        - `CENTER` - center alignment (default value).
        - `END` - bottom alignment.

- `CLASS cssClassExpr`

    Specifying the name of the CSS class for the DOM element created for the window component in HTML. This can be used to apply custom styles.

    - `cssClassExpr`

        [Expression](Expression.md), whose value determines the class name.

- `CUSTOM customExpr`

    The window's navigator elements are drawn by a React component or an HTML template instead of the standard toolbar. Only the desktop web client draws them that way: the mobile web client and the desktop client keep their standard menu, and the same holds for the `System.forms` and `System.log` windows described below, which keep their standard view there.

    - `customExpr`

        [Expression](Expression.md) (string value). A [string literal](Literals.md#strliteral) matching `[A-Z][A-Za-z0-9_$]*` names a React component; a literal with markup in it, and any property, is an HTML template; a literal that is neither is an error. Only a literal names a component, since the renderer is chosen before the first value arrives, while a template given by a property is recomputed as that value changes and the window is drawn again from the new markup.

        A literal and a property may be given together, the way `CLASS` takes a literal and a property: the literal is the markup the window is drawn from until the property computes its first value, and the property replaces it from then on. A React component cannot be paired with a property, since the component draws the window itself and a computed template would never be drawn.

        The template is markup the application writes and the platform inserts as it is, so what it contains is the application's: a `<script>` in it does not run, while an event attribute written on an element - `onclick`, `onerror` - does. The template is the one the `custom` attribute of a [`DESIGN`](DESIGN_statement.md) container takes, with the same rules for writing a place; here `<Lsf:name>` is the place the standard button of the navigator element with that name goes into. In a literal the name is resolved in the module's [namespaces](../paradigm/Naming.md#namespace), so the element must already be declared and a name that resolves to nothing is an error; a template computed by a property is not resolved, and there the [canonical name](../paradigm/Naming.md#canonicalname) is written.

        The `System.forms` and `System.log` windows are the exception among `NATIVE` windows. They hold no navigator elements, but each can be given a React component, and that component is given what the running application put into the window instead - the forms open in `System.forms`, the messages logged in `System.log`. It places one of those with `<Lsf name/>`, and what it places nowhere is not lost: a form stays open and hidden, a message stays logged. Only a component may be given: there are no navigator elements for a template's places to take, and what such a window shows exists only while the application runs, so neither a template nor a property is accepted here.

        Any other `NATIVE` window cannot be given a component or a template, since it holds no navigator elements and nothing else it could be drawn from.

### Examples

```lsf
// creating system windows in the System module
WINDOW logo HORIZONTAL POSITION(0, 0, 10, 6) VALIGN(CENTER) HALIGN(START) HIDETITLE HIDESCROLLBARS CLASS logoWindowClass();
WINDOW root HORIZONTAL POSITION(10, 0, 70, 6) VALIGN(CENTER) HALIGN(CENTER) HIDETITLE HIDESCROLLBARS CLASS rootWindowClass();
WINDOW system HORIZONTAL POSITION(80, 0, 20, 6) VALIGN(CENTER) HALIGN(END) HIDETITLE HIDESCROLLBARS CLASS systemWindowClass();

WINDOW toolbar VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CLASS toolbarWindowClass();

// forms open in forms, messages are shown in log
WINDOW forms NATIVE POSITION(20, 6, 80, 94) CLASS formsWindowClass();
WINDOW log NATIVE POSITION(80, 6, 20, 93) HIDETITLE CLASS logsWindowClass();

// a horizontal toolbar at the bottom of the desktop, in which all buttons will be centered and text will be aligned up
// in this toolbar, for example, it is possible to place forms for quick opening
WINDOW hotforms HORIZONTAL BOTTOM VALIGN(CENTER) TEXTVALIGN(START);

// a window drawn by an application React component
WINDOW appMenu VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CUSTOM 'AppMenu';

// the window the forms open in, drawn by an application React component instead of the standard tabs
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';

// the window the messages are shown in, drawn by an application React component instead of the standard list
EXTEND WINDOW System.log CUSTOM 'MessageLog';

// hiding the predefined message window (then messages are shown as dialog forms)
HIDE WINDOW System.log;
```

