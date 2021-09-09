---
title: 'Оператор NESTEDSESSION'
---

Оператор `NESTEDSESSION`- создание [действия](Actions.md), которое выполняет другое действие во [вложенной сессии](New_session_NEWSESSION_NESTEDSESSION.md#nested).

### Синтаксис

    NESTEDSESSION action 

### Описание

Оператор `NESTEDSESSION` создает действие, которое выполняет другое действие во вложенной сессии. При этом все изменения, уже произошедшие в текущей сессии, попадают в создаваемую вложенную сессию. Также все изменения, которые будут произведены во вложенной сессии, попадут в текущую сессию при [применении изменений](Apply_changes_APPLY.md) во вложенной сессии.

### Параметры

- `action`

    [Контекстно-зависимый оператор-действие](Action_operators.md#contextdependent), описывающий действие, которое должно быть выполнено во вложенной сессии.

### Примеры

```lsf
testNestedSession ()  {
    NESTEDSESSION {
        name(Sku s) <- 'aaa';
        APPLY; // на самом деле изменения применятся не в базу данных, а в "верхнюю" сессию
    }

    MESSAGE (GROUP SUM 1 IF name(Sku s) == 'aaa'); // возвращает все строки
    CANCEL;
    // возвращает NULL, если в базе не было раньше Sku с именем aaa
    MESSAGE (GROUP SUM 1 IF name(Sku s) == 'aaa'); 

}

FORM sku
    OBJECTS s = Sku PANEL
    PROPERTIES(s) id, name
;
newNestedSession()  {
    NESTEDSESSION {
        NEW s = Sku {
            // показывает форму, но любые изменения в ней не будут применены в базу данных,
            // а будут сохранены в "верхней сессии"
            SHOW sku OBJECTS s = s;
        }
    }
}
```
