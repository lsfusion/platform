---
slug: "/JRXML_report_templates"
title: 'Шаблоны отчетов JRXML'
---

Шаблон отчета JRXML — это шаблон JasperReports, который платформа заполняет данными [отчета формы](../paradigm/Report_design.md).

### Поля {#fields}

Атрибут `class` элемента `field` задает Java-класс, в котором платформа передает значение поля.

| Класс свойства | Класс поля |
|---|---|
| `INTEGER`, `YEAR` | `java.lang.Integer` |
| `LONG` | `java.lang.Long` |
| `DOUBLE` | `java.lang.Double` |
| `NUMERIC` | `java.math.BigDecimal` |
| `BOOLEAN`, `TBOOLEAN` | `java.lang.Boolean` |
| `STRING`, `ISTRING`, `BPSTRING`, `BPISTRING`, `TEXT`, `RICHTEXT`, `HTMLTEXT`, `JSON`, `JSONTEXT`, `XML`, `HTML`, `TSVECTOR`, `TSQUERY`, `LINK` и ссылки на файлы конкретного типа | `java.lang.String` |
| `DATE` | `java.time.LocalDate` |
| `TIME` | `java.time.LocalTime` |
| `DATETIME` | `java.time.LocalDateTime` |
| `ZDATETIME` | `java.time.Instant` |
| `INTERVAL[DATE]`, `INTERVAL[DATETIME]`, `INTERVAL[TIME]`, `INTERVAL[ZDATETIME]` | `lsfusion.interop.form.property.cell.IntervalValue` |
| `COLOR` | `java.awt.Color` |
| `IMAGEFILE` | `java.io.InputStream` |
| `FILE` | `lsfusion.base.file.FileData` |
| `NAMEDFILE` | `lsfusion.base.file.NamedFileData` |
| `RAWFILE` и остальные файлы конкретного типа (`PDFFILE`, `JSONFILE`, ...) | `lsfusion.base.file.RawFileData` |
| пользовательский класс, в том числе поле `<имя объекта>.object` объекта пользовательского класса | `java.lang.Long` с идентификатором объекта |
| поле `<имя объекта>.object` объекта встроенного класса | класс поля этого класса по строкам выше |

Для дат и времени JasperReports применяет атрибут `pattern` текстового поля к значениям `java.util.Date`. В автоматическом дизайне значения классов из пакета `java.time` преобразуются в выражениях текстовых полей.

| Класс свойства | Выражение в автоматическом дизайне |
|---|---|
| `DATE` | `java.sql.Date.valueOf($F{date})` |
| `TIME` | `java.sql.Time.valueOf($F{time})` |
| `DATETIME` | `java.sql.Timestamp.valueOf($F{dateTime})` |
| `ZDATETIME` | `java.sql.Timestamp.from($F{zDateTime})` |

В собственном шаблоне поле любого из этих четырех классов можно объявить как `java.util.Date`, поле класса `TIME` — как `java.sql.Time`, а поле класса `DATETIME` или `ZDATETIME` — как `java.sql.Timestamp`. Платформа преобразует значение к объявленному классу.

Поле изображения, объявленное как `<field name="photo" class="java.io.InputStream"/>`, получает поток изображения или `NULL`, если изображения нет. В выражении элемента изображения укажите `$F{photo}`. Если объявить поле как `<field name="photo" class="lsfusion.base.file.RawFileData"/>`, шаблон получит сам файл. Чтобы получить его поток, используйте выражение `$F{photo} == null ? null : $F{photo}.getInputStream()`.

Платформа удаляет пробелы в конце строковых значений.

### Подотчеты {#subreports}

Для каждого подотчета объявите три параметра в родительском шаблоне. `<ID подотчета>` — имя первой группы объектов подотчета, которое также используется как постфикс имени его шаблона по умолчанию. Укажите ссылку на каждый параметр в виде `$P{...}` в соответствующем элементе внутри `subreport`.

| Имя параметра | Класс | Значение | Элемент выражения |
|---|---|---|---|
| `<ID подотчета>_report` | `net.sf.jasperreports.engine.JasperReport` | Скомпилированный подотчет | `subreportExpression` |
| `<ID подотчета>_source` | `net.sf.jasperreports.engine.JRDataSource` | Источник данных подотчета | `dataSourceExpression` |
| `<ID подотчета>_params` | `java.util.Map` | Карта параметров подотчета | `parametersMapExpression` |

### Свойства в колонках {#columns}

В текстовом поле для свойства с [опцией `COLUMNS`](Properties_and_actions_block.md#options) укажите ссылку на поле без индекса. В выражениях копий платформа добавит индекс колонки, начиная с нуля. Например, `$F{price}` превратится в `$F{price[0]}`, `$F{price[1]}` и так далее.

Чтобы расположить несколько текстовых полей единым блоком, задайте одинаковый `key` в их элементах `reportElement`. Например, задайте `key="prices"` для текстовых полей с выражениями `$F{price.header}` и `$F{price}` в одном бэнде.

### Дополнительные поля {#auxiliary-fields}

Каждая из этих опций формы передает значение своего свойства в дополнительном поле. Java-класс поля определяется по [таблице классов](#fields) и соответствует классу свойства опции.

| Опция формы | Имя поля |
|---|---|
| `HEADER` | `<имя поля>.header` |
| `FOOTER` | `<имя поля>.footer` |
| `BACKGROUND` | `<имя поля>.background` |
| `FOREGROUND` | `<имя поля>.foreground` |
| `IMAGE` | `<имя поля>.image` |

### Видимость свойств {#showif}

Автоматический дизайн объявляет поле `<имя поля>.showif` для свойства с опцией `SHOWIF`. Чтобы использовать эту опцию в собственном шаблоне, объявите это поле с Java-классом, который по [таблице классов](#fields) соответствует классу свойства, задающего видимость.

Чтобы к текстовому полю применялось правило удаления, в выражении текстового поля укажите ровно `$F{<имя поля>}`.
