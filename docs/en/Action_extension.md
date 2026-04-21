---
title: 'Action extension'
---

The [actions](Actions.md) [extension](Extensions.md) technique allows an abstract action to be declared in one [module](Modules.md) and implementations to be added to it in other modules. This is deferred construction of the corresponding [branch operator](Branching_CASE_IF_MULTI.md) or [sequence operator](Sequence.md): the base module defines the form of the future operator and the requirements for its implementations, while other modules gradually add individual implementations.

An abstract action defines the extension contract: parameter classes determine the allowed implementations, and if necessary the action can also declare the result class and the classes of its parameters.

Action extension allows:

-   Implement the concept of action polymorphism by analogy with certain object-oriented programming languages.
-   Reduce dependencies between modules by adding specific "entry points" for later behavior additions.

### Implementation selection

Which implementation is executed depends on the form of the abstract action:

-   In the explicit conditional form, each implementation has its own selection condition.
-   In the signature-based polymorphic form, the implementation is selected by compatibility of the current argument classes with its signature.
-   In the sequential form, there is no single selected implementation: all added implementations are executed.

If several implementations are applicable at the same time in a form that selects a single implementation, the resulting behavior is determined by the mutual exclusion mode and the implementation order.

### Implementation order

An abstract action stores implementations in an ordered list. New implementations can be added to either end of the list.

This affects behavior as follows:

-   If the abstract action allows several simultaneously applicable implementations, the first applicable implementation in that list is executed.
-   In the sequential form, all implementations are executed in the order of that list.
-   In the mutually exclusive mode, there must be exactly one applicable implementation for a given set of arguments.

So implementation order is part of the extension contract, not just a technical detail.

### Completeness of implementations {#full}

An abstract action may require the entire admissible domain of parameter values to be covered by implementations. In that case, for all descendants of the parameter classes there must be at least one applicable implementation, and in the [mutually exclusive](#exclusive) mode there must be exactly one.

This makes the extension contract stronger: when more specific cases are added, the corresponding implementations must be added as well.

### Implementation contract

Each implementation of an abstract action has its own parameter signature. The platform matches this signature against the contract of the abstract action.

An implementation should not implicitly narrow the applicability of the abstract action. Otherwise, the implementation signature no longer matches the contract defined by the abstract action.

If the abstract action declares a result, the returned value and its parameters must match that result.

### Asynchronous implementation marker

In forms that select one implementation from several alternatives, an implementation can have an additional optimistic-asynchronous marker.

If an abstract action has at least one implementation with this marker, those implementations are used as the basis for its asynchronous behavior.

### Polymorphic form {#poly}

As in the [branch operator](Branching_CASE_IF_MULTI.md#poly), an abstract action also has a *polymorphic form*: the condition can be omitted and replaced with [matching the signature](Property_signature_ISCLASS.md) of the corresponding action.

### Mutual exclusion of conditions {#exclusive}

As in the [branch operator](Branching_CASE_IF_MULTI.md#exclusive), you can specify that all conditions of an abstract action must be *mutually exclusive*. In this mode, for each set of arguments there must be at most one applicable implementation.

### Language

This technique uses two language constructs: the [`ABSTRACT` operator](ABSTRACT_action_operator.md) for declaring an abstract action and the [`ACTION+` statement](ACTION+_statement.md) for adding implementations.

### Examples

```lsf
exportXls 'Export to Excel' ABSTRACT CASE OVERRIDE LAST (Order);
exportXls (Order o) + WHEN name(currency(o)) == 'USD' THEN {
    MESSAGE 'Export USD not implemented';
}

CLASS ABSTRACT Task;
run 'Execute' ABSTRACT MULTI EXCLUSIVE FULL (Task);

CLASS Task1 : Task;
name = DATA STRING[100] (Task);
run (Task1 t) + {
    MESSAGE 'Run Task1 ' + name(t);
}
```


```lsf
CLASS ABSTRACT Animal;
whoAmI  ABSTRACT ( Animal);

CLASS Dog : Animal;
whoAmI (Dog d) + {  MESSAGE 'I am a dog!'; }

CLASS Cat : Animal;
whoAmI (Cat c) + {  MESSAGE 'I am a cat!'; }

ask ()  {
    FOR Animal a IS Animal DO
        whoAmI(a);
}

CLASS Sku;
name = DATA STRING[100] (Sku);
onStarted  ABSTRACT LIST ();
onStarted () + {
    name(Sku s) <- '1';
}
onStarted () + {
    name(Sku s) <- '2';
}

CLASS Human;
name = DATA STRING[100] (Human);

testName  ABSTRACT CASE ( Human);

testName (Human h) + WHEN name(h) == 'John' THEN {  MESSAGE 'I am John'; }
testName (Human h) + WHEN name(h) == 'Bob' THEN {  MESSAGE 'I am Bob'; }

CLASS Issue;
CLASS Language;
localizedTitle = DATA STRING[100] (Issue, Language);

getLocalizedTitle(Issue issue) ABSTRACT STRING[100] (Language);
getLocalizedTitle (Issue issue) + {
    FOR Language l IS Language DO
        RETURN localizedTitle(issue, l);
}
```

  
