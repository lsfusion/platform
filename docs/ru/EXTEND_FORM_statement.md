---
title: 'Инструкция EXTEND FORM'
---

Инструкция `EXTEND FORM` - [расширение](Form_extension.md) существующей [формы](Forms.md).

### Синтаксис

    EXTEND FORM formName 
        formBlock1
        ...
        formBlockN
    ;

### Описание

Инструкция `EXTEND FORM` позволяет расширять существующую форму дополнительными [блоками формы](FORM_statement.md#blocks).

### Параметры

- `formName`

    Имя расширяемой формы. [Составной идентификатор](IDs.md#cid).

- `formBlock1 ... formBlockN`

    Блоки формы.

### Пример

```lsf
CLASS ItemGroup;
name = DATA ISTRING[100] (ItemGroup);

itemGroup = DATA ItemGroup (Item);

EXTEND FORM items
    PROPERTIES(i) NEWSESSION DELETE // добавляем на форму кнопку удаления

    OBJECTS g = ItemGroup BEFORE i // добавляем на форму объект группы товаров перед товаром
    PROPERTIES(g) READONLY name
    // если бы объект был добавлен после объекта с товарами, то фильтрация шла бы по группе товаров, а не по товарам
    FILTERS itemGroup(i) == g 
;
```
