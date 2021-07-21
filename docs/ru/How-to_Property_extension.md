---
title: 'How-to: Расширение свойств'
---

Классический подход для реализации полиморфизма может выглядеть следующим образом:

Создаем абстрактный класс `Shape` с абстрактным свойством `square`:

```lsf
CLASS ABSTRACT Shape;
square 'Площадь' = ABSTRACT DOUBLE (Shape);
```

Создаем классы `Rectangle` и `Circle`, который наследуется от `Shape`:

```lsf
CLASS Rectangle : Shape;
width 'Ширина' = DATA DOUBLE (Rectangle);
height 'Высота' = DATA DOUBLE (Rectangle);

CLASS Circle : Shape;
radius 'Радиус окружности' = DATA DOUBLE (Circle);
```

Определяем реализацию абстрактного свойства `square` для созданных классов:

```lsf
square(rectangle) += width(rectangle) * height(rectangle);
square(circle) += radius(circle) * radius(circle) * 3.14;
```

Предположим, необходимо сделать таким образом, чтобы в определенных случаях можно было переопределить способ расчета площади для класса `Circle`. В таком случае, в строке с определением реализации площади для окружности можно вставить своеобразную "точку входа" в виде абстрактного свойства, реализацию которого можно изменить в другом модуле:

```lsf
overSquareCircle 'Перегруженная площадь' = ABSTRACT DOUBLE (Circle);
square(circle) += OVERRIDE overSquareCircle(circle), (radius(circle) * radius(circle) * 3.14);
```

Если ни в одном модуле свойство `overSquareCircle` не будет реализовано, то его значение всегда будет равно `NULL` и будет использоваться базовый механизм расчета площади. Для изменения же расчета можно в некотором модуле `MyShape` задать иную реализацию, которая и будет использоваться:

```lsf
MODULE MyShape;

REQUIRE Shape;

// используем формулу с более высокой точностью
overSquareCircle (circle) += radius(circle) * radius(circle) * 3.14159265359; 
```

Следует отметить, что вместо [оператора `OVERRIDE`](OVERRIDE_operator.md) можно использовать любые другие выражения. В частности, наиболее часто используемыми могут быть [операторы `(+)` и `(-)`](Arithmetic_operators_+_-_etc.md).
