---
title: 'Приоритет операторов'
---

При вычислении [выражения](Expression.md) [операторы](Property_operators_paradigm.md) вычисляются в определенном порядке в зависимости от *приоритета операторов*. Чем выше приоритет оператора, тем раньше он будет выполнен. В таблице ниже перечислены приоритеты всех операторов в порядке убывания.

|Оператор|Описание|Тип|
|---|---|---|
|`(expression)`<br/>[`JOIN`](JOIN_operator.md)<br/>[`CASE`](CASE_operator.md), [`MULTI`](MULTI_operator.md), [`OVERRIDE`](OVERRIDE_operator.md), [`EXCLUSIVE`](EXCLUSIVE_operator.md), [`IF ... THEN`](IF_..._THEN_operator.md)<br/>[`PARTITION`](PARTITION_operator.md)<br/>`RECURSION`<br/>`GROUP`<br/>[`STRUCT`](STRUCT_operator.md)<br/>[`MAX`](MAX_operator.md)/[`MIN`](MIN_operator.md)<br/>[`CONCAT`](CONCAT_operator.md)<br/>`INTEGER`, `DOUBLE`...<br/>[`PREV`](PREV_operator.md), [`CHANGED`, ...](Change_operators.md)<br/>[`CLASS`](Property_signature_CLASS.md)<br/>[`ACTIVE`](ACTIVE_TAB_operator.md)<br/>[`literal`](Literals.md)|Выражение в круглых скобках<br/>[Композиция](Composition_JOIN.md)<br/>[Выбор](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md)<br/>[Разбиение / упорядочивание](Partitioning_sorting_PARTITION_..._ORDER.md)<br/>[Рекурсия](Recursion_RECURSION.md)<br/>[Группировка](Grouping_GROUP.md)<br/>[Создание структуры](Structure_operators_STRUCT.md)<br/>Максимум / минимум<br/>Объединение строк<br/>[Преобразование типа](Type_conversion.md)<br/> <br/>[Сигнатура свойства](Property_signature_CLASS.md)<br/>[Активность](Activity_ACTIVE.md)<br/> [Константы](Constant.md)| <br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/>Префиксный<br/><br/>|
|[`[ ]`](Brackets_operator.md)<br/>[`IS`](IS_AS_operators.md)<br/>[`AS`](IS_AS_operators.md)|[Обращение к элементу структуры](Structure_operators_STRUCT.md)<br/>[Классификация](Classification_IS_AS.md)<br/>[Классификация](Classification_IS_AS.md)|Постфиксный<br/>Постфиксный<br/>Постфиксный|
|[`-`](Arithmetic_operators.md)|Унарный минус|Префиксный|
|[`*`](Arithmetic_operators.md)<br/>[`/`](Arithmetic_operators.md)|Умножение<br/>Деление|Бинарный<br/>Бинарный|
|[`+`](Arithmetic_operators.md)<br/>[`-`](Arithmetic_operators.md)|Сложение<br/>Вычитание|Бинарный<br/>Бинарный|
|[`(+)`](Arithmetic_operators.md)<br/>[`(-)`](Arithmetic_operators.md)|Сложение с учетом `NULL`<br/>Вычитание с учетом `NULL`|Бинарный<br/>Бинарный|
|[`<`](Comparison_operators.md)<br/>[`<=`](Comparison_operators.md)<br/>[`>`](Comparison_operators.md)<br/>[`>=`](Comparison_operators.md)|Меньше<br/>Меньше или равно<br/>Больше<br/>Больше или равно|Бинарный<br/>Бинарный<br/>Бинарный<br/>Бинарный|
|[`==`](Comparison_operators.md)<br/>[`!=`](Comparison_operators.md)|Равно<br/>Не равно|Бинарный<br/>Бинарный|
|[`NOT`](AND_OR_NOT_XOR_operators.md)|[Логическое отрицание](Logical_operators_AND_OR_NOT_XOR.md)|Префиксный|
|[`AND`](AND_OR_NOT_XOR_operators.md)|[Логическое И](Logical_operators_AND_OR_NOT_XOR.md)|Бинарный|
|[`XOR`](AND_OR_NOT_XOR_operators.md)|[Логическое исключающее ИЛИ](Logical_operators_AND_OR_NOT_XOR.md)|Бинарный|
|[`OR`](AND_OR_NOT_XOR_operators.md)|[Логическое ИЛИ](Logical_operators_AND_OR_NOT_XOR.md)|Бинарный|
|[`IF`](IF_operator.md)|Условие|Бинарный|
