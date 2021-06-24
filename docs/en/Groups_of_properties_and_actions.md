---
title: 'Groups of properties and actions'
---


:::info
The behavior of properties and actions in the context of working with groups is absolutely identical, therefore, in the rest of this section, we will use only the term property (the behavior for actions is completely similar).
:::

All [properties](Properties.md) in the system are divided into *property groups*. In this case, all groups form a hierarchy in which each group can contain both properties and other property groups. The root group in this hierarchy is the `System.root` group. All other groups are contained in some *parent group*.

Each property or action belongs directly to exactly one group, and also belongs to all the ancestors of that group. So, for example, all properties and actions in the system belong to the `System.root` group.

### Builtin property groups {#builtin}

In addition to `root` in the `System` module, the following groups of properties and actions are created automatically:

-   `root`
    -   `public`
        -   `base`
            -   `id`
    -   `private`


:::info
All these groups (including `root`) are not used in form display mechanisms (i.e. containers in the [default form design](Form_design.md#defaultDesign) and elements of the [hierarchical](Structured_view.md#hierarchy) import / export hierarchy are not created for them).
:::

### Property groups usage

Property groups are currently used in the following mechanisms:

-   When displaying forms:
    -   in the [interactive](Interactive_view.md) view: you can specify for each group that a separate container must be created in the [default design](Form_design.md#defaultDesign). Accordingly, the hierarchy of containers in the default design will match the hierarchy of these property groups. That is, if it is specified for a group (or property) `A` and property group `B` that a container must be created, and group (or property) `A` is a descendant of group `B`, then the container of group (component of property) `A` will be the descendant of the container of group `B`.
    -   in the [hierarchical](Structured_view.md#hierarchy) view :you can specify for each group that a separate intermediate tag must be created when the form is exported. Accordingly, the hierarchy of tags during export will match the hierarchy of property groups. That is, if it is specified for a group (or property) `A` and property group `B` that a tag needs to be created, and group (or property) `A` is a descendant of group `B`, then group (property) `A` will be enclosed in the tag of group `B`. 
-   When automatically creating forms:
    -   All properties with one argument which are included in the `System.base` group will be displayed in the automatically generated dialog forms for [selecting/editing](Interactive_view.md#edtClass) objects. 
    -   All properties with one argument which are included in the `System.id` group will be displayed in automatically generated [message display forms](Constraints.md#message) on constraint violation.
-   In the security policy:
    -   You can set a security policy for a group as a whole (and not for each property individually).

### Language

To add a new property/action group to the system, use the [`GROUP` statement](GROUP_statement.md).

```lsf
GROUP base : root; // The caption of this group will be 'base'
GROUP local 'Local properties'; // The parent group of local will be System.private
```

  
