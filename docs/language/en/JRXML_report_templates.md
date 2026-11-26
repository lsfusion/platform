---
slug: "/JRXML_report_templates"
title: 'JRXML report templates'
---

A JRXML report template is a JasperReports template that the platform fills with the data of a form's [report](../paradigm/Report_design.md).

### Fields {#fields}

The `class` attribute of a `field` element specifies the Java class in which the platform supplies the field value.

| Property class | Field class |
|---|---|
| `INTEGER`, `YEAR` | `java.lang.Integer` |
| `LONG` | `java.lang.Long` |
| `DOUBLE` | `java.lang.Double` |
| `NUMERIC` | `java.math.BigDecimal` |
| `BOOLEAN`, `TBOOLEAN` | `java.lang.Boolean` |
| `STRING`, `ISTRING`, `BPSTRING`, `BPISTRING`, `TEXT`, `RICHTEXT`, `HTMLTEXT`, `JSON`, `JSONTEXT`, `XML`, `HTML`, `TSVECTOR`, `TSQUERY`, `LINK` and the links to files of a specific type | `java.lang.String` |
| `DATE` | `java.time.LocalDate` |
| `TIME` | `java.time.LocalTime` |
| `DATETIME` | `java.time.LocalDateTime` |
| `ZDATETIME` | `java.time.Instant` |
| `INTERVAL[DATE]`, `INTERVAL[DATETIME]`, `INTERVAL[TIME]`, `INTERVAL[ZDATETIME]` | `lsfusion.interop.form.property.cell.IntervalValue` |
| `COLOR` | `java.awt.Color` |
| `IMAGEFILE` | `java.io.InputStream` |
| `FILE` | `lsfusion.base.file.FileData` |
| `NAMEDFILE` | `lsfusion.base.file.NamedFileData` |
| `RAWFILE` and the other files of a specific type (`PDFFILE`, `JSONFILE`, ...) | `lsfusion.base.file.RawFileData` |
| a user class, including the `<object name>.object` field of an object of a user class | `java.lang.Long` with the id of the object |
| the `<object name>.object` field of an object of a built-in class | the field class of that class by the rows above |

For dates and times, JasperReports applies the text field's `pattern` attribute to `java.util.Date` values. The automatic design converts values from `java.time` in text field expressions.

| Property class | Expression in the automatic design |
|---|---|
| `DATE` | `java.sql.Date.valueOf($F{date})` |
| `TIME` | `java.sql.Time.valueOf($F{time})` |
| `DATETIME` | `java.sql.Timestamp.valueOf($F{dateTime})` |
| `ZDATETIME` | `java.sql.Timestamp.from($F{zDateTime})` |

A custom template can declare the field as `java.util.Date` for any of these four classes, as `java.sql.Time` for `TIME`, or as `java.sql.Timestamp` for `DATETIME` and `ZDATETIME`. The platform converts the value to the declared class.

An image field declared as `<field name="photo" class="java.io.InputStream"/>` receives an image stream or `NULL` when there is no image. Use `$F{photo}` as the image expression. Declaring `<field name="photo" class="lsfusion.base.file.RawFileData"/>` gives the template the file itself. Use `$F{photo} == null ? null : $F{photo}.getInputStream()` to get its stream.

The platform removes trailing spaces from string values.

### Subreports {#subreports}

Declare three parameters in the parent template for each subreport. The `<subreport ID>` is the name of the subreport's first object group, which is also the postfix of its default template name. Reference each parameter with `$P{...}` in the corresponding element of `subreport`.

| Parameter name | Class | Value | Expression element |
|---|---|---|---|
| `<subreport ID>_report` | `net.sf.jasperreports.engine.JasperReport` | Compiled subreport | `subreportExpression` |
| `<subreport ID>_source` | `net.sf.jasperreports.engine.JRDataSource` | Subreport data source | `dataSourceExpression` |
| `<subreport ID>_params` | `java.util.Map` | Map of subreport parameters | `parametersMapExpression` |

### Properties in columns {#columns}

In a text field for a property with the [`COLUMNS` option](Properties_and_actions_block.md#options), write the field reference without an index. The platform appends the zero-based column index in the copied expressions. For example, it converts `$F{price}` to `$F{price[0]}`, `$F{price[1]}`, and so on.

To lay out several text fields as a block, set the same `key` on their `reportElement` elements. For example, set `key="prices"` on text fields that show `$F{price.header}` and `$F{price}` in the same band.

### Additional fields {#auxiliary-fields}

Each of these form options supplies the value of its property in an additional field. Choose the Java class of the field from the [class table](#fields) using the option property's class.

| Form option | Field name |
|---|---|
| `HEADER` | `<field name>.header` |
| `FOOTER` | `<field name>.footer` |
| `BACKGROUND` | `<field name>.background` |
| `FOREGROUND` | `<field name>.foreground` |
| `IMAGE` | `<field name>.image` |

### Property visibility {#showif}

The automatic design declares the `<field name>.showif` field for a property with the `SHOWIF` option. In a custom template, declare that field to use the option and choose its Java class from the [class table](#fields) using the visibility property's class.

For the removal rule to match a text field, write its text field expression as exactly `$F{<field name>}`.
