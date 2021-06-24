---
title: 'Sequence ({...})'
---

To create an [action](Actions.md) that performs a sequence of other actions, the [`{...}` operator](Braces_operator.md) is used - a block enclosed by curly brackets. The body of this block must contain a sequence of [action operators](Action_operators_paradigm.md) and local property declarations.

### Language

To declare an action that executes a sequence of other actions, use the [`{...}` operator](Braces_operator.md). 

### Examples

```lsf
CLASS Currency;
name = DATA STRING[30] (Currency);
code = DATA INTEGER (Currency);

CLASS Order;
currency = DATA Currency (Order);
customer = DATA STRING[100] (Order);
copy 'Copy' (Order old)  {
    NEW new = Order {                                   // an action is created that consists of the sequential execution of two actions
        currency(new) <- currency(old);                 // a semicolon is put after each statement
        customer(new) <- customer(old);
    }                                                   // there is no semicolon in this line, because the operator ends in }
}

loadDefaultCurrency(ISTRING[30] name, INTEGER code)  {
    NEW c = Currency {
        name(c) <- name;
        code(c) <- code;
    }
}
run ()  {
    loadDefaultCurrency('USD', 866);
    loadDefaultCurrency('EUR', 1251);
}
```
