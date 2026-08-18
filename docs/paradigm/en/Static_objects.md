---
slug: "/Static_objects"
title: 'Static objects'
---

*Static* (or built-in) objects are objects that are defined by the developer and are automatically created on system startup (if they are not present in the system at launch time). Also, such objects are prohibited from being deleted.

When declaring a [custom class](User_classes.md), you can declare objects of this class which will be static objects. If you do this, this custom class automatically [inherits](User_classes.md#inheritance) from class `System.StaticObject`.

For each static object of a custom class name and caption must be specified, and an image can also be specified. Later this name, caption, and image can be accessed using the [properties](Properties.md) `name[StaticObject]`, `caption[StaticObject]`, and `image[StaticObject]` respectively. `name[StaticObject]` returns the object's *canonical* name — its identifier qualified with the module namespace and class, in the form `<namespace>_<Class>.<object>` (for the `Direction.north` object below, `name[StaticObject]` yields `<namespace>_Direction.north`, not the bare `north`) — while `caption[StaticObject]` returns the caption shown to the user. The short unqualified name (the part after the dot) is returned by the `basicName[StaticObject]` property from the `Utils` system module.

Static objects of [built-in classes](Built-in_classes.md) are numbers, strings, date values, etc., used by the user in describing the logic.

Static objects can be used to create a limited set of objects of a certain class. Such a set can be used as an enumerated data type to provide a choice from a limited set of values. To show such an enumerated value on a form and let the user change it, what is displayed is not the link to the static object itself but its composition with `caption[StaticObject]`: the [interactive view](Interactive_view.md#select), under the conditions described there, shows such a composition as a selection element over the objects of the class, and a write through it goes into the link, not into the caption of the static object.

### Language

Static objects of custom classes are defined in the [`CLASS` statement](../language/CLASS_statement.md) in a block enclosed in braces.

### Examples

```lsf
CLASS Direction 'Direction'
{
    north 'North',
    east 'East',
    south 'South',
    west 'West'
}

direction = DATA Direction ();

showDirection  {
    MESSAGE name(direction());
    MESSAGE caption(direction());
}

// creating a form by choosing an object of Direction class
FORM directions 'Directions'
    OBJECTS d = Direction
    PROPERTIES(d) READONLY caption

    LIST Direction OBJECT d
;

// choosing a direction on a form: the caption composition is displayed,
// a write through it goes into the direction property
FORM windRose 'Wind rose'
    PROPERTIES captionDirection 'Direction' = caption(direction())
;
```

