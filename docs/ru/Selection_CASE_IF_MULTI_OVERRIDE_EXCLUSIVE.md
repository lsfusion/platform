---
title: 'Выбор (CASE, IF, MULTI, OVERRIDE, EXCLUSIVE)'
---

Оператор *выбора* создает свойство, которое для набора *условий* определяет, какое из этих условий выполняется, и возвращает значение соответствующего этому условию *результата*. Если ни одно из условий не выполняется, то значением создаваемого свойства будет `NULL`. 

Все условия и результаты задаются как некоторые свойства и/или параметры. Соответственно, условие *выполняется*, если значение свойства или параметра, с помощью которого задается это условие, не равно `NULL`. 

### Полиморфная форма {#poly}

Также в платформе существует возможность задавать условие и соответствующий ему результат одним свойством. В этом случае условием может являться либо принадлежность [сигнатуре](Property_signature_CLASS.md) этого свойства, либо само это свойство. Такую форму оператора выбора будем называть *полиморфной*.


:::info
Отметим то, что [оператор экстремума](Extremum_MAX_MIN.md) и [логические операторы](Logical_operators_AND_OR_NOT_XOR.md) также по сути являются разновидностями оператора выбора (причем его полиморфной формой, то есть условия и результат определяются одним свойством)
:::

### Взаимоисключаемость условий {#exclusive}

Для оператора выбора можно указать, что все его условия должны быть *взаимоисключающими*. Соответственно, если эта опция указана, а условия не являются взаимоисключающими, платформа выдаст соответствующую ошибку.

Стоит отметить, что такая проверка является не более чем подсказкой платформе (для лучшей оптимизации) и определенным самоконтролем со стороны разработчика. Однако, при этом использование такой проверки позволяет во многих случаях сделать код более прозрачным и читабельным (особенно в полиморфной форме оператора выбора).

### Неявное задание

У этого оператора есть возможность [неявного задания](Property_extension.md) с помощью техники расширений, что позволяет, в частности, реализовывать полиморфизм в том виде, в котором это обычно принято делать в ООП.

### Одиночная форма {#single}

Оператор выбора в *одиночной* форме проверяет ровно одно условие. Если это условие выполняется, возвращается значение указанного результата. Также в этой форме существует возможность указать *альтернативный результат*, значение которого будет возвращаться, если условие не выполняется.


:::info
Тип взаимоисключения и неявное задание для этой формы оператора не имеют смысла / не поддерживаются
:::

### Определение класса результата

Результирующим классом оператора выбора является общий предок его операндов ([встроенный](Built-in_classes.md#commonparentclass) или [пользовательский](User_classes.md#commonparentclass)).

### Язык

Для создания свойства, реализующего выбор в общем случае, используется оператор [`CASE`](CASE_operator.md). Полиморфная форма оператора выбора реализуется при помощи операторов [`MULTI`](MULTI_operator.md), [`OVERRIDE`](OVERRIDE_operator.md) и [`EXCLUSIVE`](EXCLUSIVE_operator.md), одиночная - при помощи операторов [`IF`](IF_operator.md) и [`IF ... THEN`](IF_..._THEN_operator.md) (только в этом операторе можно указывать альтернативный результат).

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

sex (Human h) = IF h IS Male THEN 'Male' ELSE ('Female' IF h IS Female); // если h будет другого класса, то будет NULL

isDifferent(a, b) = IF a != b THEN TRUE;
```
