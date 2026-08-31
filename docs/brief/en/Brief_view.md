---
slug: "/Brief_view"
title: 'Brief: view logic'
---

- [Forms](#forms)
- [Form design](#form-design)
- [Navigator](#navigator)
- [Reports](#reports)
- [Internationalization](#internationalization)

## Forms

### Form structure

A form is the universal element describing which objects and which properties for them are read. It is declared by the [`FORM` statement](../language/FORM_statement.md) and consists of blocks:

- `OBJECTS` — the form's objects with their classes; objects in one pair of parentheses form an *object group*, whose sets are their cartesian product;
- `PROPERTIES` — the properties and actions the form's objects are passed to;
- `FILTERS` — condition properties: only the object sets with a non-`NULL` value remain;
- `ORDERS` — the ordering of the object sets;
- `FILTERGROUP` — filters the user picks one active filter from;
- `TREE` — several object groups shown as one tree table.

```lsf
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) date, number
    OBJECTS d = OrderDetail
    PROPERTIES(d) nameSku, quantity
    FILTERS order(d) = o
;
```

**Analogy**: a `SELECT` over several row sets at once, each with its own selection. In detail — [Form structure](../paradigm/Form_structure.md).

### Interactive view

In the [interactive view](../paradigm/Interactive_view.md) the user works with the open form: picks current objects, changes data properties, runs actions. Data is read as needed and changes are visible immediately. An object group is shown as a table or a panel, and only the groups listed in a `TREE` block are joined into one tree; the user can add filters and orderings of their own.

A property is displayed in exactly one object group — its *display group*, by default the last group whose objects it takes as arguments. A property that has no display group at all — one with no parameters, for instance — goes into the form's top-level `PANEL`, or into `TOOLBARBOX` in the `TOOLBAR` view.

Which view is used is set not in the form but in the operator that opens it, so one form serves all of them. Where the components land on screen is the [form design](../paradigm/Form_design.md), see [Brief: form design](Brief_view.md#form-design).

### Print view

The [print view](../paradigm/Print_view.md) is one of the two [static](../paradigm/Static_view.md) ones: all data is read at the moment the form is opened, with no feedback. The object groups are arranged into a hierarchy — a parent group's properties are output once for each of its own object sets, with the matching child sets under it, and then again for the next one. A file comes out of it only when a format is given; without one the report is rendered interactively — shown to the user, sent to the printer, or delivered as a message. The object groups are arranged into a hierarchy — the properties of a parent group are output once, those of a child group for all of its object sets.

It produces **PDF**, **DOC**, **DOCX**, **XLS**, **XLSX**, **HTML**, **RTF**: a preview, printing on a printer, or a file. The layout is described by a [report design](../paradigm/Report_design.md) — a `.jrxml` template found by the form's canonical name; where no template is found the platform generates an automatic design from the form structure ([Brief: reports](Brief_view.md#reports)).

### Structured view

The [structured view](../paradigm/Structured_view.md) is the second static view, beside the print one: it reads all the data at the moment the form opens and takes the hierarchy of its object groups — a parent group's properties once for each of ITS OWN object sets, with the matching child sets nested under it — but writes it as data rather than as a page — **JSON**, **XML**, **CSV**, **DBF**, **XLS**, **XLSX**, **TABLE**.

A form is both the shape the data is written in and the shape it is read back from. Reading back takes the same formats, except that Excel has one keyword, `XLS`, covering both `.xls` and `.xlsx` files. What each direction means is in [Brief: data export](Brief_integration.md#data-export-export) and [Brief: data import](Brief_integration.md#data-import).

### Opening a form (SHOW, DIALOG, PRINT, EXPORT)

The form opening operator creates an action that opens the form in the chosen view.

| Operator | View | Purpose |
| -------- | ---- | ------- |
| [`SHOW`](../language/SHOW_operator.md) | interactive | show the form to the user |
| [`DIALOG`](../language/DIALOG_operator.md) | interactive | the same plus returning the current value of an object as the entered value |
| [`PRINT`](../language/PRINT_operator.md) | print | a report: preview, printing, or a file |
| [`EXPORT`](../language/EXPORT_operator.md) | structured | exporting the form's data to a file |

The [`IMPORT` operator](../language/IMPORT_operator.md) does not open the form: it parses a file and writes the values into the form's properties, so that exporting the form back would recreate that file ([form import](../paradigm/In_a_structured_view_EXPORT_IMPORT.md#importForm)).

```lsf
selectSku (OrderDetail d) { DIALOG skus OBJECTS s INPUT DO sku(d) <- s; }
printOrder (Order o) { PRINT printOrder OBJECTS o = o XLSX TO orderFile; }
```

### Passing objects and the opening mode

The `OBJECTS` block of the opening operator passes object values to the form: the passed value becomes the current object in the interactive view, and a filter for equality to it in a static one. By default a `NULL` among the passed values cancels the action; the `NULL` keyword after the value allows it, and a `DIALOG` object marked `INPUT` or `CHANGE` allows it automatically.

`SHOW` and `DIALOG` also set the form location — `FLOAT`, `DOCKED`, `EMBEDDED`, `POPUP`, `IN`. `SHOW` takes `WAIT` / `NOWAIT`; a `DIALOG` has no such option and runs synchronously whenever its result is consumed — by a `DO` / `ELSE` continuation or by a `CHANGE` write-back.

In detail — [Opening a form](../paradigm/Open_form.md). A form is also opened by picking a [navigator](../paradigm/Navigator.md) item, see [Brief: navigator](Brief_view.md#navigator).

## Form design

### Containers

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

### Sizes and alignment

A component is given a *base size* in pixels (`size`, `width`, `height`). Beyond that, [along the main direction](../paradigm/Form_design.md#components) of a container the free space is divided between the children in proportion to their *extension coefficient* `flex` (the `fill` option sets it together with the alignment), and across that direction the *alignment* `align` applies — `START`, `CENTER`, `END`, `STRETCH`. **Analogy**: CSS Flexible Box Layout, where `flex` is `flex-grow` and the base size is `flex-basis`; in the web client the layout is implemented through it.

For a property, the size of the [value cell](../paradigm/Form_design.md#valueWidth) is set separately from the whole component: `valueWidth` and `valueHeight` in pixels, `charWidth` and `charHeight` in characters. It is also what sets the column width when the property is shown in a table. The `autoSize` option fits the base size to the content, and applies to text components only.

### The default design

The platform builds a [default design](../paradigm/Form_design.md#defaultDesign) from the form structure — the form's `BOX` with a ready container inside it for every object group and tree and for their tables, toolbars and panels — and `DESIGN` edits exactly that; the `CUSTOM` keyword in the statement header builds a design from scratch.

Which container a property component lands in is determined by its view on the form (`GRID`, `PANEL`, `TOOLBAR`, `POPUP`) and by its display group — the form's top-level `PANEL` or `TOOLBARBOX` when it has none — which is why a design usually comes down to moving ready containers around rather than creating components.

## Navigator

### Navigator structure

The [navigator](../paradigm/Navigator.md) is the tree the user starts working with the application from. Its elements come in three types: a *folder* groups other elements, a *form element* opens a [form](Brief_view.md#forms) in the interactive view, and an *action element* runs an action that takes no arguments. The root is the `System.root` folder. **Analogy**: the application menu together with its routing.

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

### Windows and placement

The [navigator design](../paradigm/Navigator_design.md) is a set of *windows*, areas of the screen each of which shows its own navigator elements. A window is declared by the [`WINDOW` statement](../language/WINDOW_statement.md): `VERTICAL` or `HORIZONTAL` sets the orientation of the toolbar, `POSITION(x, y, width, height)` sets the place on the `100` by `100` point desktop, and `LEFT`, `RIGHT`, `TOP` and `BOTTOM` pin the window to an edge. There are also `HIDETITLE`, `HIDESCROLLBARS`, the alignments `HALIGN`, `VALIGN`, `TEXTHALIGN`, `TEXTVALIGN`, `CLASS`, and `CUSTOM` — a custom view of the window, drawn by a React component or an HTML template. Only the desktop web client draws it: the mobile web client and the desktop client keep the standard menu.

Which window an element is drawn in is set by the `WINDOW` option of its parent folder. An element that ended up in a window other than the window of its folder is shown only when that folder is the one [selected](../paradigm/Navigator_design.md#selectedfolder) by the user in its own window — this is how a folder switches the content of a neighbouring window.

The [system windows](../paradigm/Navigator_design.md#systemwindows) are created by the platform: `System.forms` — the window forms open in, `System.log` — messages to the user, `System.root` and `System.toolbar` — the horizontal and vertical navigator toolbars, `System.system` — the system buttons, `System.logo` — the logo. The `EXTEND WINDOW ... CUSTOM` statement changes the renderer of an already declared window, and `HIDE WINDOW` hides it. Among the `NATIVE` windows only `System.forms` and `System.log` take a renderer, and only a React component — one that is handed what the application put into the window instead of navigator elements; the rest hold no navigator elements and take neither a component nor a template.

## Reports

### The print view

A *report* is a template that a document is built from by substituting data into it. A form's [print view](../paradigm/Print_view.md) consists of a set of reports, each given its own set of the form's [object groups](../paradigm/Form_structure.md#objects); how the groups are split between reports and how the `SUBREPORT` option on a group makes it a separate report — a *subreport* — is described in [building the report hierarchy](../paradigm/Print_view.md#buildhierarchy).

What happens to the document is chosen by the options of the [`PRINT` operator](../language/PRINT_operator.md), which come in two forms. `MESSAGE` shows the form to the user as a message: the properties of the root group become its header, and the table under it comes from the FIRST child group — a form with no child group gives an empty table, and every child group after the first is left out. The other form is the [interactive](../paradigm/In_a_print_view_PRINT.md#interactive) one: `previewMode` is either `PREVIEW`, the default, where the report is shown to the user, or `NOPREVIEW`, where it goes straight to the printer (`TO <expression>` picks the printer), while a format — `PDF`, `DOC`, `DOCX`, `XLS`, `XLSX`, `HTML`, `RTF` — together with `TO <property>` writes the document into a file on the server. `SHEET` and `PASSWORD` are available for `XLS` and `XLSX`.

```lsf
FORM printOrder
    OBJECTS o = Order
    PROPERTIES(o) date, nameCustomer

    OBJECTS d = OrderDetail SUBREPORT
    PROPERTIES(d) nameSku, quantity
    FILTERS order(d) = o
;

print (Order o) { PRINT printOrder OBJECTS o = o DOCX TO orderFile; }
```

### Report design

The [report design](../paradigm/Report_design.md) is how a report is laid out in the document. A report is described by a [template](../paradigm/Report_design.md#template) — a [`.jrxml` file](../paradigm/Report_design.md#format) of the JasperReports technology that the platform looks up in the server classpath by a name derived from the form's canonical name; the fields of the template are the form's properties, and their names and types match. The exact naming and what a wrong name costs are in [Rules: reports](../rules/Rules_view.md).

A report need not have a template of its own: if at least one is not found, the platform builds an [automatic design](../paradigm/Report_design.md#auto) from the form structure. A template can also be set explicitly, by a property holding either a file name or the template file itself: the `REPORT` block of the [`FORM` statement](../language/FORM_statement.md) for the top report, `REPORTS` (the synonym `REPORTFILES`) for the listed object groups, and the expression after `SUBREPORT` in an [object block](../language/Object_blocks.md) for that group.

## Internationalization

### Localizing strings and captions

Localization is reached for when one logic runs in several languages. A user-visible string — the caption of a class, a property, an action, a form, the text of a message — is localized by a string data identifier in curly braces inside a [string literal](../language/String_literal.md#localization). When the value is sent to the client, the platform looks each identifier up in the project files whose name ends with `ResourceBundle`, in the required locale, and substitutes the translation found; if there is no translation, the identifier itself is left without the braces.

```lsf
CLASS Book '{use.case.i18n.book}';
name '{use.case.i18n.book.name}' = DATA STRING[40] (Book);
```

The current locale — language, country, timezone — is taken from the `Authentication.language[CustomUser]` property and the like, and for actions started by the system, from the server locale; user data is not translated. The mechanism is described in [internationalization](../paradigm/Internationalization.md).

**Analogy**: ResourceBundle keys written straight into the caption text instead of a call to a translation function.

### Reverse translation

Reverse translation removes the need to place identifiers by hand: captions are written in the code as plain text in one language, and it is the platform that finds the entry whose value is that text — the entry keeps its own identifier as the key, and the text is its value. It is turned on by the `logics.lsfStrLiteralsLanguage` [launch parameter](../paradigm/Launch_parameters.md), which sets the language of the string literals in lsf code: at server start a `value -> identifier` dictionary is built from all ResourceBundle files of the project in that locale, and a plain literal matching an entry value is replaced at code parse time with that entry's identifier and then behaves as a localizable one. Leading and trailing spaces are excluded from the match and kept around the substitution, and a literal that is empty or all spaces is not replaced at all.

The replacement applies to any plain literal, not only to captions, so technical strings — JSON keys, addresses, formats, external identifiers — are written in the raw form of a [string literal](../language/String_literal.md), which takes part neither in localization nor in reverse translation:

```
'content'    // plain: localized, and replaced by reverse translation on a match
r'rawContent'  // raw: neither
```
