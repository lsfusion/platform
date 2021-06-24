---
title: 'How-to: EXEC'
---

## Пример 1

### Условие

Есть категория книг, для которой заданы наименование, числовой код и дата начала продаж.

```lsf
CLASS Category 'Категория';

name 'Наименование' = DATA ISTRING[50] (Category);
id 'Код' = DATA INTEGER (Category);
saleDate 'Дата начала продаж' = DATA DATE (Category);
```

Нужно создать действие, которое создаст 3 новых предопределенных категории.

### Решение

```lsf
createCategory 'Создать категорию' (ISTRING[50] name, INTEGER id, DATE saleDate)  {
    NEW c = Category {
        name(c) <- name;
        id(c) <- id;
        saleDate(c) <- saleDate;
    }
}

create3Categories 'Создать 3 категории' ()  {
    createCategory('Категория 1', 1, 2010_02_14);
    createCategory('Категория 2', 2, 2011_03_08);
    createCategory('Категория 3', 3, 2014_07_01);
}
```

## Пример 2

### Условие

Аналогично [**Примеру 1**](#пример-1), но у каждой категории задан ее "родитель".

```lsf
parent 'Родитель' = DATA Category (Category); // если значение NULL, то родителя нету
```

Нужно создать действие, которое проставит глубину категории для каждой из них.

### Решение

```lsf
depth = DATA INTEGER (Category);
fillDepth (Category c, INTEGER depth)  {
    FOR parent(Category i) == c DO {
        depth(i) <- depth;
        fillDepth(i, depth + 1);
    }
}

run()  {
    fillDepth(NULL, 0);
}
```
