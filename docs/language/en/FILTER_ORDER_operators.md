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

- the singular operators (`ORDER`, `FILTER`, `FILTERGROUP`, `FILTER PROPERTY`) **apply** the value from the `FROM` block to the form element — as if the user had set that order or filter themselves;
- the plural operators (`ORDERS`, `FILTERS`, `FILTERGROUPS`, `FILTERS PROPERTY`) **read** the current value of the form element into the property from the `TO` block.

The form element is given by its name: `groupObjectId` — a group object (its orders or filters), `filterGroupId` — a filter group, `formPropertyId` — a form property (its filter).

The value is in a serialized form that depends on the element:

- for a group object's orders (`ORDER` / `ORDERS`) — `JSON`: a list of orders (the property name and the descending flag);
- for a group object's filters (`FILTER` / `FILTERS`) — `JSON`: a list of filter conditions (the property name, comparison, negation, value, and `OR` junction);
- for a filter group (`FILTERGROUP` / `FILTERGROUPS`) — `INTEGER`: the number of the active filter in the group;
- for a property's filter (`FILTER PROPERTY` / `FILTERS PROPERTY`) — `STRING`: the property's filter value.

If the `FROM` or `TO` block is omitted, the corresponding property of the [`UserEvents`](../paradigm/System_UserEvents.md) system module is used by default (`orders`, `filters`, `filterGroups`, `filtersProperty`), through which these operators are usually invoked.

### Parameters

- `groupObjectId`

    [Group object ID](IDs.md#groupobjectid) on the form.

- `filterGroupId`

    The name of a [filter group](../paradigm/Interactive_view.md#filtergroup) on the form, qualified by the form.

- `formPropertyId`

    [ID of a property or action on a form](IDs.md#formpropertyid).

- `expr`

    An [expression](Expression.md) whose value is applied to the form element. Its class determines the form of serialization.

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
// savedFilters gets JSON like [{"property": "number", "compare": ">", "value": 1000, "negation": false, "or": false}]
saveFilters ()  { FILTERS orders.o TO savedFilters; }
restoreFilters ()  { FILTER orders.o FROM savedFilters; }

// read the current order of group object o;
// currentOrders gets JSON like [{"property": "date", "desc": true}, {"property": "number", "desc": false}]
readOrders ()  { ORDERS orders.o TO currentOrders; }

// activate the second filter ("Small") in the filter group amount (zero-based)
showSmall ()  { FILTERGROUP orders.amount FROM 1; }

// filter the customer property by the value 'Acme'
filterAcme ()  { FILTER PROPERTY orders.customer FROM 'Acme'; }
```
