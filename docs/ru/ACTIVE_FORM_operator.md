---
title: 'Оператор ACTIVE FORM'
---

Оператор `ACTIVE FORM` - создание [действия](Actions.md), проверяющего [активность](Activity_ACTIVE.md) [формы](Forms.md).

### Синтаксис

    ACTIVE FORM formName

### Описание

Оператор `ACTIVE FORM` создает действие, которое записывает в свойство `System.isActiveForm[]` значение активности указанной формы. Если форма активна, то записывается `TRUE`.

### Параметры

- `formName`

    Имя формы. [Составной идентификатор](IDs.md#cid).

### Примеры

```lsf
FORM exampleForm;
testActive  {
    ACTIVE FORM exampleForm;
    IF isActiveForm() THEN MESSAGE 'Example form is active';
}
```
