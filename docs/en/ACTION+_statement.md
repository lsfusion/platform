---
title: 'ACTION+ statement'
---

The `ACTION+` statement adds an implementation (branching condition) to an [abstract action](Action_extension.md).

### Syntax

    [ACTION] actionId(param1, ..., paramN) + { implAction }
    [ACTION] actionId(param1, ..., paramN) + WHEN whenExpr THEN { implAction }

### Description

The `ACTION+` statement adds an implementation to an abstract action. The syntax for adding an implementation depends on the type of abstract action. If the abstract action is of type `CASE`, then the implementation should be described using `WHEN ... THEN ...` otherwise, the implementation should be described simply as an action. 

### Parameters

- `actionId`  

    [ID](IDs.md#propertyid) of the abstract action. 

- `param1, ..., paramN`

    List of parameters that will be used to define the implementation. Each element is a [typed parameter](IDs.md#paramid). The number of these parameters must be equal to the number of parameters of the abstract action. These parameters can then be used in the implementation operator of the abstract property and in the selection condition expression of this implementation.

- `implAction`

    [Context-dependent action operator](Action_operators.md#contextdependent) whose value determines the implementation of the abstract action. 

- `whenExpr`

    An [expression](Expression.md) whose value determines the selection condition of the implementation of an abstract property (action) that has type `CASE`. 

### Examples

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
