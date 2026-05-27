---
slug: "/Arithmetic_operators"
title: 'Арифметические операторы'
---

Операторы `+`, `-`, `*`, `/`, `(+)`, `(-)` создают [свойства](../paradigm/Properties.md), реализующие [арифметические операции](../paradigm/Arithmetic_operators_plus_minus_etc.md).

### Синтаксис

```
expression1 + expression2
expression1 - expression2
expression1 * expression2
expression1 / expression2
expression1 (+) expression2
expression1 (-) expression2
- expression1
```

### Описание

Каждый бинарный оператор принимает два операнда и выполняется слева направо; унарный минус принимает один операнд. Порядок вычисления относительно других операторов определяется [приоритетом операторов](Operator_priority.md).

### Параметры

- `expression1, expression2`

    [Выражения](Expression.md), значения которых будут являться аргументами арифметических операторов.

### Примеры

```lsf
sum(a, b) = a + b;
transform(a, b, c) = -a * (b (+) c);
```
