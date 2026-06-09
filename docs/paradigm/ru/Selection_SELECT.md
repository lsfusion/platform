---
slug: "/Selection_SELECT"
title: 'Выделение (SELECT)'
---

Операторы выделения создают [свойства](Properties.md), сообщающие о выделении строк пользователем в [группе объектов](Form_structure.md#objects):

-   свойство набора объектов возвращает `TRUE`, если этот набор сейчас выделен (отмечен) пользователем в группе, и `NULL` в противном случае;
-   свойство группы в целом возвращает `TRUE`, если в группе сейчас включено выделение нескольких строк, и `NULL` в противном случае.

### Язык

Для объявления этих свойств используются операторы [`SELECT` и `SELECT ACTIVE`](../language/Object_group_operator.md).

### Примеры

```lsf
CLASS Store;
name = DATA STRING[100] (Store);

FORM stores
    OBJECTS s = Store
    PROPERTIES(s) name
;
selectedCount 'Количество выделенных складов' () = GROUP SUM 1 IF [ SELECT stores.s](Store s);
multiSelectActive 'Включено выделение нескольких строк' () = [ SELECT ACTIVE stores.s]();
```
