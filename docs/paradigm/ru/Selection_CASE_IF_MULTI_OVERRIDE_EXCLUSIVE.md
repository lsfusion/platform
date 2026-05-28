---
slug: "/Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE"
title: 'Выбор (CASE, IF, MULTI, OVERRIDE, EXCLUSIVE)'
---

Оператор *выбора* создает свойство, которое для набора *условий* определяет, какое из этих условий выполняется, и возвращает значение соответствующего этому условию *результата*. Если выполняется сразу несколько условий, результатом будет результат, соответствующий первому из них в порядке перечисления. Если ни одно из условий не выполняется, то значением создаваемого свойства будет `NULL`. 

Все условия и результаты задаются как некоторые свойства и/или параметры. Соответственно, условие *выполняется*, если значение свойства или параметра, с помощью которого задается это условие, не равно `NULL`. 

### Полиморфная форма {#poly}

Также в платформе существует возможность задавать условие и соответствующий ему результат одним свойством. В этом случае условием может являться либо принадлежность сигнатуре этого свойства, либо само это свойство. Такую форму оператора выбора будем называть *полиморфной*.


:::info
Отметим то, что [оператор экстремума](Extremum_MAX_MIN.md) и [логические операторы](Logical_operators_AND_OR_NOT_XOR.md) также по сути являются разновидностями оператора выбора (причем его полиморфной формой, то есть условия и результат определяются одним свойством)
:::

### Взаимоисключаемость условий {#exclusive}

Для оператора выбора можно указать, что все его условия должны быть *взаимоисключающими* — для любого набора аргументов выполняется не более одного из них, поэтому результат уже не зависит от их порядка.

### Неявное задание

У этого оператора есть возможность [неявного задания](Property_extension.md), при котором его условия и результаты добавляются по частям в разных модулях. Когда такой неявно заданный оператор дополнительно объявлен взаимоисключающим, его условия во всех модулях не должны пересекаться; платформа проверяет это на этапе финализации модуля. Для оператора, заданного непосредственно, то же требование принимается на доверии.

### Одиночная форма {#single}

Оператор выбора в *одиночной* форме проверяет ровно одно условие. Если это условие выполняется, возвращается значение указанного результата. Также в этой форме существует возможность указать *альтернативный результат*, значение которого будет возвращаться, если условие не выполняется.

Опция взаимоисключения и неявное задание к одиночной форме не применимы.

### Класс результата

Результирующий класс — общий предок ([встроенный](Built-in_classes.md#commonparentclass) или [пользовательский](User_classes.md#commonparentclass)) его возможных результатов.

### Язык

Для создания свойства, реализующего общую форму выбора — явные условия с результатами — используется оператор [`CASE`](../language/CASE_operator.md).

В полиморфной форме [`MULTI`](../language/MULTI_operator.md) выбирает операнд, сигнатура которого соответствует классам параметров; [`OVERRIDE`](../language/OVERRIDE_operator.md) возвращает первый операнд, значение которого не равно `NULL`; [`EXCLUSIVE`](../language/EXCLUSIVE_operator.md) делает то же самое, что и `OVERRIDE`, и дополнительно декларирует, что одновременно не равно `NULL` значение не более чем одного операнда.

В одиночной форме оператор [`IF`](../language/IF_operator.md) возвращает результат, когда условие выполняется; оператор [`IF ... THEN`](../language/IF_..._THEN_operator.md) дополнительно принимает альтернативный результат, возвращаемый, когда условие не выполняется.

### Примеры

```lsf
CLASS Color;
id = DATA STRING[100] (Color);

background 'Цвет' (Color c) = CASE
    WHEN id(c) == 'Black' THEN RGB(0,0,0)
    WHEN id(c) == 'Red' THEN RGB(255,0,0)
    WHEN id(c) == 'Green' THEN RGB(0,255,0)
;

id (TypeExecEnv type) = CASE EXCLUSIVE
    WHEN type == TypeExecEnv.materialize THEN 3
    WHEN type == TypeExecEnv.disablenestloop THEN 2
    WHEN type == TypeExecEnv.none THEN 1
    ELSE 0
;
```

```lsf
nameMulti (Human h) = MULTI 'Male' IF h IS Male, 'Female' IF h IS Female;

CLASS Ledger;
CLASS InLedger : Ledger;
quantity = DATA INTEGER (InLedger);

CLASS OutLedger : Ledger;
quantity = DATA INTEGER (OutLedger);

signedQuantity (Ledger l) = MULTI quantity[InLedger](l), quantity[OutLedger](l);
```


```lsf
CLASS Group;
markup = DATA NUMERIC[8,2] (Group);

markup = DATA NUMERIC[8,2] (Book);
group = DATA Group (Book);
overMarkup (Book b) = OVERRIDE markup(b), markup(group(b));

notNullDate (INTEGER i) = OVERRIDE date(i), 2010_01_01;
```


```lsf
background 'Цвет' (INTEGER i) = EXCLUSIVE RGB(255,238,165) IF i <= 5,
                                                   RGB(255,160,160) IF i > 5;

CLASS Human;

CLASS Male : Human;
CLASS Female : Human;

name(Human h) = EXCLUSIVE 'Male' IF h IS Male, 'Female' IF h IS Female;
```


```lsf
name = DATA STRING[100] (Book);
hasName (Book b) = TRUE IF name(b);

background (Book b) = RGB(224, 255, 128) IF b IS Book;

countTags (Book b) = GROUP SUM 1 IF in(b, Tag t);
```


```lsf
price1 = DATA NUMERIC[10,2] (Book);
price2 = DATA NUMERIC[10,2] (Book);
maxPrice (Book b) = IF price1(b) > price2(b) THEN price1(b) ELSE price2(b);

// если h будет другого класса, то будет NULL
sex (Human h) = IF h IS Male THEN 'Male' ELSE ('Female' IF h IS Female); 

isDifferent(a, b) = IF a != b THEN TRUE;
```
