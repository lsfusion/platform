---
title: 'Блок PIVOT'
---

Блоки PIVOT [инструкции `FORM`](FORM_statement.md) - набор конструкций, управляющих опциями [вида отображения `PIVOT`](Interactive_view.md#property) в интерактивном представлении формы.

### Синтаксис

```
PIVOT 
pivotOptionsBlock1, ..., pivotOptionsBlockN
```

Где каждый `pivotOptionsBlocki` имеет следующий синтаксис:

```
groupObjectId pivotOptions |
COLUMNS columnPropertyDraw1, ..., columnPropertyDrawN |
ROWS rowPropertyDraw1, ..., rowPropertyDrawN |
MEASURE measurePropertyDraw1, ..., measurePropertyDrawN
```

`pivotOptions` имеет следующий синтаксис:

```
  (
  pivotOptionsType |
  SUM | MAX | MIN |
  SETTINGS | NOSETTINGS | 
  CONFIG configStr
  )*
```

### Описание

Блок PIVOT позволяет задать опции [вида отображения PIVOT](Interactive_view.md#property). 

### Параметры 

- `groupObjectId`

    [Идентификатор группы объектов](IDs.md#groupobjectid), к которому применяются `pivotOptions`.

- `pivotOptionsType`

    Строковый литерал, который задаёт по умолчанию один из стандартных типов отображения PIVOT. Список значений: 
    - `Table` (используется по умолчанию)
    - `Table Bar Chart`
    - `Table Heatmap`
    - `Table Row Heatmap`
    - `Table Col Heatmap`
    - `Bar Chart`
    - `Stacked Bar Chart`
    - `Line Chart`
    - `Area Chart`
    - `Scatter Chart`
    - `Multiple Pie Chart`
    - `Horizontal Bar Chart`
    - `Horizontal Stacked Bar Chart` 

- `SUM | MAX | MIN`

    Ключевые слова, задающие тип аггрегатора:
    - `SUM` - сумма (используется по умолчанию) 
    - `MAX` - максимум 
    - `MIN` - минимум 

- `SETTINGS | NOSETTINGS`

    Ключевые слова, определяющие, показываются ли пользователю настройки PIVOT:
    - `SETTINGS` - настройки показываются (используется по умолчанию)
    - `NOSETTINGS` - настройки не показываются

- `configStr`
    Строковый литерал, указывающий на имя javascript функции, используемой для отображения PIVOT.
