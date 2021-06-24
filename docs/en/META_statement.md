---
title: 'META statement'
---

The `META` statement creates a new [metacode](Metaprogramming.md#metacode).

### Syntax

    META name(param1, ..., paramN)
        statement1
        ...
        statementM
    END

### Description

The `META` statement declares a new metacode and adds it to the current [module](Modules.md). 

The `META` statement is an exception - it is not supposed to end with a semicolon.  

### Parameters

- `name`

    Metacode name. [Simple ID](IDs.md#id). Must be unique within the current namespace among metacodes with the same number of parameters.

- `param1, ..., paramN`

    List of metacode parameters. Each parameter is defined by a simple ID. The list cannot be empty.

- `statement1 ... statementM`

    A sequence of  [statements](Statements.md) represented by a block of code. Statements may contain [special operators `##` and `###`](Metaprogramming.md#concat) used for concatenating [lexemes](Tokens.md). Statements cannot include another `META` statement.

### Examples

```lsf
META objectProperties(object, type, caption)
    object##Name 'Name'##caption = DATA BPSTRING[100](###object); // capitalizing the first letter
    object##Type 'Type'##caption = DATA type (###object);
    object##Value 'Cost'##caption = DATA INTEGER (###object);
END

META objectProperties(object, type)
    @objectProperties(object, type, '');
END
```
