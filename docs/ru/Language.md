---
title: 'Язык'
---

*lsFusion* - это язык программирования, на котором описываются [модули](Modules.md) lsFusion.

Каждый модуль состоит из последовательности [инструкций](Statements.md), разделенных на [*заголовок*](Module_header.md) и *тело* модуля. Заголовок может включать в себя четыре специальные инструкции: `MODULE`, `REQUIRE`, `PRIORITY` и `NAMESPACE`, описывающие свойства модуля. После заголовка идет тело модуля, которое состоит из остальных инструкций и описывает логику работы этого модуля.

### Пример

```lsf
// Заголовок модуля
MODULE LanguageExample;

REQUIRE System;

NAMESPACE Example;

// Тело модуля

// Объявление классов
CLASS Employee;
CLASS Company;

// Объявление свойств
name(employee) = DATA BPSTRING[100](Employee);
age(employee) = DATA INTEGER(Employee);
company(employee) = DATA Company(Employee);

name(company) = DATA BPSTRING[100](Company);

// Объявление формы
FORM employeeForm
    OBJECTS e = Employee
    PROPERTIES(e) name, age, company
;

// Добавление формы в навигатор
NAVIGATOR {
    NEW employeeForm;
}
```

