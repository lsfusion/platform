---
slug: "/plus_equals_statement"
title: '+= statement'
---

The `+=` statement adds an implementation to an [abstract property](../paradigm/Property_extension.md).

### Syntax

```lsf
abstractProperty(param1, ..., paramN) +=
    [WHEN conditionExpr THEN]
    implementationExpr;
```

### Description

The `+=` statement does not create a new property. It adds another implementation to an already declared abstract property.

For an abstract property of type `CASE`, the `WHEN conditionExpr THEN` block is required. For abstract properties of types `MULTI` and `VALUE`, the `WHEN ... THEN` block is not used and the implementation expression appears directly after `+=`.

The `+=` statement applies to properties only. An implementation of an abstract action is added by the [`ACTION+` statement](ACTION_plus_statement.md): an action body, not an expression, is written after the `+` sign.

The position of the added implementation in the abstract property's [implementation list](../paradigm/Property_extension.md#poly) is determined by the abstract property's `OVERRIDE FIRST` / `OVERRIDE LAST` setting; see the [`ABSTRACT` operator](ABSTRACT_operator.md) for the available modes.

For an abstract property in the [mutually exclusive](../paradigm/Property_extension.md#exclusive) mode (the default for `MULTI`) the platform checks at module finalization, when the server starts, that the conditions of the implementations do not intersect; for the `MULTI` form these are the signatures of the implementations, and an intersection stops the start with the error `signature intersection of property ... for abstract property ...`. In particular, an abstract property without parameters in this mode admits at most one implementation: a second `+=`, even from another module, is such an intersection. A property meant to take several simultaneously applicable implementations, or a default one and an override, is declared in the `OVERRIDE` mode, which is written after the form: `ABSTRACT VALUE` has it by default and applies the implementation added later when it returns a value, falling back to the earlier one; `ABSTRACT MULTI OVERRIDE` applies the more specific signature and, for equal signatures, the implementation added later.

### Parameters

- `abstractProperty`

    [ID](IDs.md#propertyid) of the abstract property being extended.

- `param1, ..., paramN`

    List of [typed parameters](IDs.md#paramid) of the implementation being added; defines its signature. The list may be empty. The number of parameters and their classes must be compatible with the signature of the abstract property. These parameters can be used in `implementationExpr` and, for the `CASE` form, in `conditionExpr`.

- `conditionExpr`

    [Expression](Expression.md) for the selection condition of this implementation. Used only for an abstract property of type `CASE`.

- `implementationExpr`

    Expression for the implementation. Its result class must be compatible with the result class of the abstract property.

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
