---
slug: "/WINDOW_statement"
title: 'WINDOW statement'
---

The `WINDOW` statement - creating a new [window](../paradigm/Navigator_design.md), the `EXTEND WINDOW` statement - changing the renderer of an existing window or how it draws the forms opened into it, the `HIDE WINDOW` statement - hiding an existing window.

### Syntax

```
WINDOW name [caption] [windowKind] [options];
```

The `options` that appear at the end of the statement can be specified one after another in any order:

```
HIDETITLE 
HIDESCROLLBARS 
AUTOSIZE
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

An existing window can be given a renderer, told how to draw the forms opened into it, or hidden, with separate statements:

```
EXTEND WINDOW windowName CUSTOM customExpr;
EXTEND WINDOW windowName FORMS [formsKind] [formsClose];
HIDE WINDOW windowName;
```

Where `windowKind` is one of:

```
NATIVE
FORMS [formsKind] [formsClose]
```

`formsKind` is one of:

```
TABBED
NOTABBED
```

and `formsClose` is one of:

```
CLOSE seconds
NOCLOSE
```

### Description

The `WINDOW` statement declares a new window and adds it to the current [module](../paradigm/Modules.md).

By default a window is created that displays [navigator elements](../paradigm/Navigator.md). A `FORMS` window holds forms instead, the way `System.forms` does: a form is opened into it with [`SHOW ... WINDOW windowName`](SHOW_operator.md), which is how a form becomes the application's header or side panel.

The `EXTEND WINDOW` statement gives an already declared window - a standard one included - the React component or HTML template that draws its navigator elements, leaving everything else about the window as it is. The elements keep the window they were placed in, so the navigator's structure, its selection and its startup behavior stay as they were, and only the renderer changes. The window must already exist. A `FORMS` window and `System.log` can be given a component and nothing else; any other `NATIVE` window can be given neither. When several modules extend one window, the last literal is the markup the window starts with and the last property is the one that recomputes it.

The same statement also says how a window that holds forms draws them and when it closes a form that was displaced, which is the only way to say it about `System.forms`, since the platform declares that window and an application cannot declare it again. At least one of the two is given, and each replaces only itself: an extension that says when to close leaves the window drawing its forms the way it did, and one that turns the strip over leaves the close policy the window was given. A window that does not hold forms cannot be extended this way. This is what makes the work area itself replace its content rather than collect tabs.

The `HIDE WINDOW` statement hides the specified window, making it invisible. A hidden window draws nothing, so a window given a `CUSTOM` component and then hidden does not draw it, and the messages of a hidden `System.log` are logged and not shown anywhere.

### Parameters

- `name`

    Window name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](../paradigm/Naming.md#namespace).

- `caption`

    Window caption. [String literal](Literals.md#strliteral). If caption is not specified, the window's name will be used as the caption.  

- `NATIVE`

    Keyword specifying that the window is filled by the client rather than by the navigator: navigator elements cannot be placed into it. This is how the predefined `System.log` window, where user messages appear, is defined. For such a window the `POSITION`, `CLASS`, `HIDETITLE`, `HIDESCROLLBARS` and `AUTOSIZE` options apply, and the orientation is the axis `AUTOSIZE` sizes the window across, while alignment is ignored.

- `FORMS`

    Keyword specifying that the window holds forms: navigator elements cannot be placed into it, and a form is opened into it with [`SHOW ... WINDOW windowName`](SHOW_operator.md). This is how the predefined `System.forms` window is defined, and it is the window a form opens in when `WINDOW` names none. The same options apply as for a `NATIVE` window, `AUTOSIZE` and the orientation it sizes the window across included, and `CUSTOM` may name a React component that draws the window instead, as described below.

    Such a window draws one form at a time and nothing of its own around it - no tab strip, no close button, and none of the platform's own toolbar - which is what a header, a side panel or a kiosk screen wants. Which form is drawn is the application's to say, with [`SHOW`](SHOW_operator.md) and [`ACTIVATE FORM`](ACTIVATE_operator.md). The window still holds more than one: when a form is opened into it, the form already there is displaced and asked to close, the request its close button makes, and a form with unsaved changes asks the user and may stay open - hidden behind the form drawn, where `ACTIVATE FORM` reaches it. Neither end of a pair opened with `WINDOW windowName` in synchronous mode takes part: the blocked form is not asked to close, since it cannot while that form is open, and the blocking form does not ask the forms already in the window to close.

    Only the desktop web client draws one form alone: the mobile web client draws such a window as a strip of tabs, where the form that stayed is a tab, and the desktop client draws `System.forms` alone, so a form opened into any other `FORMS` window opens there, as a tab.

- `TABBED`

    Keyword specifying that the window is drawn as a strip of tabs, one per open form, and the user switches between them; nothing is displaced and nothing is closed on its own, so `formsClose` cannot be given together with it. Usually one window in an application is like this, and it is `System.forms`, which is declared with it; a window declared without `formsKind` draws one form at a time.

- `NOTABBED`

    Keyword specifying that the window draws one form at a time. This is what a window declared without `formsKind` does, so it is written to turn `System.forms` over into a work area that replaces its content.

- `seconds`

    [Integer literal](Literals.md#intliteral) ranging from `0` to `2000000` - how long the displaced form is kept before it is closed. `CLOSE 0` asks it to close at once, which is what a window given no `formsClose` does, and a form with unsaved changes then asks the user and may stay. A wait closes only a form that closes without asking: a form with unsaved changes stays open, hidden behind the form drawn, until the application or the user closes it - the user is not asked about a form they are no longer looking at. A form the user is brought back to within the wait - by [`ACTIVATE FORM`](ACTIVATE_operator.md), by a [`SHOW ... ACTIVATE`](SHOW_operator.md) of the same form, or by working in it - is not closed at all, and being displaced again while it is still waiting does not start the wait over. A form that is being drawn is not displaced and is never put on a wait, and neither is one whose place was taken by a form that has not arrived yet: an open can fail, and a form opened with `WAIT` leaves the window holding what it held, which is the answer its opener comes back to.

    A window drawn by a `CUSTOM` component closes nothing by itself: what such a window draws is the component's to say, and it may draw several forms at once, so the platform cannot tell a form it stopped drawing from one it rearranged. Such a window closes what it holds through its own controller.

- `NOCLOSE`

    Keyword specifying that the displaced form is not asked to close at all: it stays open, hidden behind the form drawn, until the application or the user closes it.

- `windowName`

    Name of the window to hide. [Composite ID](IDs.md#cid) of an existing window.

### Options

- `HIDETITLE`

    Keyword specifying that no caption should be displayed in the user interface.

- `HIDESCROLLBARS`

    Keyword specifying that no scrollbars should be displayed for this window.

- `AUTOSIZE`

    Keyword specifying that the window is sized by what it draws, instead of taking the share its `POSITION` asks for - which is what a window drawing a header wants: no taller than its form. It is sized that way across its own orientation and stretched along it: a `HORIZONTAL` window is as tall as its form and as wide as the space it is given, a `VERTICAL` one as wide as its form and as tall as the space. When no orientation is given, a window holding navigator elements is `VERTICAL` and a `NATIVE` or `FORMS` window is `HORIZONTAL`, so a header declared without one is as tall as its form. `POSITION` still decides where the window sits and what it is grouped with. A group of windows is sized by its content only when every window in it is, so an `AUTOSIZE` window beside an ordinary one leaves the group stretching as before. Note that the client remembers a size the user dragged and restores it over the declared one, until [`Service.resetWindowsLayout`](../paradigm/System_Service.md) puts the declared sizes back. Only the web client sizes a window this way: the desktop client ignores `AUTOSIZE`.

- `orientationType`

    Specifying the vertical or horizontal orientation of the toolbar or panel being created. Specified by one of the keywords:

    - `VERTICAL` - vertical orientation. The default for a window holding navigator elements.
    - `HORIZONTAL` - horizontal orientation. The default for a `NATIVE` or `FORMS` window.

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

    The window's navigator elements are drawn by a React component or an HTML template instead of the standard toolbar. Only the desktop web client draws them that way: the mobile web client and the desktop client keep their standard menu, and the same holds for a `FORMS` window and the `System.log` window described below, which keep their standard view there.

    - `customExpr`

        [Expression](Expression.md) (string value). A [string literal](Literals.md#strliteral) matching `[A-Z][A-Za-z0-9_$]*` names a React component; a literal with markup in it, and any property, is an HTML template; a literal that is neither is an error. Only a literal names a component, since the renderer is chosen before the first value arrives, while a template given by a property is recomputed as that value changes and the window is drawn again from the new markup.

        A literal and a property may be given together, the way `CLASS` takes a literal and a property: the literal is the markup the window is drawn from until the property computes its first value, and the property replaces it from then on. A React component cannot be paired with a property, since the component draws the window itself and a computed template would never be drawn.

        The template is markup the application writes and the platform inserts as it is, so what it contains is the application's: a `<script>` in it does not run, while an event attribute written on an element - `onclick`, `onerror` - does. The template is the one the `custom` attribute of a [`DESIGN`](DESIGN_statement.md) container takes, with the same rules for writing a place; here `<Lsf:name>` is the place the standard button of the navigator element with that name goes into. In a literal the name is resolved in the module's [namespaces](../paradigm/Naming.md#namespace), so the element must already be declared and a name that resolves to nothing is an error; a template computed by a property is not resolved, and there the [canonical name](../paradigm/Naming.md#canonicalname) is written.

        A `FORMS` window - `System.forms` included - and the `System.log` window hold no navigator elements, but each can be given a React component, and that component is given what the running application put into the window instead - the forms open in the window, the messages logged in `System.log`. It places one of those with `<Lsf name/>`, and what it places nowhere is not lost: a form stays open and hidden, a message stays logged. Only a component may be given: there are no navigator elements for a template's places to take, and what such a window shows exists only while the application runs, so neither a template nor a property is accepted here.

        Any other `NATIVE` window cannot be given a component or a template, since it holds no navigator elements and nothing else it could be drawn from.

### Examples

```lsf
// creating system windows in the System module
WINDOW logo HORIZONTAL POSITION(0, 0, 10, 6) VALIGN(CENTER) HALIGN(START) HIDETITLE HIDESCROLLBARS CLASS logoWindowClass();
WINDOW root HORIZONTAL POSITION(10, 0, 70, 6) VALIGN(CENTER) HALIGN(CENTER) HIDETITLE HIDESCROLLBARS CLASS rootWindowClass();
WINDOW system HORIZONTAL POSITION(80, 0, 20, 6) VALIGN(CENTER) HALIGN(END) HIDETITLE HIDESCROLLBARS CLASS systemWindowClass();

WINDOW toolbar VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CLASS toolbarWindowClass();

// forms open in forms, messages are shown in log
WINDOW forms FORMS TABBED POSITION(20, 6, 80, 94) CLASS formsWindowClass();
WINDOW log NATIVE POSITION(80, 6, 20, 93) HIDETITLE CLASS logsWindowClass();

// a horizontal toolbar at the bottom of the desktop, in which all buttons will be centered and text will be aligned up
// in this toolbar, for example, it is possible to place forms for quick opening
WINDOW hotforms HORIZONTAL BOTTOM VALIGN(CENTER) TEXTVALIGN(START);

// a window above the forms area that holds one form at a time, no taller than its form - the application's header, opened once at client start
WINDOW header 'Header' FORMS AUTOSIZE POSITION(20, 6, 80, 10) HIDETITLE;
onWebClientStarted() + { SHOW appHeader WINDOW header NOWAIT; }

// a window drawn by an application React component
WINDOW appMenu VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CUSTOM 'AppMenu';

// the window the forms open in, drawn by an application React component instead of the standard tabs
EXTEND WINDOW System.forms CUSTOM 'FormsBoard';

// the window the messages are shown in, drawn by an application React component instead of the standard list
EXTEND WINDOW System.log CUSTOM 'MessageLog';

// hiding the predefined message window (its messages are then logged and shown nowhere)
HIDE WINDOW System.log;
```

