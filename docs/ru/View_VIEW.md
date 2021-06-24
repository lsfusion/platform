---
title: 'Отображение (VIEW)'
---

Оператор отображения создает свойство, которое возвращает `TRUE`, если набор объектов входит в видимое пользователю множество наборов объектов в заданной группе объектов, и `NULL` в обратном случае.

### Язык

Для объявления свойства, определяющего отображается ли пользователю заданный набор объектов или нет, используется [оператор `VIEW`](Object_group_operator.md).

### Примеры

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
;
countF 'Кол-во фильтр. складов' = GROUP SUM 1 IF [ VIEW stores.s](Store s);
orderF 'Порядок в группе объектов' (Store s) = PARTITION SUM 1 IF [ FILTER stores.s](s) ORDER [ ORDER stores.s](s), s;
setNameX 'Добавить X к имени'()  {
    LOCAL k = INTEGER ();
    k() <- 0;
    FOR [ FILTER stores.s](Store s) ORDER [ ORDER stores.s](s) DO {
        k() <- k() + 1;
        name(s) <- 'X' + k() + name(s);
    }
}
```
