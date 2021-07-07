---
title: 'Action extension'
---

The [actions](Actions.md) [extension](Extensions.md) technique allows the developer to declare an abstract action in one [module](Modules.md) and add to it an implementation in other modules. This technique is essentially a "postponed definition" of a [branch operator](Branching_CASE_IF_MULTI.md), where the operator’s title is defined when the property is declared, and branching conditions are added as new functionality (of [classes](Classes.md) or [static objects](Static_objects.md)) is added to the system. Furthermore, branching conditions (if branching is not mutually exclusive) can be added both to the beginning and to the end of the abstract action created. Similarly, this technique works with a [sequence operator](Sequence.md).

For abstract actions, the expected classes of parameters must be specified. Then the platform will automatically check that the added implementations match these classes. Also, if necessary, you can check that for all descendants of the parameter classes at least one implementation is specified (or exactly one, if the conditions are [mutually exclusive](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md)).

Actions extension allows:

-   Implement the concept of action polymorphism by analogy with certain object-oriented programming languages.
-   Remove dependency between modules by adding specific "entry points," to which new behavior can be added later.

### Polymorphic form {#poly}

As with a branching operator, for an abstract action there is a *polymorphic form*, where it is possible not to define a condition explicitly, but to use as a condition matching the [signature](Property_signature_CLASS.md) of the action that corresponds to this condition.

### Mutual exclusion of conditions {#exclusive}

As for a branch operator, you can specify that all conditions of an abstract action must be *mutually exclusive*. If this option is specified, and the conditions are not in fact mutually exclusive, the platform will throw the corresponding error.

It is worth noting that this check is no more than a hint to the platform (for better optimization), and also a kind of self-checking on the part of the developer. However, in many cases it allows to make the code more transparent and readable (especially in a polymorphic form of the abstract action).

### Language

The key features that implement the extension technique are the [`ABSTRACT` operator](ABSTRACT_action_operator.md), for declaring an abstract action, and the [`ACTION+` statement](ACTION+_statement.md), for adding an implementation to it.

### Examples

```lsf
exportXls 'Export to Excel' ABSTRACT CASE (Order); // ABSTRACT CASE OVERRIDE LAST is created        
exportXls (Order o) + WHEN name(currency(o)) == 'USD' THEN {
    MESSAGE 'Export USD not implemented';
}

CLASS Task;
run 'Execute' ABSTRACT (Task); // ABSTRACT MULTI EXCLUSIVE

CLASS Task1 : Task;
name = DATA STRING[100] (Task);
run (Task1 t) + {
    MESSAGE 'Run Task1 ' + name(t);
}


CLASS OrderDetail;
price = DATA NUMERIC[14,2] (OrderDetail);

CLASS InvoiceDetail;
price = DATA NUMERIC[14,2] (InvoiceDetail);
fill ABSTRACT LIST (OrderDetail, InvoiceDetail); // ABSTRACT LIST LAST

fill (OrderDetail od, InvoiceDetail id) + {
    price(id) <- price(od);
}
```


```lsf
CLASS ABSTRACT Animal;
whoAmI  ABSTRACT ( Animal);

CLASS Dog : Animal;
whoAmI (Dog d) + {  MESSAGE 'I am a dog!'; }

CLASS Cat : Animal;
whoAmI (Cat c) + {  MESSAGE 'I am a сat!'; }

ask ()  {
    FOR Animal a IS Animal DO
        whoAmI(a); // a corresponding message will be shown for each object
}

onStarted  ABSTRACT LIST ( );
onStarted () + {
    name(Sku s) <- '1';
}
onStarted () + {
    name(Sku s) <- '2';
}
// first, the 1st action is executed, then the 2nd action

CLASS Human;
name = DATA STRING[100] (Human);

testName  ABSTRACT CASE ( Human);

testName (Human h) + WHEN name(h) == 'John' THEN {  MESSAGE 'I am John'; }
testName (Human h) + WHEN name(h) == 'Bob' THEN {  MESSAGE 'I am Bob'; }
```

  
