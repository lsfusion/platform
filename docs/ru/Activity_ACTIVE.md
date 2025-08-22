---
title: 'Активность (ACTIVE)'
---

Оператор *активности* создает свойство, которое определяет, является ли активным один из следующих элементов формы:

-   Свойство - находится ли фокус на заданном [свойстве](Properties.md) на форме.
-   Закладка - является ли одна из закладок активной в заданной [панели закладок](Form_design.md#containers).
-   Форма - определяет активна ли у пользователя заданная [форма](Forms.md).

### Язык

Для создания свойства, определяющего активность закладки, используется [оператор `ACTIVE TAB`](ACTIVE_TAB_operator.md). 
Для создания свойства, определяющего активность свойства, используется [оператор `ACTIVE PROPERTY`](ACTIVE_PROPERTY_operator.md). 
Определение активности формы реализуется путем создания действия с использованием [оператора `ACTIVE FORM`](ACTIVE_FORM_operator.md).

### Примеры

```lsf
//Форма с двумя закладками
FORM tabbedForm 'Форма с табами'
    OBJECTS u = CustomUser
    PROPERTIES(u) name

    OBJECTS c = Chat
    PROPERTIES(c) name
;

DESIGN tabbedForm {
    NEW tabPane FIRST {
        tabbed = TRUE;
        NEW contacts {
            caption = 'Контакты';
            MOVE BOX(u);
        }
        NEW recent {
            caption = 'Последние';
            MOVE BOX(c);
        }
    }
}

//Активна ли закладка 'Последние'
recentActive() = ACTIVE TAB tabbedForm.recent;
```

```lsf
FORM users
OBJECTS c = CustomUser
PROPERTIES(c) name, login;

activeLogin = ACTIVE PROPERTY users.login(c);
EXTEND FORM users
PROPERTIES() activeLogin;
```

```lsf
FORM exampleForm;
testActive  {
    ACTIVE FORM exampleForm;
    IF isActiveForm() THEN MESSAGE 'Example form is active';
}
```

  
