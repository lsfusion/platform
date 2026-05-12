---
title: 'SCREENSHOT operator'
---

The `SCREENSHOT` operator creates an [action](Actions.md) that [captures](Capture_SCREENSHOT.md) the currently rendered user interface and writes the result into a property, or sends it to the client when no target property is specified.

### Syntax

```
SCREENSHOT [HTML] [captureTarget] [TO propertyId]
```

Where `captureTarget` is one of:

```
FORM
containerId
```

### Description

The `SCREENSHOT` operator creates an action that captures the currently rendered state of the user interface in the [web client](Capture_SCREENSHOT.md#client). The captured area is determined by the chosen [capture target](Capture_SCREENSHOT.md#target); the output [format](Capture_SCREENSHOT.md#format) is selected by the `HTML` keyword; the [destination](Capture_SCREENSHOT.md#destination) of the captured content is set by the `TO` clause.

### Parameters

- `HTML`

    Keyword. If specified, the inner HTML markup of the target is captured. The target property must accept `HTMLFILE` or one of the generic file types `FILE`, `NAMEDFILE`. If omitted, the target is captured as a PNG image; the target property must then accept `IMAGEFILE` or one of the generic file types `FILE`, `NAMEDFILE`.

- `captureTarget`

    Capture target. Specified in one of the following ways:

    - `FORM`

        Keyword. If specified, the main container of the form in whose context the action is executing is captured. The action must be executed in a form context.

    - `containerId`

        A [simple ID](IDs.md#id) of a container in the [design](Form_design.md) of the form in whose context the action is executing. The action must be executed in a form context, and the form must contain a container with the specified SID.

    If neither `FORM` nor `containerId` is specified, the whole document body of the web client is captured.

- `propertyId`

    [Property ID](IDs.md#propertyid) of the property the captured content is written to. The property must be parameterless and must accept either the format-specific file type (`IMAGEFILE` for an image, `HTMLFILE` for HTML) or one of the generic file types `FILE`, `NAMEDFILE`. If not specified, the generated file is sent to the client and opened by the operating system.

### Examples

```lsf
CLASS Report;
name = DATA ISTRING[100] (Report);
image = DATA IMAGEFILE (Report);
html = DATA HTMLFILE (Report);

FORM dashboard
    OBJECTS r = Report
    PROPERTIES(r) name, image, html
;

DESIGN dashboard {
    NEW chartBox {
        caption = 'Chart';
        MOVE PROPERTY(image(r));
    }
}

captureToClient ()  {
    SCREENSHOT; // delivered to the client and opened by the OS
}

captureViewport (Report r)  {
    LOCAL img = IMAGEFILE ();
    SCREENSHOT TO img;
    image(r) <- img();
}

captureForm (Report r)  {
    LOCAL img = IMAGEFILE ();
    SCREENSHOT FORM TO img;
    image(r) <- img();
}

captureChart (Report r)  {
    LOCAL img = IMAGEFILE ();
    SCREENSHOT chartBox TO img;
    image(r) <- img();
}

captureFormHtml (Report r)  {
    LOCAL page = HTMLFILE ();
    SCREENSHOT HTML FORM TO page;
    html(r) <- page();
}
```
