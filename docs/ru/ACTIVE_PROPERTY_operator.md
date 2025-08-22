---
title: 'Оператор ACTIVE PROPERTY'
---

Оператор `ACTIVE PROPERTY` - создание [свойства](Properties.md), реализующего проверку [активности](Activity_ACTIVE.md) свойства на форме.

### Синтаксис 

```
ACTIVE PROPERTY formPropertyId
```

### Описание

Оператор `ACTIVE PROPERTY` создает свойство, которое возвращает `TRUE`, если указанное свойство активно на [форме](Forms.md). 

### Параметры

- `formName`

    Имя формы. [Составной идентификатор](IDs.md#cid).

- `formPropertyId`  

    Глобальный [идентификатор свойства или действия на форме](IDs.md#formpropertyid), активность которого проверяется.

### Примеры

```lsf
FORM users
OBJECTS c = CustomUser
PROPERTIES(c) name, login;

activeLogin = ACTIVE PROPERTY users.login(c);
EXTEND FORM users
PROPERTIES() activeLogin;
```
