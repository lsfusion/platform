---
title: 'Инструкция ACTION+'
---

Инструкция `ACTION+` - добавление реализации (условия ветвления) к [абстрактному действию](Action_extension.md).

### Синтаксис

    [ACTION] actionId(param1, ..., paramN) + { implAction }
    [ACTION] actionId(param1, ..., paramN) + WHEN whenExpr THEN { implAction }

### Описание

Инструкция `ACTION+` добавляет реализацию к абстрактному действию. Синтаксис добавления реализации зависит от типа абстрактного действия. Если абстрактное действие имеет тип `CASE`, то реализация должна описываться в виде `WHEN ... THEN ...`, в ином случае реализация должна быть описана просто в виде действия. 

### Параметры

- `actionId`

    [Идентификатор](IDs.md#propertyid) абстрактного действия. 

- `param1, ..., paramN`

    Список параметров, которые будут использованы при описании реализации. Каждый элемент является [типизированным параметром](IDs.md#paramid). Количество этих параметров должно совпадать с количеством параметров абстрактного действия. Эти параметры далее могут быть использованы в операторе реализации абстрактного свойства и выражении условия выбора этой реализации.

- `implAction`

    [Контекстно-зависимый оператор-действие](Action_operators.md), значение которого определяет реализацию абстрактного действия. 

- `whenExpr`

    [Выражение](Expression.md), значение которого определяет условие выбора реализации для абстрактного действия типа `CASE`. 

### Примеры

```lsf
CLASS ABSTRACT Animal;
whoAmI  ABSTRACT ( Animal);

CLASS Dog : Animal;
whoAmI (Dog d) + {  MESSAGE 'I am a dog!'; }

CLASS Cat : Animal;
whoAmI (Cat c) + {  MESSAGE 'I am a сat!'; }

ask ()  {
    FOR Animal a IS Animal DO
        whoAmI(a); // для каждого объекта будет выдано соответствующее сообщение
}

onStarted  ABSTRACT LIST ( );
onStarted () + {
    name(Sku s) <- '1';
}
onStarted () + {
    name(Sku s) <- '2';
}
// сначала выполниться 1е действие, потом 2е действие

CLASS Human;
name = DATA STRING[100] (Human);

testName  ABSTRACT CASE ( Human);

testName (Human h) + WHEN name(h) == 'John' THEN {  MESSAGE 'I am John'; }
testName (Human h) + WHEN name(h) == 'Bob' THEN {  MESSAGE 'I am Bob'; }
```
