---
title: 'Property extension'
---

The [properties](Properties.md) [extension](Extensions.md) technique allows the developer to declare an *abstract action* in one [module](Modules.md) and define its implementation in other modules. This technique is essentially a "postponed definition" of a [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md), where the operator’s title is defined when the property is declared, and as new functionality (of [classes](Classes.md) or [static objects](Static_objects.md)) is added, selection options are added to the system. Furthermore, variants of selection (if it is not mutually exclusive) can be added both to the beginning and to the end of the abstract property created.

For abstract properties, the expected classes of parameters must be specified. Then the platform will automatically check that the implementations added match these classes. Also, if necessary, you can check that for all descendants of the parameter classes at least one implementation is specified (or exactly one, if the conditions are [mutually exclusive](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md)).

Extension of properties allows you to:

-   Implement the concept of property polymorphism by analogy with certain object-oriented programming languages.
-   Remove dependency between modules by adding specific "entry points" to change the way properties are calculated.

### Polymorphic form {#poly}

Just as [for a selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#poly), for an abstract property there is a *polymorphic form* where the selection condition and the result corresponding to it are set by a single property. Accordingly, as in a selection operator, either belonging to the [signature ](Property_signature_CLASS.md)of this property or the property itself can be a condition.

### Mutual exclusion of conditions {#exclusive}

As [for a selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive), you can specify that all conditions of an abstract property must be *mutually exclusive*. If this option is set, and the conditions are not in fact mutually exclusive, the platform will throw the corresponding error.

It is worth noting that this check is no more than a hint to the platform (for better optimization), and also a kind of self-checking on the part of the developer. However, in many cases it allows you to make the code more transparent and readable (especially given a polymorphic form of the abstract property).

### Language

The key features that implement the extension procedure are the [`ABSTRACT` operator](ABSTRACT_operator.md),for declaring an abstract action, and the [`+=` statement](+=_statement.md), for defining its implementation.

### Example


```lsf
CLASS Invoice;
CLASS InvoiceDetail;
CLASS Range;

// ABSTRACT MULTI EXCLUSIVE is created
rateChargeExchange(invoice) = ABSTRACT NUMERIC[14,6] (Invoice);
             
// ABSTRACT CASE OVERRIDE LAST is created, and if there are several suitable implementations, 
// the first of them will be calculated
backgroundSku 'Color' (d) = ABSTRACT CASE FULL COLOR (InvoiceDetail);
 
// The last matching implementation will be calculated here
overVAT = ABSTRACT VALUE OVERRIDE FIRST Range (InvoiceDetail);          
```

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
// Here name[AClass] will be found on the left, because the search goes only among abstract properties, 
// and on the right name[CClass] will be found
name(CClass c) += name(c); 
name(DClass d) += 'DClass' + innerName(d) IF d IS DClass;
```
