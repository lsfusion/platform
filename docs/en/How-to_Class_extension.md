---
title: 'How-to: Class extension'
---

The typical scheme for extracting relations between classes to a separate module is as follows:

Create the `MA` module in which the `A` class will be created:

```lsf
MODULE MA;

CLASS ABSTRACT A; // declaring an abstract class
a = ABSTRACT BPSTRING[10] (A); // declaring an abstract property a
```

Create the `MB` module in which the `B` class will be created:

```lsf
MODULE MB;

CLASS B; // declaring class B
b = DATA BPSTRING[10] (B); // declaring the data property b for class B
```

Create the `MBA` module in which the relation between the `A` and `B` class will be defined:

```lsf
MODULE MBA;

// specifying that the MBA module depends on the MA and MB modules so 
// that the system elements declared in them can be used in that module
REQUIRE MA, MB; 

EXTEND CLASS B : A; // inheriting class B from A
// specifying that for the abstract property a, property B should be used as an implementation
a(ba) += b(ba); 
```

Therefore, the `MA` and `MB` modules do not directly depend on each other and the relation between them can be enabled (disabled) by linking (unlinking) the `MBA` module. Note that the `MBA` module extends the functionality of the `MB` module without any changes to its code.

You can use mixin classes when using the metacode as follows:

Suppose that we have a metacode that declares a class and defines certain properties for it:

```lsf
MODULE MyModule;

META defineMyClass (className) // declaring the defineMyClass metacode with the className parameter
    CLASS className; // declaring a class named className
    // adding a property named myProperty + className for the created class
    myProperty###className = DATA BPSTRING[20] (className); 
END
```

Note that when calling this metacode, you cannot specify the classes from which the created class must inherit anything. However, this can be implemented through a mixin of classes as follows:

```lsf
CLASS MySuperClass;

@defineMyClass(MyClass); // calling the metacode that will create the class and property

// inheriting MyClass from MySuperClass, while MyClass will "will receive"
// all the properties that are declared for the MySuperClass class
EXTEND CLASS MyClass : MySuperClass; 
```
