---
title: 'Порядок (ORDER)'
---

Оператор *порядка* создает [свойство](Properties.md), которое возвращает порядковый номер набора объектов в заданной группе объектов, в соответствии с установленным в этой группе [порядком](Form_structure.md#sort).

### Язык

Для объявления свойства, определяющего порядок в группе объектов, используется [оператор `ORDER`](Object_group_operator.md).

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
