---
title: 'Interactive view'
---

A form opened in *interactive* mode is a graphical component with a certain [design](Form_design.md) in which the user can trigger various [events](Form_events.md) and thereby navigate through system objects, view and change [property](Properties.md) values, execute [actions](Actions.md), and so on. Developers can also use an [additional set of operators](Form_operators.md) with this view, making it possible to manage the open form.

### Object views {#objects}

In the interactive view, object groups can be displayed in a table. The rows in the table are object collections, and the columns are properties. The records displayed in the table and their order are determined by the current [filters](Form_structure.md#filters) and [orders](Form_structure.md#sort).

[Current values](Form_structure.md#currentObject) of objects can change either as a result of an action created using the special search operator [(`SEEK`)](Search_SEEK.md), or as a result of a change to the current row, if an object group is displayed in a table.

When an object group is displayed in a table, the number of rows (object collections) displayed can either be determined automatically based on the height of the visible part of the table, or specified by the developer explicitly when creating the form.

### Object trees {#tree}

The platform also allows to display multiple object groups in one table simultaneously. This happens similarly to the [object group hierarchy](Static_view.md#hierarchy) in a static view, i.e. if we have two groups `A` and `B` then, in the "joined" table, the first object collection from `A` is displayed first, then all object collections from `B` (as filtered), then a second object collection from `A`, then again all the object collections from `B` and so on. In this case, it is highly desirable that the filters for `B` used all objects from `A`, since otherwise combining these groups into a single tree doesn't make sense. Initially, when a form is opened in the table, only objects of the topmost object group are displayed, but at the same time, a special column is created on the left of the table, using which the user can open nodes on his own and thus view only objects of interest in the lower object groups. Another function of this created column is to demonstrate the nesting of nodes by tabulating the elements inside this column (this allows the user to better understand what level of the hierarchy he is currently at).

<a className="lsdoc-anchor" id="treegroup"/>

Object trees also can be used to display hierarchical data (such as classifiers). In this case, the descendants of the object collection of a group in the tree can be not only object collections of lower groups but also object collections of the same group (such an object group shall be called *hierarchical*). To determine these child object collections in a hierarchical object group, it is necessary to define an additional filter for it – which, unlike regular filters, can refer not only to the values of the filtered object collections but also to the values of the "upper in the tree" object collection (the same approach is used in the [recursion](Recursion_RECURSION.md) operator). It is highly desirable that the hierarchical filter uses all the values of the upper object collections, since otherwise, as with filters between different groups of objects, creating such a tree doesn't make sense. Initially, it is assumed that all values of the "upper in the tree" object collection are `NULL`.


:::info
In the current platform implementation, hierarchical groups allow only trees to be displayed (not directed graphs). Accordingly, it is allowed to use only values of the upper object collections and properties that take lower (filtered) values of objects as input for a hierarchical filter (so that it is guaranteed that the same tree node cannot be reached in different ways)
:::

The properties of different object groups in the tree are arranged in columns under each other, that is, the first column displays the first properties of each object group, the second column displays the second ones, and so on. The total number of tree columns is determined by the last group of objects on the tree (all "extra" properties of the upper groups are simply ignored).

### Property views {#property}

Any property or action can be displayed on a form in one of the following *views*:

-   *Panel* (`PANEL`): a separate component that displays a property caption and this property value for the current values of the form objects.
-   *Toolbar* (`TOOLBAR`): similar to a panel, but this component has a different default location (immediately below the table), and if the table to which a toolbar belongs is hidden then the toolbar is hidden with it.
-   *table column* (`GRID`): a separate column in the table that displays the property values for all object collections (rows) in the table.

For each object group, you can specify which *default view* the properties of this group will be displayed in (by default, this view is a table column). If the property has no parameters (that is, it does not have a display group), it is displayed in a panel. Actions are always displayed in a panel by default.


:::info
For the remainder of the section, the behavior of properties and actions is exactly the same and so we will use only the term property (behavior is absolutely identical for actions).
:::

If necessary, the developer can explicitly specify which view a property should use.

If at any point there are no properties displayed in the table for the object group, the table is automatically hidden.

By default, the caption of each property on the form is the title of the property itself. If necessary, the developer can specify a different caption, or, if you need even more flexibility, use a property as a caption. This caption property can receive [upper](Form_structure.md#groupcolumns) objects of the displayed property as input. It is also worth noting that if [groups-in-columns](Form_structure.md#groupcolumns) are defined for the property, then it is desirable to have different captions for the created columns (in order to distinguish them somehow): in this case, it is recommended to use a property that receives all (!) objects of the defined group-in-columns as input.

In addition to the captions, you can define colors (both the background color and the text color) for each property view on a form, as well as a condition that needs to be met for the property to be displayed. Like the caption, each of these parameters is defined using some property.

### Filter group {#filtergroup}

In order to provide the user with an interface for choosing filters to apply, they can be combined into *filter groups*. For each of these groups, a special component will be created on the form: the user can use it to select one filter from the group as the current active filter. If several filters in one group are applied to different object groups, then the component will be displayed for the last of them.

The developer can specify a name for each filter group which can be used to access it in the future (for example, in form design).

### Custom filters/orders {#userfilters}

The user can change existing orders or add their own, as well as add their own filters using the corresponding interfaces:

-   Orders – by double-clicking on the column heading.
-   Filters – by using the corresponding button under the table for each object group. By default, the filter is set to the active property in the table, and filters it for equality to the entered value (for all types except case-insensitive string types, where the filter is set to include the entered string). If necessary, the developer can specify the default filtering type explicitly by using the corresponding option.

### Default objects selection {#defaultobject}

In the interactive form view, object group filters can change as a result of various user actions (for example, changing the upper objects of these filters, selecting filters in the filter group, etc.), after which the [current](Form_structure.md#currentObject) objects may no longer meet the conditions of the new filters. Also, when [a form is opened](Open_form.md), some objects may not be [passed](Open_form.md#params) or may be passed equal to `NULL`. In both of these cases, it is necessary to change the current objects, to some current *default objects*. The platform provides several options for selecting new current objects:

-   First (`FIRST`) - the first object collection (in accordance with the current order)
-   Last (`LAST`) – last object collection.
-   Previous (`PREV`) – the previous object collection (or as close to it as possible).
-   Undefined (`NULL`) – `NULL` values collection.

If none of these options is explicitly specified, the platform will try to determine whether the permanent filters in the group of objects are a) mutually exclusive for different values of the upper objects (if any), and/or b) the filter selects a very small percentage of the total number of objects of the specified classes. In both of these cases, it makes no sense to search for the previous object and, by default, the first object is selected (`FIRST`); in all other cases, the previous object (`PREV`).


:::info
It is worth noting that the selection of objects by default is pretty the same as the [object search](Search_SEEK.md) operation, where the search objects are:

-   for type `PREV`
    -   on opening a form: either the passed objects, or, if there are none, the last used objects for the form object class.
    -   in other cases: the previous current object values
-   for other types
    -   on opening the form - passed objects
    -   in other cases – an empty object collection

Search direction is determined by the object's default type (`PREV` here is equivalent to `FIRST`).
:::

### Object operators {#objectoperators}

When adding properties to a form, you can use a predefined set of operators that implement the most common scenarios for working with objects instead of using specific properties (thus avoiding the need to create and name these properties outside the form each time):

-   Object value (`VALUE`) – for a form object of [built-in class](Built-in_classes.md) , a special property with one argument will be added which displays the current object value and allows the user to change it. For [custom classes](User_classes.md), a property will be added which displays the object ID in the database; when you try to change it, it shows a dialog with a list of objects of that class. The selected value will be used as the current value of the object on the form.
-   Create object (`NEW`) – adds an action without arguments, which [creates](New_object_NEW.md) an object of the class of the passed form object (or the class explicitly specified by the developer), after which it automatically makes this object current. If the class has descendants, the user will be shown a dialog where he can select specific child class. If any filters are applied to the form object, for which the object is created, the system will try to [change](Property_change_CHANGE.md) the newly created object's properties so that it meets these filter conditions (as a rule, for created objects, a default value of the class of each filter's value is written to that filter)
-   Edit object (`EDIT`) – adds an action with one argument, which calls the `System.formEdit` action (which, in turn, open the default [edit form](#edtClass) for the edited object class). 
-   Create and edit an object (`NEWEDIT`) – adds an action without arguments which creates an object of the form object class, calls the edit object action (`EDIT`), and if the input is not [canceled](Value_input.md#result), sets the added object as current.
-   Delete object (`DELETE`) – adds an action with one argument which deletes the current object.

You can also specify options for the last four operators (ignored for all other actions):

-   [New Session](New_session_NEWSESSION_NESTEDSESSION.md) (`NEWSESSION`) – in this case, the action added to the form will be executed in a new session. When opening forms in a new session, it is important to remember that changes made in the current session (form) will not be visible. Thus, this mechanism is only recommended if the form is opened from a form in which the user cannot change anything, or if the properties and actions of the two forms do not intersect in any way. Note that when the operator is used to create a new object (`NEW`) in a new session, the object is not only created but also edited (`NEWEDIT`) (otherwise, the session would immediately close and your changes would be lost).
-   Nested Session (`NESTEDSESSION`) – the action will be executed in a new nested session. As with a new session, `NEW` is replaced by `NEWEDIT`.

### Selection/editing forms {#edtClass}

For each form, you can specify that it is the default form for viewing/editing objects of a given class. In this case, this form will be opened when you call actions created using the operators for object operations (create/edit an object). The same form will be opened when the corresponding  [form selection](Open_form.md#form) option is used in the form opening operator.

If list/edit form is not defined for a class, the platform will create one automatically. This form will consist of one object of the class, along with all properties matching the class and belonging to the `System.base` [property group](Groups_of_properties_and_actions.md). Also, actions of [creating, editing and deleting](#objectoperators) an object in a [new session](#objectoperators) will be automatically added to the form, along with the [object value](#objectoperators) property if there are no properties from the `System.id` property group corresponding to the class of the object (that is, no "ID" of the object has been added to the form).

### Session owner {#owner}

Since a form is opened by default in the current session, it may not always be safe to apply/cancel changes to this session: for example, the changes made in other forms may accidentally be applied. To avoid such situations, the platform has the concept of a *session owner* – a form which is responsible for managing the life cycle of the session (for example, applying / canceling changes). By default, it is considered that a form is the session owner if the session did not have any other owner when the form was [opened](In_an_interactive_view_SHOW_DIALOG.md).

To implement the mechanism for working with session owners the platform uses a numerical [local](Data_properties_DATA.md#local) property called `System.sessionOwners`. Accordingly, this property is incremented by `1` when you open a form and decremented by `1` when you close it. Thus, it shows the nesting depth of the "form opening stack", and is `NULL` if the session has no owner and not `NULL` otherwise.

If necessary, the developer can explicitly specify when opening a form that this form is the owner of the session that it uses.


:::info
Session ownership only affects the display / behavior of system actions for managing the life cycle of a form / session. When using the remaining actions, it is recommended that the developer should consider the risk of applying the "wrong" changes by himself (and, for example, use the mentioned above `System.sessionOwners` property).
:::

### System actions for form/session lifecycle management {#sysactions}

The following system actions are automatically added to any form (their names are specified in brackets):

-   Refresh (`System.formRefresh`) - updates the current state of the form, re-reading all the information from the database.
-   Save (`System.formApply`) - saves the changes made on the form to the database.
-   Cancel (`System.formCancel`) - cancels all changes made on the *form*.
-   OK (`System.formOk`) – closes the current form and, if the form is the session owner, applies the changes to the database.
-   Close (`System.formClose`) - closes the current *form* and does nothing with the changes.
-   Drop (`System.formDrop`) – closes the current form and returns `NULL` as the selected object.

By default, these system actions have the following visibility conditions:

|Action|Condition|
|---|---|
|Refresh|Always|
|Save, Cancel|If the form is the owner and actions that change the current session can be called on the form. Cancel may not be shown if the platform determines that canceling the changes is guaranteed to lead to a change of the [initial values](Open_form.md#params) of form objects (i.e., selecting other objects)|
|OK, Close|If the form was opened [synchronously](In_an_interactive_view_SHOW_DIALOG.md#flow)|
|Drop|If the form is opened synchronously, returns a value and allows `NULL` values to be passed|

If necessary, all these actions can be shown/hidden by removing the corresponding components from the [form design](Form_design.md) and/or using the corresponding options in the [open form](Open_form.md) operator.

### Additional features {#extra}

You can specify an image file which will be displayed as the form's icon.

Also, if necessary, you can enable *automatic update* mode for a form: the `System.formRefresh` action will then be executed for the form at a specified interval.

### Language

All of the above options, as well as defining the form structure, can be done using the [`FORM` statement](FORM_statement.md).

### Open form

To display the form in the interactive view, the corresponding [open form](Open_form.md) operator is used in [interactive view](In_an_interactive_view_SHOW_DIALOG.md).

### Examples

```lsf
date = DATA DATE (Order);
FORM showForm
    OBJECTS dateFrom = DATE, dateTo = DATE PANEL
    PROPERTIES VALUE(dateFrom), VALUE(dateTo)

    OBJECTS o = Order
    FILTERS date(o) >= dateFrom, date(o) <= dateTo
;

testShow ()  {
    SHOW showForm OBJECTS dateFrom = 2010_01_01, dateTo = 2010_12_31;

    NEWSESSION {
        NEW s = Sku {
            SHOW sku OBJECTS s = s FLOAT;
        }
    }
}
```

```lsf
FORM selectSku
    OBJECTS s = Sku
    PROPERTIES(s) id
;

testDialog  {
    DIALOG selectSku OBJECTS s INPUT DO {
        MESSAGE 'Selected sku : ' + id(s);
    }
}

sku = DATA Sku (OrderDetail);
idSku (OrderDetail d) = id(sku(d));

changeSku (OrderDetail d)  {
    DIALOG selectSku OBJECTS s = sku(d) CHANGE;

    //equivalent to the first option
    DIALOG selectSku OBJECTS s = sku(d) INPUT NULL CONSTRAINTFILTER DO {
        sku(d) <- s;
    }
}
```
