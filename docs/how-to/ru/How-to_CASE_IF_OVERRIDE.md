---
slug: "/How-to_CASE_IF_OVERRIDE"
title: 'How-to: CASE/IF/OVERRIDE'
---

## Пример 1

### Условие

Есть набор книг, которые могут быть белыми и черными.

```lsf
CLASS Color 'Цвет' {
    white 'Белый',
    black 'Черный'
}

CLASS Book 'Книга';

color 'Цвет' = DATA Color (Book);
```

Нужно определить свойство, которое возвращает цвет книги.

### Решение

```lsf
// Вариант 1
nameColor1 'Цвет' (Book b) = caption(color(b));

// Вариант 2
nameColor2 'Цвет' (Book b) = IF color(b) == Color.white THEN 'Белый' ELSE 'Черный';
```

В данном случае эти два варианта идентичны.

## Пример 2

### Условие

Есть заказы на книги поставщикам. Для каждого из них определено, был ли он отправлен поставщику, согласован и поставлен. В примере они введены как [первичные](../paradigm/Data_properties_DATA.md) свойства, но в более сложных случаях они будут вычисляемыми.

```lsf
CLASS Order 'Заказ';

sent 'Отправлен' = DATA BOOLEAN (Order);
agreed 'Согласован' = DATA BOOLEAN (Order);
accepted 'Принят' = DATA BOOLEAN (Order);
```

Необходимо определить статус заказа.

### Решение

```lsf
// Вариант 1
nameStatus1 'Статус' (Order o) = CASE WHEN accepted(o) THEN 'Принят'
                                     WHEN agreed(o) THEN 'Согласован'
                                     WHEN sent(o) THEN 'Отправлен'
                                ELSE 'Новый';

// Вариант 2
CLASS Status 'Статус' {
    new 'Новый',
    sent 'Отправлен',
    agreed 'Согласован',
    accepted 'Принят'
}

status 'Статус' (Order o) = CASE WHEN accepted(o) THEN Status.accepted
                                 WHEN agreed(o) THEN Status.agreed
                                 WHEN sent(o) THEN Status.sent
                            ELSE Status.new;
nameStatus2 'Статус' (Order o) = caption(status(o));
```

## Пример 3

### Условие

Есть набор книг, аналогично [**Примеру 1**](#пример-1).

Нужно задать для книги торговую надбавку, но чтобы можно было задать значение по умолчанию.

### Решение

```lsf
dataMarkup 'Надбавка, %' = DATA NUMERIC[6,2] (Book);

defaultMarkup 'Надбавка по умолчанию' = DATA NUMERIC[6,2] ();

markup1 'Надбавка, %' (Book b) = OVERRIDE dataMarkup(b), defaultMarkup();

// Эквивалентно :
markup2 'Надбавка, %' (Book b) = IF dataMarkup(b) THEN dataMarkup(b) ELSE defaultMarkup();
```

## Пример 4

### Условие

Аналогичен [**Примеру 3**](#пример-3), только для книги задана категория.

```lsf
CLASS Category 'Категория';

category 'Категория' = DATA Category (Book);
```

Нужно задать для книги торговую надбавку, но чтобы можно было задать значение по умолчанию для категории, к которой она относится.

### Решение

```lsf
markup 'Надбавка, %' = DATA NUMERIC[6,2] (Category);

markup 'Надбавка, %' (Book b) = OVERRIDE dataMarkup(b), markup(category(b));
```

## Пример 5

### Условие

Есть набор книг, для каждой из которых задан номер.

```lsf
number 'Номер' = DATA INTEGER (Book);
```

Нужно найти номер, следующий за максимальным.

### Решение

```lsf
freeNumber1 () = (GROUP MAX number(Book b)) (+) 1;

// Эквивалентно:
freeNumber2() = (OVERRIDE (GROUP MAX number(Book b)), 0) + 1;
```

Оператор `(+)` используется вместо обычного оператора `+`, так как если не будет ни одной книги, то обычное сложение с единицей вернет `NULL`.

## Пример 6

### Условие

Документы относятся к периоду одной из двух гранулярностей — кварталу или месяцу; у каждого документа задан ровно один из двух порядковых номеров.

```lsf
CLASS Doc 'Документ';

quarterOrd 'Номер квартала' = DATA INTEGER (Doc);
monthOrd 'Номер месяца' = DATA INTEGER (Doc);

ord 'Номер периода' (Doc d) = OVERRIDE quarterOrd(d), monthOrd(d);
```

Нужно для каждого документа посчитать документы той же гранулярности, период которых не позже его собственного.

### Решение

```lsf
lePeriod (Doc a, Doc b) = quarterOrd(a) <= quarterOrd(b) OR monthOrd(a) <= monthOrd(b);

notLaterCount 'Не позже' (Doc d) = GROUP SUM 1 IF lePeriod(Doc dd, d);
```

Прямолинейное условие `ord(dd) <= ord(d)` сравнивает друг с другом два многоветочных выбора: при компиляции разворачивается произведение их ветвей, поэтому с ростом числа гранулярностей и тяжести свойств-ветвей текст запроса растёт мультипликативно и может превысить максимальную длину запроса. Поветочное сравнение сравнивает каждую гранулярность только с самой собой: в смешанной паре один из операндов каждого сравнения равен `NULL`, и пара не учитывается — здесь это ровно требуемая семантика. Так переписать сравнение можно, только когда значения разных ветвей не должны сравниваться между собой; в противном случае храните составной номер, пометив свойство [`MATERIALIZED`](../paradigm/Materializations.md).
