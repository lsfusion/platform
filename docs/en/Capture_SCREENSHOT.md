---
title: 'Capture (SCREENSHOT)'
---

The *capture* operator creates an [action](Actions.md) that captures the currently rendered visual state of the user interface as a PNG image or as HTML markup. The captured content is written into a [property](Properties.md) when a target property is specified, or sent to the client and opened by the operating system otherwise.

### Capture target {#target}

The capture target determines which portion of the user interface is captured:

-   *Document body* — the whole page body of the user's web client. The action does not depend on any [form](Forms.md) being open and can be called from any [action](Actions.md) context.
-   *Form* — the main container of the form in whose context the action is executing. The action expects a form context.
-   *Container* — a specific [container](Form_design.md#containers) of the form in whose context the action is executing, identified by its container SID from the form's [design](Form_design.md). The action expects a form context and the form must contain a container with the specified SID.

If a form or container target is specified but the required context is missing, the action fails with an error.

### Capture format {#format}

The captured content can be produced in one of two formats:

-   *Image* — a PNG screenshot of the target rendered as it appears on the screen. The target property must accept either [`IMAGEFILE`](Built-in_classes.md) or one of the generic file types [`FILE`, `NAMEDFILE`](Built-in_classes.md).
-   *HTML* — the inner HTML markup of the target. The target property must accept either [`HTMLFILE`](Built-in_classes.md) or one of the generic file types [`FILE`, `NAMEDFILE`](Built-in_classes.md).

The target property must be parameterless.

### Capture destination {#destination}

The captured content is written to the target property when a target property is specified. Otherwise the generated file is sent to the client and opened by the operating system.

### Client support {#client}

The capture operator runs only in the web client. It relies on the live DOM in the browser to render the screenshot; on the desktop client the action is not supported and any attempt to execute it fails.

### Language

To create the action, use the [`SCREENSHOT` operator](SCREENSHOT_operator.md).

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
