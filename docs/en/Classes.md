---
title: 'Classes'
sidebar_label: Overview
---

The base element in **lsFusion** is the object. Each object is an *instance* of some *class*, which determines the qualities of all its instances. Classes in turn can be divided into [built-in classes](Built-in_classes.md), which are responsible for primitive data types, and [user classes](User_classes.md). 

### Inheritance

Classes can *inherit* from each other. When class `B` inherits from class `A`, class `A` shall be called the *parent*, and class `B` shall be called the *child*.

The idea of inheritance is as follows: if class `B` inherits from class `A`, then all instances of class `B` will have all the qualities of class `A`. Thus, with inheritance, each class determines the qualities not only of all instances of this class but also of all instances of this class descendants.  

Let's say that an object *belongs to* class `A` if that object is either an instance of class `A` or an instance of a class `A` descendant.  

To implement polymorphism, inheritance is usually used together with [properties](Property_extension.md) and [actions](Action_extension.md) extension mechanism. 

### Class limitations

The class mechanism has several limitations:

1.  Belonging to a class cannot be calculated (only set explicitly when [creating](New_object_NEW.md) and [changing the class](Class_change_CHANGECLASS_DELETE.md)) of an object.
2.  A class is set for only one object (not for an object collection).
3.  It is not possible to inherit the same class multiple times.

Accordingly, if the class mechanism is not sufficient, the platform also supports the [aggregation](Aggregations.md) mechanism, which together with inheritance allows implementing almost any polymorphic logic.
