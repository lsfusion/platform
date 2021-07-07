---
title: 'Последовательность ({...})'
---

Для того, чтобы создать [действие](Actions.md), выполняющее последовательность других действий, используется [оператор `{...}`](Braces_operator.md) - блок, ограниченный фигурными скобками. В теле этого блока должна находиться последовательность [операторов-действий](Action_operators_paradigm.md) и объявлений локальных свойств.

### Язык

Для объявления действия, выполняющего последовательность других действий, используется [оператор `{...}`](Braces_operator.md). 

### Примеры

```lsf
CLASS Currency;
name = DATA STRING[30] (Currency);
code = DATA INTEGER (Currency);

CLASS Order;
currency = DATA Currency (Order);
customer = DATA STRING[100] (Order);
copy 'Копировать' (Order old)  {
    // создается действие, состоящее из последовательного выполнения двух действий
    NEW new = Order {                                   
        currency(new) <- currency(old); // точка с запятой указывается после каждого оператора                 
        customer(new) <- customer(old);
    } // в этой строке точка с запятой не ставится, потому что оператор заканчивается на }                                                   
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
