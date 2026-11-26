---
slug: "/Report_design"
title: 'Report design'
---

The *report design* defines how each [report](Print_view.md) of a [form](Forms.md) is laid out when the result document is built.

### Templates {#template}

For each report you need to specify a special file (*template*) which will be used to build the result document. The name of the template for a specific report is `<canonical name of form>_<name of the first group of objects>`, where the form's [canonical name](Naming.md#canonicalname) includes its namespace, with each `.` replaced by `_`. An [empty](Static_view.md#empty) object group is considered to have no name, so if the first object group is empty, the name of the template is simply equal to the canonical name of the form (without the `_` postfix). The empty object group is always the form's first object group, so the form always has a top report named by the canonical name of the form alone, without a postfix. When no subreports are needed, this is the only template. A form whose dependent groups make up a single flat report under the [report hierarchy rules](Print_view.md#buildhierarchy) uses only the top template, without a postfix. An independent branch or a group explicitly marked as a subreport uses its own template, with the `_<group>` postfix taken from the subreport's first (non-empty) object group. A form with two independent object groups, for example, produces three templates, one for the top report without a postfix and one for each subreport.

A template can use the properties of object groups in its report or any ancestor report as fields. Each field takes the name of its property on the form.

The template of a parent report includes each of its child reports as a subreport. The properties and filters in each subreport use the current object values of the parent report as the values of their upper objects.

By default, the template is resolved by its name. The developer can also explicitly specify, for the form as a whole or for an individual report, a property whose value provides the template to use — either a file name to be resolved the same way, or the template file itself. The object values of the form can be used as the parameters of this property.

Templates are used only as a complete set. If the template of at least one report of the form is not found — neither by name nor through such a property — all the templates that were found are ignored, and the [automatic design](#auto) is used for the whole form. So a form with subreports needs its top template even when it holds nothing but the subreport elements. The substitution itself produces no error: if some of the templates were found, a warning naming the missing template is written to the server log, and if none of them were found, nothing is reported at all.

### Properties in columns {#columns}

When the document is built, the platform makes one copy per column value of each text field placed directly in a band that refers to a property shown in [columns](Form_structure.md#groupcolumns). It divides the bounding rectangle of all text fields in the band with the same key as the original into equal parts, one per column value. For a field with no key, it divides the field's own rectangle. Each copy keeps the original field's relative horizontal position and width within its part. With no column values, the text field is removed.

### Property visibility {#showif}

When the `useShowIfInReports` [working parameter](Working_parameters.md) is enabled (the default), the platform checks the property's declared visibility field in the first row of the report. If the value is `NULL`, it removes each text field that sits directly in a band and gets its value solely from a direct reference to the property's field. It also cuts the vertical strip occupied by the text field. The platform cuts the portions of elements placed directly in the bands that overlap the strip and shifts the elements to its right leftward by the width of the strip. For formats with pagination (all except XLS, XLSX and HTML), it then stretches the remaining elements proportionally to the original width.

### Automatic design {#auto}

If at least one of the templates cannot be found when generating reports, an automatic template generation mechanism starts based on the current hierarchy: it creates a separate template for each report and adds all the necessary properties and subreports to it. At the same time, if several object groups are included in one report then the lowest group in the list will be used for detail, and a separate report group will be created with its own block for each upper group in the template, to which all the properties from this group will be added.

### Template format {#format}

The LGPL technology [JasperReports](https://community.jaspersoft.com/project/jasperreports-library) is used as a specific implementation of the document generation mechanism. Accordingly, templates are jrxml format files, which can be edited using the [JasperSoft Studio](https://community.jaspersoft.com/project/jaspersoft-studio) application. At the time of generating the report, template files with the name `<template name>.jrxml` are searched for in the server's current [classpath](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html) (including all subdirectories). If no template file is found, the platform will generate an automatic design. When starting the server from the IDE and generating a report in [preview mode](In_a_print_view_PRINT.md#interactive), you can use the corresponding buttons to save this design to the launch directory (and the source directory), and then edit it in line with the requirements of the task (in this case, the platform will automatically sync files in the launch directory and source directory).

### Language

Which template (report file) is used for the form and for its individual reports can be specified explicitly through the [`FORM` statement](../language/FORM_statement.md).

The [`SUBREPORT` option of the object block](../language/Object_blocks.md#objects) starts a separate report at the specified object group.

The [JRXML template reference](../language/JRXML_report_templates.md) defines the field classes, subreport parameters, column field references and keys, and declarations for additional fields and `SHOWIF`.
