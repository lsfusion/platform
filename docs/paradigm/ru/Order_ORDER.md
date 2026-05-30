---
slug: "/Order_ORDER"
title: 'Порядок (ORDER)'
---

Оператор *порядка* создает [свойство](Properties.md), определенное на объектах группы объектов, которое возвращает значение, отражающее относительный порядок их набора в установленном в группе [порядке](Form_structure.md#sort). Это значение не имеет самостоятельного смысла: осмысленно только его сравнение с тем же свойством для других наборов объектов группы, и такое сравнение воспроизводит текущий порядок группы.

### Язык

Для объявления свойства, определяющего порядок в группе объектов, используется [оператор `ORDER`](../language/Object_group_operator.md).

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
