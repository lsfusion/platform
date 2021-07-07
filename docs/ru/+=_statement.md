---
title: 'Инструкция +='
---

Инструкция `+=` - добавление реализации (варианта выбора) к [абстрактному свойству](Property_extension.md).

### Синтаксис

    propertyId (param1, ..., paramN) += implExpr;
    propertyId (param1, ..., paramN) += WHEN whenExpr THEN implExpr;

### Описание

Инструкция `+=` добавляет реализацию к абстрактному свойству. Синтаксис добавления реализации зависит от типа абстрактного свойства. Если абстрактное свойство имеет тип `CASE`, то реализация должна описываться в виде `WHEN ... THEN ...`, в ином случае реализация должна быть описана просто в виде свойства. 

### Параметры

- `propertyId`

    [Идентификатор](IDs.md#propertyid) абстрактного свойства. 

- `param1, ..., paramN`

    Список параметров, которые будут использованы при описании реализации. Каждый элемент является [типизированным параметром](IDs.md#paramid). Количество этих параметров должно совпадать с количеством параметров абстрактного свойства. Эти параметры далее могут быть использованы в выражениях реализации абстрактного свойства и условия выбора этой реализации.

- `implExpr`

    [Выражение](Expression.md), значение которого определяет реализацию абстрактного свойства.

- `whenExpr`

    Выражение, значение которого определяет условие выбора реализации для абстрактного свойства типа `CASE`. 

### Примеры


```lsf
CLASS ABSTRACT AClass;
CLASS BClass : AClass;
CLASS CClass : AClass;
CLASS DClass : AClass;

name(AClass a) = ABSTRACT BPSTRING[50] (AClass);
innerName(BClass b) = DATA BPSTRING[50] (BClass);
innerName(CClass c) = DATA BPSTRING[50] (CClass);
innerName(DClass d) = DATA BPSTRING[50] (DClass);

name(BClass b) = 'B' + innerName(b);
name(CClass c) = 'C' + innerName(c);

name[AClass](BClass b) += name(b);
// Здесь слева будет найден name[AClass], потому что поиск идет только среди абстрактных свойств,
// справа же будет найден name[CClass]
name(CClass c) += name(c); 
name(DClass d) += 'DClass' + innerName(d) IF d IS DClass;
```
