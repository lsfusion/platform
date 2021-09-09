---
title: 'Создание сессий (NEWSESSION, NESTEDSESSION)'
---

Оператор создания новой [сессии](Change_sessions.md) позволяет выполнить действие в другой, отличной от текущей, сессии. 

Как и для других операторов управления сессиями, для оператора создания сессии можно явно указать [вложенные локальные свойства](Session_management.md#nested).

### Вложенные сессии {#nested}

Также в платформе существует возможность создать новую *вложенную* сессию. В этом случае все изменения, произошедшие в текущей сессии, копируются в создаваемую вложенную сессию (это же происходит и при [отмене изменений](Cancel_changes_CANCEL.md) во вложенной сессии). В то же время, при [применении изменений](Apply_changes_APPLY.md) в создаваемой вложенной сессии все изменения копируются обратно в текущую сессию (при этом в базу данных они не сохраняются). 

### Язык

Для создания действия, выполняющего другое действие в новой сессии, используется [оператор `NEWSESSION`](NEWSESSION_operator.md) (для вложенных сессий используется [оператор `NESTEDSESSION`](NESTEDSESSION_operator.md)).

### Примеры

```lsf
testNewSession ()  {
    NEWSESSION {
        NEW c = Currency {
            name(c) <- 'USD';
            code(c) <- 866;
        }
        APPLY;
    }
    // здесь новый объект класса Currency уже в базе данных

    LOCAL local = BPSTRING[10] (Currency);
    local(Currency c) <- 'Local';
    NEWSESSION {
        MESSAGE (GROUP SUM 1 IF local(Currency c) == 'Local'); // возвратит NULL
    }
    NEWSESSION NESTED (local) {
        // возвратит кол-во объектов класса Currency
        MESSAGE (GROUP SUM 1 IF local(Currency c) == 'Local'); 
    }

    NEWSESSION {
        NEW s = Sku {
            id(s) <- 1234;
            name(s) <- 'New Sku';
            SHOW sku OBJECTS s = s;
        }
    }

}
```


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
