---
title: 'Property extension'
---

The [properties](Properties.md) [extension](Extensions.md) technique allows an *abstract property* to be declared in one [module](Modules.md) and its implementations to be defined in other modules. This is deferred construction of a property computed as the corresponding [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md): the base module defines the form of the future operator and the requirements for its implementations, while other modules gradually add individual implementations.

An abstract property defines the extension contract: the result class determines the returned value, and the parameter classes determine the allowed implementations.

Property extension allows:

-   Implement the concept of property polymorphism by analogy with certain object-oriented programming languages.
-   Reduce dependencies between modules by adding specific "entry points" for later changes to property calculation.

### Implementation choice

Which implementation is considered suitable depends on the kind of abstract property:

-   In the explicit conditional form, each implementation has its own selection condition.
-   In the signature-based polymorphic form, the implementation is selected by compatibility of the current argument classes with its signature.
-   In the value-based form, an implementation is suitable if it returns a defined value.

If several implementations are suitable at the same time, the final result is determined by the mutual exclusion mode and the implementation order.

### Implementation order

An abstract property stores implementations in an ordered list. New implementations can be added to either end of the list.

This affects behavior as follows:

-   If the abstract property allows several simultaneously applicable implementations, the result is returned by the first applicable implementation in that list.
-   In the mutually exclusive mode, there must be exactly one applicable implementation for a given set of arguments.

So implementation order is part of the extension contract, not just a technical detail.

### Completeness of implementations {#full}

An abstract property may require the entire admissible domain of parameter values to be covered by implementations. In that case, for all descendants of the parameter classes there must be at least one applicable implementation, and in the [mutually exclusive](#exclusive) mode there must be exactly one.

This makes the extension contract stronger: when more specific cases are added, the corresponding implementations must be added as well.

### Implementation contract

Each implementation of an abstract property has its own parameter signature and its own result class. The platform matches them against the contract of the abstract property.

An implementation must not narrow the signature of the abstract property. In particular, in the explicit conditional form this usually means that all parameters must participate either in the selection condition or in the result calculation. Otherwise such an implementation will have a narrower signature and will not be able to extend the abstract property correctly.

### Polymorphic form {#poly}

As in a [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#poly), an abstract property also has a *polymorphic form*: one property defines both the selection condition and the corresponding result. In this form, the condition can be either matching the [signature](Property_signature_ISCLASS.md) of that property or the property itself.

### Mutual exclusion of conditions {#exclusive}

As in a [selection operator](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive), you can specify that all conditions of an abstract property must be *mutually exclusive*. In this mode, for each set of arguments there must be at most one applicable implementation.

### Language

This technique uses two language constructs: the [`ABSTRACT` operator](ABSTRACT_operator.md) for declaring an abstract property and the [`+=` statement](+=_statement.md) for adding implementations.

### Examples


```lsf
CLASS Invoice;
CLASS InvoiceDetail;
CLASS Range;

rateChargeExchange(invoice) = ABSTRACT NUMERIC[14,6] (Invoice);
             
backgroundSku 'Color' (d) = ABSTRACT CASE FULL COLOR (InvoiceDetail);
 
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
name(CClass c) += name(c); 
name(DClass d) += 'DClass' + innerName(d) IF d IS DClass;
```
