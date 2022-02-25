---
title: 'Navigator'
---

*Navigator* is a hierarchical structure (tree) consisting of *navigator elements*. There are three types of navigator elements:

-   *folder* - combines other navigator elements into a group. Selecting this element [affects](Navigator_design.md#selectedfolder) the display of its children if they belong to another [window](Navigator_design.md).
-   *action* – executes a specified [action](Actions.md). Only actions that take no arguments can be added to the navigator.
-   *form* – [opens](In_an_interactive_view_SHOW_DIALOG.md) a specified [form](Forms.md) in the interactive view and the [asynchronous](In_an_interactive_view_SHOW_DIALOG.md#flow) mode.

When an element is added to the navigator, a *parent* element is defined for it. The root element of the navigator is the `System.root` folder. 

If no navigator elements are displayed in a particular navigator folder, this folder is automatically hidden.

Just as an [interactive](Interactive_view.md) form view, the navigator is displayed in a 2D space: on the user's device screen. Therefore, it's [design](Navigator_design.md) can/has to be defined, as well as for all other [graphic](Form_views.md#graphic) views.

### Language

To manage the navigator use the [`NAVIGATOR` statement](NAVIGATOR_statement.md).

### Examples

```lsf
FORM items;
FORM stocks;
FORM legalEntities;
FORM shipments;
hello()  { MESSAGE 'Hello world'; }
hi()  { MESSAGE 'Hi'; }

NAVIGATOR {
    // creating a new navigator folder and making all its descendants appear in a window with a vertical toolbar
    NEW FOLDER catalogs 'Directories' WINDOW toolbar { 
        // creating a form element for the items form in the folder, the default element name is the form name
        NEW items; 
    }
    catalogs {  // navigator element editing statement
        // creating a stocksNavigator form element for the stocks form and adding the last element
        // to the catalogs folder
        NEW FORM stocksNavigator 'Warehouses' = stocks; 
        // creating a form element named legalEntities in the catalogs folder right after the items element
        NEW legalEntities AFTER items; 
        NEW shipments;
    }
    // creating another folder, the elements of which will also be displayed in a window with a vertical toolbar
    NEW FOLDER documents 'Documents' WINDOW toolbar { 
        // the folders themselves will be displayed in the root window, and when the user selects one of them
        // in a window with a vertical toolbar the descendants of this particular folder will be shown
        NEW ACTION hi;   // creating an action element
        NEW ACTION h=hello;   // creating an action element
        // the statement to move the shipments element from the catalogs folder to the document folder
        // before the hello element
        MOVE shipments BEFORE h; 
    }
}
```
