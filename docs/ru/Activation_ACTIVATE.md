---
title: 'Активация (ACTIVATE)'
---

Оператор *активации* создает [действие](Actions.md), которое делает активным один из трех элементов формы:

-   Свойство - устанавливает фокус на заданное [свойство](Properties.md) на форме.
-   Закладка - выбирает одну из вкладок в заданной [панели закладок](Form_design.md#containers).
-   Форма - активирует заданную [форму](Forms.md), если она была открыта. Если одна форма была открыта несколько раз, активируется та, которая была открыта первой.

### Язык

Для создания действия, активирующего элемент формы, используется [оператор `ACTIVATE`](ACTIVATE_operator.md).

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

 
