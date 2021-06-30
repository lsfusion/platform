---
title: 'Modules'
---

A *module* is a functionally complete part of a [project](Projects.md). A module consists of declarations of [properties](Properties.md), [actions](Actions.md), [events](Events.md), [constraints](Constraints.md), and other [system elements](Naming.md).

Each module has a name, which must be unique within the [project](Projects.md).

### Dependencies between modules {#depends}

Usually modules use elements from other modules to describe part of their functionality. Accordingly, if module `B` uses elements from module `A`, it must be specified in module `B` that it *depends* on `A`. Based on these dependencies, all modules in the project are arranged in a certain order in which they are initialized. It is guaranteed that if module `B` depends on module `A`, module `A` will be initialized before module `B`. Circular dependencies between project modules are not allowed.  

If module `C` depends on module `B`, and module `B` depends on module `A`, we will also assume that module `C` depends on module `A`.

Any module always automatically depends on the system module [`System`](https://github.com/lsfusion/platform/blob/master/server/src/main/lsfusion/system/System.lsf), regardless of whether or not this is specified explicitly.

### Namespaces

In each module a [namespace](Naming.md#namespace) is specified, containing the names of all system elements created in this module. By default, the module creates its own namespace, the name of which is equal to the name of the module. For the module you can also specify a list of additional namespaces that will have priority when [finding](Search_.md) [system elements](Element_identification.md).

### Language

Each module in the platform corresponds to exactly one file, which starts with a special [header](Module_header.md).

### Examples

```lsf
MODULE EmployeeExample;	 	// Defining the module name

REQUIRE System, Utils;	 	// Listing the modules that the Employee module depends on
NAMESPACE Employee;		 	// Setting the namespace

CLASS Employee 'Employee';	// Creating a class
CLASS Position 'Position'; // Creating another class

employeePosition(employee) = DATA Position (Employee); // Creating property
```
