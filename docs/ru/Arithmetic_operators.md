---
title: 'Арифметические операторы'
---

Операторы `+`, `-`, `*`, `/`, `(+)`, `(-)` - создание [свойств](Properties.md), реализующих [арифметические операции](Arithmetic_operators_+_-_etc.md).

### Синтаксис

    expression1 + expression2  
    expression1 - expression2  
    expression1 / expression2  
    expression1 * expression2  
    -expression1
    expression1 (+) expression2  
    expression1 (-) expression2  

### Параметры

- `expression1, expression2`

    [Выражения](Expression.md), значения которых будут являться аргументами арифметических операторов.

### Примеры

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
```
