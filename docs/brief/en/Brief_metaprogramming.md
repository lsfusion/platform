---
slug: "/Brief_metaprogramming"
title: 'Brief: metaprogramming'
---

## META and calling metacode (@)

*Metacode* is a block of lsFusion code with parameters that produces other code when used. It is declared by the [`META` statement](../language/META_statement.md) — the name, the parameter list, a sequence of statements, the closing `END` — and used by the [`@` statement](../language/commat_statement.md), which names the metacode and passes the arguments: every metacode parameter is replaced with the argument passed in all the places it is used.

Besides substitution, the metacode has operations on tokens: `##` concatenates two adjacent tokens into one, and `###` does the same while converting the first character of the second token to uppercase — except when everything to its left came out empty and the token itself does not start with `###`, where the capitalization is skipped, so `prefix###name` with an empty `prefix` yields `name`, not `Name`. They are what the names of the generated elements are built from. The mechanism is described in [metaprogramming](../paradigm/Metaprogramming.md).

**Analogy**: a macro with textual substitution of parameters.

```lsf
META objectProperties(object, caption)
    object##Name 'Name of '##caption = DATA BPSTRING[100](object);
    object##Value 'Value of '##caption = DATA INTEGER (object);
END

@objectProperties(Document, 'the document');   // DocumentName 'Name of the document' = ...
```

## Generated code and call arguments

The body of a metacode consists of module-level [statements](../language/Statements.md), and the `@` statement itself is written at module level: a metacode produces declarations of system elements, not operators inside an action body.

Hence the two typical uses: a family of same-shaped declarations for a class or a prefix passed in, and a set of elements added to an arbitrary form through `EXTEND FORM` with the form name as a parameter ([Brief: extensions](Brief_extensions.md)).

An argument of the call can be a composite ID, a class identifier, a literal, or an empty parameter — but not the value of a property: choosing behaviour by data is done not with a metacode but with abstract properties and actions.

The IDE writes the produced code after the `@` call in braces so that navigation and analysis work on it; the platform ignores that block and generates the code anew, so it must not be edited by hand.
