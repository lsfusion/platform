---
slug: "/FILTER_ORDER_operators"
title: 'Form filter and order operators'
---

The `ORDER`, `FILTER`, `FILTERGROUP`, `FILTER PROPERTY` operators (and their variants `ORDERS`, `FILTERS`, `FILTERGROUPS`, `FILTERS PROPERTY`) create [actions](../paradigm/Actions.md) that apply or read the current user filters and orders of the elements of an open form.

### Syntax

```
ORDER            groupObjectId   [FROM expr]
FILTER           groupObjectId   [FROM expr]
FILTERGROUP      filterGroupId   [FROM expr]
FILTER PROPERTY  formPropertyId  [FROM expr]

ORDERS           groupObjectId   [TO propId]
FILTERS          groupObjectId   [TO propId]
FILTERGROUPS     filterGroupId   [TO propId]
FILTERS PROPERTY formPropertyId  [TO propId]
```

### Description

The operators work with the [interactive view](../paradigm/Interactive_view.md) of an open form and fall into two groups:

- the singular operators (`ORDER`, `FILTER`, `FILTERGROUP`, `FILTER PROPERTY`) **apply** the value from the `FROM` block to the form element — the same orders and filters the user can set on it;
- the plural operators (`ORDERS`, `FILTERS`, `FILTERGROUPS`, `FILTERS PROPERTY`) **read** the current value of the form element into the property from the `TO` block.

A singular operator applies the value at once: a plural operator later in the same action reads the new value, and the form shows it together with its other changes. It does not trigger the [form events](../paradigm/Form_events.md) `ORDERS`, `FILTERS`, `FILTERGROUPS` and `FILTERS PROPERTY`, which occur when the user changes the orders or filters; the `ORDER` and `FILTER` events of a group object occur on any change of them, this one included.

The form element is given by its name: `groupObjectId` — a group object (its orders or filters), `filterGroupId` — a filter group, `formPropertyId` — a form property (its filter).

The value is in a serialized form that depends on the element:

- for a group object's orders (`ORDER` / `ORDERS`) — `JSON`: a list of orders (the property name and the descending flag);
- for a group object's filters (`FILTER` / `FILTERS`) — `JSON`: a list of filter conditions (the property name, comparison, negation, value, and `OR` junction);
- for a filter group (`FILTERGROUP` / `FILTERGROUPS`) — `INTEGER`: the number of the active filter in the group, counting from 1, or 0 when no filter is active, in a `NONULL` group too; a number naming no filter of the group changes nothing;
- for a property's filter (`FILTER PROPERTY` / `FILTERS PROPERTY`) — `STRING`: the property's filter value.

`ORDER` and `FILTER` skip an item that names a property of another group object or a property with the `COLUMNS` option, since an item names no column. A filter condition with no comparison gets the default comparison of the property.

If the `FROM` or `TO` block is omitted, the corresponding property of the [`UserEvents`](../paradigm/System_UserEvents.md) system module is used by default (`orders`, `filters`, `filterGroups`, `filtersProperty`), through which these operators are usually invoked.

### Parameters

- `groupObjectId`

    [Group object ID](IDs.md#groupobjectid) on the form.

- `filterGroupId`

    The name of a [filter group](../paradigm/Interactive_view.md#filtergroup) on the form, qualified by the form.

- `formPropertyId`

    [ID of a property or action on a form](IDs.md#formpropertyid).

- `expr`

    An [expression](Expression.md) whose value is applied to the form element. Its class determines the form of serialization (see Description). In a plain string literal curly braces delimit a [localization](String_literal.md#localization) identifier, so `JSON` given by a literal is written as a raw string literal `r'...'`, which keeps them.

- `propId`

    [ID of a property](IDs.md#propertyid) without parameters that the read value is written to.

### Examples

```lsf
FORM orders
    OBJECTS o = Order
    PROPERTIES(o) number, date, customer

    FILTERGROUP amount
        FILTER 'Large' number(o) > 1000
        FILTER 'Small' number(o) <= 1000
;

savedFilters = DATA JSON ();
currentOrders = DATA JSON ();

// save the current filters of group object o and re-apply them later;
// savedFilters gets JSON like [{"property": "number", "compare": ">", "negation": false, "value": "1000"}]
saveFilters ()  { FILTERS orders.o TO savedFilters; }
restoreFilters ()  { FILTER orders.o FROM savedFilters; }

// read the current order of group object o;
// currentOrders gets JSON like [{"property": "date", "desc": true}, {"property": "number", "desc": false}]
readOrders ()  { ORDERS orders.o TO currentOrders; }

// order group object o by date, latest first: JSON given by a literal is a raw literal
sortByDate ()  { ORDER orders.o FROM JSON(r'[{"property": "date", "desc": true}]'); }

// activate the second filter ("Small") in the filter group amount
showSmall ()  { FILTERGROUP orders.amount FROM 2; }

// filter the customer property by the value 'Acme'
filterAcme ()  { FILTER PROPERTY orders.customer FROM 'Acme'; }
```
