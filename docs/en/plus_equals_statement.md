---
title: '+= statement'
---

The `+=` statement adds an implementation to an [abstract property](Property_extension.md).

### Syntax

```lsf
abstractProperty(param1, ..., paramN) +=
    [WHEN conditionExpr THEN]
    implementationExpr;
```

### Description

The `+=` statement does not create a new property, but adds another implementation to an already declared [abstract property](Property_extension.md).

For an abstract property of type `CASE`, the `WHEN conditionExpr THEN implementationExpr` form is used. For abstract properties of types `MULTI` and `VALUE`, the implementation is written without the `WHEN ... THEN` block.

### Parameters

- `abstractProperty`

    [ID](IDs.md#propertyid) of the abstract property being extended.

- `param1, ..., paramN`

    List of [typed parameters](IDs.md#paramid) of the implementation being added. It defines its signature. The list may be empty. The number of parameters and their classes must be compatible with the signature of the abstract property. These parameters can be used in `implementationExpr` and, for the `CASE` form, in `conditionExpr`.

- `implementationExpr`

    [Expression](Expression.md) for the property implementation. Its result class must be compatible with the result class of the abstract property.

- `conditionExpr`

    [Expression](Expression.md) for the selection condition of this implementation. Used only for an abstract property of type `CASE`.

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
name(CClass c) += name(c); 
name(DClass d) += 'DClass' + innerName(d) IF d IS DClass;
```

```lsf
CLASS Person;
CLASS PersonDocumentType;
name = DATA ISTRING[64] (PersonDocumentType);

caption = ABSTRACT CASE ISTRING[100] (Person, PersonDocumentType);

caption(Person p, PersonDocumentType t) +=
    WHEN p IS Person AND name(t) == 'Passport' THEN 'Passport';
```
