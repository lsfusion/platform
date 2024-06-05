---
title: 'WINDOW statement'
---

The `WINDOW` statement - creating a new [window](Navigator_design.md).

### Syntax

```
WINDOW name [caption] [options];
```

The `options` that appear at the end of the statement can be specified one after another in any order:

```
HIDETITLE 
HIDESCROLLBARS 
orientationType
POSITION(x, y, width, height)
fixedPositionType
HALIGN(alignType)
VALING(alignType) 
TEXTHALIGN(alignType)
TEXTVALIGN(alignType)
CLASS cssClassExpr
```

### Description

The `WINDOW` statement declares a new window and adds it to the current [module](Modules.md).

### Parameters

- `name`

    Window name. [Simple ID](IDs.md#id). The name must be unique within the current [namespace](Naming.md#namespace).

- `caption`

    Window caption. [String literal](Literals.md#strliteral). If caption is not specified, the window's name will be used as the caption.  

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

### Examples

```lsf
// creating system windows in the System module
WINDOW logo HORIZONTAL POSITION(0, 0, 10, 6) VALIGN(CENTER) HALIGN(START) HIDETITLE HIDESCROLLBARS CLASS logoWindowClass();
WINDOW root HORIZONTAL POSITION(10, 0, 70, 6) VALIGN(CENTER) HALIGN(CENTER) HIDETITLE HIDESCROLLBARS CLASS rootWindowClass();
WINDOW system HORIZONTAL POSITION(80, 0, 20, 6) VALIGN(CENTER) HALIGN(END) HIDETITLE HIDESCROLLBARS CLASS systemWindowClass();

WINDOW toolbar VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CLASS toolbarWindowClass();

// a horizontal toolbar at the bottom of the desktop, in which all buttons will be centered and text will be aligned up
// in this toolbar, for example, it is possible to place forms for quick opening
WINDOW hotforms HORIZONTAL BOTTOM VALIGN(CENTER) TEXTVALIGN(START);
```

