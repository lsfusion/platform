---
title: 'Оператор INPUT'
---

Оператор `INPUT` - создание [действия](Actions.md), осуществляющего [ввод примитива](Primitive_input_INPUT.md).

### Синтаксис

    INPUT inputOptions 
    [CHANGE [= changeExpr]]
    [DO actionOperator [ELSE elseActionOperator]]

`inputOptions` - опции ввода. Задаются одним из следующих синтаксисов:

    [alias =] builtInClassName
    [alias] = expr

### Описание

Оператор `INPUT` создает действие, которое позволяет запрашивать у пользователя значение одного из [встроенных классов](Built-in_classes.md).

### Параметры

- `builtInClassName`

    Имя одного из [встроенных классов](Built-in_classes.md). 

- `expr`

    [Выражение](Expression.md), значение которого определяет [начальное значение](Value_input.md#initial) ввода.

- `alias`

    Имя локального параметра, в который будет записан результат ввода. [Простой идентификатор](IDs.md#id).

- `CHANGE`

    Ключевое слово, которое обозначает, что кроме ввода значения, полученный результат также необходимо записать в указанное свойство.

- `changeExpr`

    [Выражение](Expression.md), которое определяет свойство, в которое будет записан результат ввода. По умолчанию, для этого используется свойство, указанное в качестве начального значения ввода.

- `actionOperator`

    [Контекстно-зависимый оператор-действие](Action_operators.md), выполняется, если ввод был успешно завершен.

- `elseActionOperator`

    [Контекстно-зависимый оператор-действие](Action_operators.md), выполняется, если ввод был отменен. В качестве параметров нельзя использовать параметр результата ввода.

### Примеры

```lsf
changeCustomer (Order o)  {
    INPUT s = STRING[100] DO {
        customer(o) <- s;
        IF s THEN
            MESSAGE 'Customer changed to ' + s;
        ELSE
            MESSAGE 'Customer dropped';
    }
}

FORM order
    OBJECTS o = Order
    PROPERTIES(o) customer ON CHANGE changeCustomer(o)
;

testFile  {
    INPUT f = FILE DO { // запрашиваем диалог по выбору файла
        open(f); // открываем выбранный файл
    }
}
```
