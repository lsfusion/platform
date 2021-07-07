---
title: 'Metaprogramming'
---

*Metaprogramming* is a type of programming associated with writing software code that results in the generation of more software code. Metaprogramming is used for code reusability and to speed up development.  

### Metacode {#metacode}

In **lsFusion** the metaprogramming tool used is *metacode*, which is described by the [`META` statement](META_statement.md). Metacode consists of a header and an **lsFusion** code block describing the [statement](Statements.md) sequence. This code block must end with the keyword `END`. Let us consider an example of metacode that allows you to add two [actions](Actions.md) to an arbitrary [form](Forms.md):

```lsf
META addActions(formName)
    EXTEND FORM formName
        PROPERTIES() showMessage, closeForm
    ;
END
```

The first line of the example contains the metacode header. It consists of the keyword `META`, metacode name, and parameter list. In this example, the metacode `addActions` has one parameter: `formName`. This is the name of the form to which the actions will be added. Let's consider the possible uses for this metacode, which are described by the [`@` statement](commat_statement.md). 

```lsf
@addActions(documentForm);
@addActions(orderForm);
```

The statement to use metacode starts with the special symbol `@`, followed by the name of the metacode and the parameters passed. When generating the code, each metacode parameter is replaced by the value passed as a parameter of the `@` statement in all places where the metacode parameter is used. In this example, the metacode parameter `formName` will be replaced with `documentForm` and `orderForm`. The above metacode uses generate the following code block:

```lsf
EXTEND FORM documentForm
    PROPERTIES() showMessage, closeForm
;

EXTEND FORM orderForm
    PROPERTIES() showMessage, closeForm
;
```

### Lexeme concatenation  {#concat}

Simply substituting an ID for a metacode parameter is often not enough. For example, when creating a large number of new [system elements](Element_identification.md) inside the metacode, you must be able to specify these new names. Passing all the names as metacode parameters can be inconvenient. For this reason the metacode contains the special operation `##`, which operates at the [tokens](Tokens.md) level. This operation can concatenate two adjacent lexemes into one. If one of the concatenated lexemes is a [string literal](Literals.md#strliteral), the concatenation will result in a single string literal.

```lsf
META objectProperties(object, caption)
    object##Name 'Name '##caption = DATA BPSTRING[100](object);
    object##Type 'Type '##caption = DATA Type (object);
    object##Value 'Cost '##caption = DATA INTEGER (object);
END

@objectProperties(Document, 'of the document');
```

Using the metacode `objectProperties` produces the following code:

```lsf
DocumentName 'Document name' = DATA BPSTRING[100](Document);
DocumentType 'Document type' = DATA Type (Document);
DocumentValue 'Document cost' = DATA INTEGER (Document);
```

There is also the special operation `###`. It is equivalent to operation `##`, except that in the second of the concatenated literals, the first character, if a letter, is converted to uppercase.

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
