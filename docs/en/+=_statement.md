---
title: '+= statement'
---

The `+=` statement adds an implementation (selection option) to an [abstract property](Property_extension.md).

### Syntax

    propertyId (param1, ..., paramN) += implExpr;
    propertyId (param1, ..., paramN) += WHEN whenExpr THEN implExpr;

### Description

The `+=` statement adds an implementation to an abstract property. The syntax for adding an implementation depends on the type of abstract property. If the abstract property is of type `CASE`, then the implementation should be described using `WHEN ... THEN ...` otherwise, the implementation should be described simply as a property. 

### Parameters

- `propertyId`

    [ID](IDs.md#propertyid) of the abstract property. 

- `param1, ..., paramN`

    List of parameters that will be used to define the implementation. Each element is a [typed parameter](IDs.md#paramid). The number of these parameters must be equal to the number of parameters of the abstract property. These parameters can then be used in expressions of the implementation of the abstract property and the selection condition of this implementation.

- `implExpr`

    [Expression](Expression.md) whose value determines the implementation of an abstract property.

- `whenExpr`

    An expression whose value determines the selection condition of the implementation of an abstract property (action) that has type `CASE`. 

### Examples

```lsf
CLASS ABSTRACT AClass;
CLASS BClass : AClass;
CLASS CClass : AClass;
CLASS DClass : AClass;

name(AClass a) = ABSTRACT BPSTRING[50] (AClass);
innerName(BClass b) = DATA BPSTRING[50] (BClass);
innerName(CClass c) = DATA BPSTRING[50] (CClass);
innerName(DClass d) = DATA BPSTRING[50] (DClass);

name(BClass b) = 'B' + innerName(b);
name(CClass c) = 'C' + innerName(c);

name[AClass](BClass b) += name(b);
name(CClass c) += name(c); // Here name[AClass] will be found on the left, because the search goes only among abstract properties, and on the right name[CClass] will be found
name(DClass d) += 'DClass' + innerName(d) IF d IS DClass;
```

