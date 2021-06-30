---
title: 'WINDOW statement'
---

The `WINDOW` statement creates a new [window](Navigator_design.md).

### Syntax

    WINDOW name [caption] type [options]

The `options` that go at the end of the statement has the following syntax (the syntax for each option is indicated on a separate line):

    HIDETITLE 
    HIDESCROLLBARS 
    DRAWROOT 
    VERTICAL | HORIZONTAL
    POSITION(x, y, width, height)
    LEFT | RIGHT | TOP | BOTTOM
    HALIGN(alignType)
    VALING(alignType) 
    TEXTHALIGN(alignType)
    TEXTVALIGN(alignType)

### Description

The `WINDOW` statement declares a new window and adds it to the current [module](Modules.md). Options are listed one after another in arbitrary order, separated by spaces or line breaks. Depending on the selected window type – `TOOLBAR`, `PANEL`, `TREE`, or `MENU` – a toolbar, panel, tree, or menu will be created.

### Parameters

- `name`

    Window name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](Naming.md#namespace).

- `caption`

    Window caption. [String literal](Literals.md#strliteral). If caption is not specified, the caption of the window will be its name.  

- `type`

    Type of window to create. Specified with one of the keywords `TOOLBAR`, `PANEL`, `TREE`, or `MENU`.

- `options`

    - `HIDETITLE`

        Specifying that no caption should be displayed in the user interface.

    - `HIDESCROLLBARS`

        Specifying that no scrollbars should be displayed for this window.

    - `DRAWROOT`

        Specifying that the [navigator elements](Navigator.md) whose descendants will be added to this window will also be added to it.

    - `VERTICAL` | `HORIZONTAL`

        Specifying the vertical or horizontal orientation of the toolbar or panel being created. `VERTICAL` is used by default. This option only makes sense for `TOOLBAR` or `PANEL` windows.

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

    - `LEFT` | `RIGHT` | `TOP` | `BOTTOM`

        Specifying a fixed location of the window on the desktop, which does not allow the user to change its position and size. Here the window size is automatically determined based on the preferred dimensions of the component. The window will be located to the left, right, top, and bottom of the desktop, respectively. This option only makes sense for `TOOLBAR` windows and cannot be used simultaneously with the `POSITION` option.

    - `HALIGN(alignType)`

        Specifying the horizontal alignment of the buttons in a vertical toolbar. This option only makes sense for `TOOLBAR` windows with `VERTICAL` orientation.

        - `alignType`

            Alignment type. This is specified using one of these keywords:

            - `START` - all buttons will have the same left coordinate. Used by default.
            - `CENTER` - all buttons will be centered along the X axis.
            - `END` - all buttons will have the same right coordinate.

    - `TEXTHALIGN(alignType)`

        Specifies the horizontal alignment of text on the buttons in a vertical toolbar. This option makes sense only for `TOOLBAR` windows with `VERTICAL` orientation. 

        - `alignType`

            Alignment type. This is specified using one of these keywords:

            - `START` - the text will be located on the button on the left. Used by default.
            - `CENTER` – the text will be located in the center of the button.
            - `END` - the text will be located on the button on the right.

    - `VALIGN(alignType)`

        Specifies the vertical alignment of the buttons in a horizontal toolbar. This option only makes sense for `TOOLBAR` windows with `HORIZONTAL` orientation. 

        - `alignType`

            Alignment type. This is specified using one of these keywords:

            - `START` - all buttons will have the same upper coordinate. Used by default.
            - `CENTER` - all buttons will be centered along the Y axis.
            - `END` - all buttons will have the same lower coordinate.

    - `TEXTVALIGN(alignType)`

        Specifying the vertical alignment of text on the buttons in a horizontal toolbar. This option makes sense only for `TOOLBAR` windows with `HORIZONTAL` orientation. 

        - `alignType`

            Alignment type. This is specified using one of these keywords:

            - `START` - the text will be located at the top of the button.
            - `CENTER` - the text will be located in the center of the button. Used by default.
            - `END` - the text will be located at the bottom of the button.  


### Examples

```lsf
// creating system windows in the System module
WINDOW root 'Root' TOOLBAR HIDETITLE HIDESCROLLBARS HORIZONTAL POSITION(0, 0, 100, 6);
WINDOW toolbar 'Toolbar' TOOLBAR HIDETITLE VERTICAL POSITION(0, 6, 20, 64);
WINDOW tree 'Tree' TOOLBAR HIDETITLE POSITION(0, 6, 20, 64);

// menu without scrollbars under the root window
WINDOW menu MENU HIDESCROLLBARS DRAWROOT POSITION(20, 6, 80, 4);

// a horizontal toolbar at the bottom of the desktop, in which all buttons will be centered and text will be aligned up
// in this toolbar, for example, it is possible to place forms for quick opening
WINDOW hotforms TOOLBAR HORIZONTAL VALIGN(CENTER) TEXTVALIGN(START) BOTTOM;
```

