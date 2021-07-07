---
title: 'Оператор IF ... THEN'
---

Оператор `IF ... THEN` - создание [свойства](Properties.md), реализующего [выбор](Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md) с одним условием (одиночная форма).

### Синтаксис

    IF condition 
        THEN value
        [ELSE alternativeValue]

### Описание

Оператор `IF ... THEN `создает свойство, которое реализует условный выбор. Условие задается с помощью свойства. Если это условие выполняется, то есть значение свойства не равняется `NULL`, то значением создаваемого свойства будет являться значение свойства указанного в блоке `THEN`, иначе значением будет являться значение свойства в блоке `ELSE` либо `NULL`, если блок `ELSE` не задан.

### Параметры

- `condition`

    [Выражение](Expression.md), задающее условие. 

- `value`

    Выражение, значение которого будет являться значением создаваемого свойства, если условие выполняется.

- `alternativeValue`

    Выражение, значение которого будет являться значением создаваемого свойства, если условие не выполняется.

### Примеры

```lsf
price1 = DATA NUMERIC[10,2] (Book);
price2 = DATA NUMERIC[10,2] (Book);
maxPrice (Book b) = IF price1(b) > price2(b) THEN price1(b) ELSE price2(b);

// если h будет другого класса, то будет NULL
sex (Human h) = IF h IS Male THEN 'Male' ELSE ('Female' IF h IS Female); 

isDifferent(a, b) = IF a != b THEN TRUE;
```
