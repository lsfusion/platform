---
slug: "/Brief_extensions"
title: 'Brief: extensions'
---

## Extending classes

The [`EXTEND CLASS` statement](../language/EXTEND_CLASS_statement.md) inherits an already declared class from further parent classes and adds new [static objects](../paradigm/Static_objects.md) to it.

Structurally this moves the relations between classes into a separate module: the base module declares the class, and a module depending on it adds a parent, changing nothing in the base one. The mechanism is described in [class extension](../paradigm/Class_extension.md).

```lsf
CLASS Box : Shape;
CLASS Quadrilateral;
EXTEND CLASS Box : Quadrilateral;   // adding inheritance

EXTEND CLASS ShapeType {            // adding a static object
    circle 'Circle'
}
```

## Extending properties and actions

An abstract property or action is a declared extension point: the base module sets the contract with the `ABSTRACT` operator — the parameter classes, the way an implementation is chosen, and the result class, which a property always has and an action only when the operator names one — and other modules add implementations with the [`+=` statement](../language/plus_equals_statement.md) for properties and the [`ACTION+` statement](../language/ACTION_plus_statement.md) for actions. The kinds of selection and the options are covered in [Brief: properties](Brief_properties.md).

Structurally this is the deferred assembly of an ordinary operator: for a property, of a [selection operator](../paradigm/Property_extension.md); for an action, of a [branching or sequence operator](../paradigm/Action_extension.md). The implementations are kept in an ordered list, and a new one is added at its start or at its end.

Hence the main modularity technique: the base module declares the extension point, the modules depending on it add behaviour, and no reverse dependency appears.

## Extending forms

The [`EXTEND FORM` statement](../language/EXTEND_FORM_statement.md) extends a form declared in another module — with the same blocks as the [`FORM`](../language/FORM_statement.md) declaration (see [Brief: forms](Brief_forms.md)): objects, the properties and actions shown for them, filters, orders. Separate extension blocks change the elements already on the form; the form's [design](../paradigm/Form_design.md) is likewise set from outside.

An added element can be placed before or after a specific element of the form, or at the start or at the end; for objects this position sets their place in the order of object groups, which a property's display group and the object group a filter applies to depend on. The mechanism is described in [form extension](../paradigm/Form_extension.md).

```lsf
EXTEND FORM items
    PROPERTIES(i) NEWSESSION DELETE     // a delete button
    OBJECTS g = ItemGroup BEFORE i      // the item group before the item
    PROPERTIES(g) READONLY name
    FILTERS itemGroup(i) = g
;
```

There is no separate extension logic for the navigator and the form design: these constructs are extensible by definition ([extensions](../paradigm/Extensions.md)).
