---
title: 'ACTION+ statement'
---

The `ACTION+` statement adds an implementation to an [abstract action](Action_extension.md).

### Syntax

```lsf
[ACTION] abstractAction(param1, ..., paramN) +
    [WHEN conditionExpr THEN]
    { actionBody }
    [OPTIMISTICASYNC]
```

### Description

The `ACTION+` statement does not create a new action, but adds another implementation to an already declared [abstract action](Action_extension.md).

For an abstract action of type `CASE`, the `WHEN conditionExpr THEN` block is used. For abstract actions of types `MULTI` and `LIST`, the implementation is written without the `WHEN ... THEN` block.

### Parameters

- `ACTION`

    Optional keyword. Makes it explicit that an action is being extended.

- `abstractAction`

    [ID](IDs.md#propertyid) of the abstract action being extended.

- `param1, ..., paramN`

    List of [typed parameters](IDs.md#paramid) of the implementation being added. It defines its signature. The list may be empty. The number of parameters and their classes must be compatible with the signature of the abstract action. These parameters can be used in `actionBody` and, for the `CASE` form, in `conditionExpr`.

- `conditionExpr`

    [Expression](Expression.md) for the selection condition of this implementation. Used only for an abstract action of type `CASE`.

- `actionBody`

    Body of the added implementation: contents of the [`{...}` operator](Braces_operator.md), i.e. a sequence of [action operators](Action_operators.md) and, if necessary, `LOCAL` declarations. If the abstract action declares a result, the returned value and its parameters must be compatible with that result.

- `OPTIMISTICASYNC`

    Keyword that marks the implementation being added as optimistic asynchronous. Used only in forms where one implementation is selected from several.

### Examples

```lsf
CLASS ABSTRACT Animal;
whoAmI ABSTRACT (Animal);

CLASS Dog : Animal;
whoAmI(Dog d) + {
    MESSAGE 'I am a dog!';
}

CLASS Cat : Animal;
whoAmI(Cat c) + {
    MESSAGE 'I am a cat!';
}
```

```lsf
CLASS ABSTRACT Animal;
CLASS Dog : Animal;

notify(Animal a) ABSTRACT (Animal);
notify(Dog d) {
    MESSAGE 'Dog';
}

notify[Animal](Dog d) + {
    notify(d);
}
```

```lsf
CLASS Human;
name = DATA STRING[100] (Human);

testName ABSTRACT CASE (Human);

testName(Human h) + WHEN name(h) == 'John' THEN {
    MESSAGE 'I am John';
}
testName(Human h) + WHEN name(h) == 'Bob' THEN {
    MESSAGE 'I am Bob';
}
```

```lsf
onStarted ABSTRACT LIST ();

onStarted() + {
    MESSAGE 'Preparing data';
}
onStarted() + {
    MESSAGE 'Starting handlers';
}
```

```lsf
edit '{logics.edit}' ABSTRACT MULTI OVERRIDE FIRST (Object) TOOLBAR;

ACTION edit(Object o) + {
    SHOW EDIT Object = o DOCKED;
} OPTIMISTICASYNC
```
