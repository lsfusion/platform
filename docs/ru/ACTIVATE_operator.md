---
title: 'Оператор ACTIVATE'
---

Оператор `ACTIVATE` - создание [действия](Actions.md), [активирующего](Activation_ACTIVATE.md) указанную [форму](Forms.md), закладку, свойство или действие на форме

### Синтаксис 

    ACTIVATE FORM formName
    ACTIVATE TAB formName.componentSelector
    ACTIVATE PROPERTY formPropertyId

### Описание

Оператор `ACTIVATE` создает действие, которое активизирует форму, закладку, свойство или действие на форме.

### Параметры

- `formName`

    Имя формы. [Составной идентификатор](IDs.md#cid).

- `componentSelector`

    [Селектор](DESIGN_statement.md#selector) компонента дизайна. Компонент должен быть закладкой на панели вкладок.

- `formPropertyId`

    Глобальный [идентификатор свойства или действия на форме](IDs.md#formpropertyid), на которое должен перейти фокус.

### Примеры

```lsf
//Форма с двумя закладками
FORM myForm 'Моя форма'
    OBJECTS u = CustomUser
    PROPERTIES(u) name

    OBJECTS c = Chat
    PROPERTIES(c) name
;

DESIGN myForm {
    NEW tabbedPane FIRST {
        type = TABBED;
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

testAction()  {
    ACTIVATE FORM myForm;
    ACTIVATE TAB myForm.recent;
}

CLASS ReceiptDetail;
barcode = DATA STRING[30] (ReceiptDetail);
quantity = DATA STRING[30] (ReceiptDetail);

FORM POS
    OBJECTS d = ReceiptDetail
    PROPERTIES(d) barcode, quantityGrid = quantity
;

createReceiptDetail 'Добавить строку продажи'(STRING[30] barcode)  {
    NEW d = ReceiptDetail {
        barcode(d) <- barcode;
        ACTIVATE PROPERTY POS.quantityGrid;
    }
}
```
