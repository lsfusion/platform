---
title: 'Оператор MULTI'
---

Оператор `MULTI` - создание [свойства](Properties.md), реализующего [выбор](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#single) одного из значений (полиморфная форма).

### Синтаксис

    MULTI expr1, ..., exprN [exclusionType]

### Описание

Оператор `MULTI` создает свойство, значением которого будет значение одного из указанных в операторе свойств. Условием выбора свойства является принадлежность параметров [сигнатуре](CLASS_operator.md) этого свойства. 

### Параметры

- `expr1, ..., exprN` 

    Список [выражений](Expression.md), определяющих свойства, из которых будет производиться выбор.

- `exclusionType`

    [Тип взаимоисключения](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md#exclusive). Определяет, могут ли несколько условий выбора свойства одновременно выполняться при некотором наборе наборе параметров. Задается одним из ключевых слов:

    - `EXCLUSIVE`
    - `OVERRIDE`

  Тип `EXCLUSIVE` указывает на то, что условия выбора свойства не могут одновременно выполняться. Тип `OVERRIDE` допускает несколько одновременно выполняющихся условий, в этом случае будет выбрано первое в списке свойство с выполняющимся условием выбора. 

    Тип `EXCLUSIVE` используется по умолчанию.

### Примеры

```lsf
nameMulti (Human h) = MULTI 'Male' IF h IS Male, 'Female' IF h IS Female;

CLASS Ledger;
CLASS InLedger : Ledger;
quantity = DATA INTEGER (InLedger);

CLASS OutLedger : Ledger;
quantity = DATA INTEGER (OutLedger);

signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
```
