---
title: 'Data export (EXPORT)'
---

The *data export* operator creates an [action](Actions.md) that reads values of the specified properties for all object collections where the value of the specified property (*condition*) is not `NULL`, and then saves these values to a file in a specified [format](Structured_view.md). 

The condition is optional in this operator and, if not defined, it is considered equal to the [disjunction](Logical_operators_AND_OR_NOT_XOR.md) of all exported properties (i.e., at least one of the properties must be non-`NULL`). 

Similar to the other [set operations](Set_operations.md), the condition must be such that the operation is [correct](Set_operations.md).

### General case

It should be noted that data export is a special case of (syntactic sugar for) [form export](In_a_structured_view_EXPORT_IMPORT.md), in which the exported form is created automatically, named `export` and consists of:

-   one [object group](Form_structure.md#objects) named `value` whose objects correspond to exported property parameters (not created if all exported property parameters are fixed values).
-   exported properties. The [built-in](Groups_of_properties_and_actions.md#builtin) `System.private` group is used as the [property group](Form_structure.md#propertygroup) for the created properties on the form, and the created object group is used as the [display group](Form_structure.md#drawgroup). If there is only one exported property and it does not have a name, the corresponding property on the form is created with the name `value`.
-   a filter equal to the defined condition.
-   defined orders.

Thus, the behavior of the data export operator (for example, determining the names of the resulting columns/keys, [processing `value`](Structured_view.md#value), etc.) is completely determined by the behavior of the form export operator (as if the above form was passed to it as a parameter).

### Language

To declare an action that exports data, use the [`EXPORT` operator](EXPORT_operator.md).

### Examples


```lsf
CLASS Store;

name = DATA STRING[20] (Sku);
weight = DATA NUMERIC[10,2] (Sku);

in = DATA BOOLEAN (Store, Sku);

exportSkus (Store store)  {
    EXPORT DBF CHARSET 'CP866' FROM id(Sku s), name(s), weight(s) WHERE in(store, s); // uploading to DBF all Sku for which in (Store, Sku) is specified for the desired warehouse
    EXPORT CSV NOHEADER NOESCAPE FROM id(Sku s), name(s), weight(s) WHERE in(store, s); // uploads to CSV without header line and escaping special characters
    EXPORT FROM id(Sku s), name(s), weight(s) WHERE in(store, s) ORDER name(s) DESC; // uploads JSON, sorting by property name[Sku] in descending order
    EXPORT FROM ff='HI'; // uploads JSON {"ff":"HI"}, as by default it gets the name value, and the platform gets the object {"value":"HI"} to
    EXPORT FROM 'HI'; // uploads JSON "HI", as by default it gets the name value, and the platform automatically converts the object {"value": "HI"} to "HI"
}
```

  
