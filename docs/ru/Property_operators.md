---
title: 'Операторы-свойства'
---

*Оператор-свойство* - это синтаксическая конструкция, описывающая [оператор](Property_operators_paradigm.md), создающий [свойство](Properties.md). 

Все операторы-свойства можно разделить на *контекстно-зависимые* и *контекстно-независимые*.

### Контекстно-зависимые операторы

Контекстно-зависимые операторы-свойства могут являться частью [выражений](Expression.md) и использовать общие параметры (контекст). Такими операторами являются:

-   [Арифметические операторы](Arithmetic_operators.md)
-   [Оператор `[]`](Brackets_operator.md)
-   [Оператор `ACTIVE TAB`](ACTIVE_TAB_operator.md)
-   [Оператор `AGGR`](AGGR_operator.md)
-   [Оператор `CASE`](CASE_operator.md)
-   [Оператор `CLASS`](CLASS_operator.md)
-   [Оператор `CONCAT`](CONCAT_operator.md)
-   [Оператор `EXCLUSIVE`](EXCLUSIVE_operator.md)
-   [Оператор `IF`](IF_operator.md)
-   [Оператор `IF ... THEN`](IF_..._THEN_operator.md)
-   [Оператор `JOIN`](JOIN_operator.md)
-   [Оператор `MAX`](MAX_operator.md)
-   [Оператор `MIN`](MIN_operator.md)
-   [Оператор `MULTI`](MULTI_operator.md)
-   [Оператор `OVERRIDE`](OVERRIDE_operator.md)
-   [Оператор `PARTITION`](PARTITION_operator.md)
-   [Оператор `GROUP`](GROUP_operator.md) (без блока `BY`)
-   [Оператор `PREV`](PREV_operator.md)
-   [Оператор `RECURSION`](RECURSION_operator.md)
-   [Оператор `STRUCT`](STRUCT_operator.md)
-   [Оператор `UNGROUP`](UNGROUP_operator.md)
-   [Оператор преобразования типа](Type_conversion_operator.md)
-   [Операторы `AND`, `OR`, `NOT`, `XOR`](AND_OR_NOT_XOR_operators.md)
-   [Операторы `IS`, `AS`](IS_AS_operators.md)
-   [Операторы изменений](Change_operators.md)
-   [Операторы сравнения](Comparison_operators.md)

### Контекстно-независимые операторы {#contextindependent}

Контекстно-независимые операторы-свойства отличаются от контекстно-зависимых тем, что сами определяют параметры (а не просто используют верхние). Как следствие, они не могут являться частью выражений, и могут быть использованы только в [инструкции `=`](=_statement.md) и [операторе `JOIN`](JOIN_operator.md): 

-   [Оператор `DATA`](DATA_operator.md)
-   [Оператор `GROUP`](GROUP_operator.md) (с блоком `BY`)
-   [Оператор `FORMULA`](FORMULA_operator.md)
-   [Оператор `ABSTRACT`](ABSTRACT_operator.md)
-   [Операторы групп объектов](Object_group_operator.md)
