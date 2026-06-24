---
slug: "/Report_design"
title: 'Report design'
---

The *report design* defines how each [report](Print_view.md) of a [form](Forms.md) is laid out when the result document is built.

### Templates {#template}

For each report you need to specify a special file (*template)* which will be used to build the result document. The name of the template for a specific report is `<canonical name of form>_<name of the first group of objects>`, where the form's [canonical name](Naming.md#canonicalname) includes its namespace, with each `.` replaced by `_`. An [empty](Static_view.md#empty) object group is considered to have no name, so if the first object group is empty, the name of the template is simply equal to the canonical name of the form (without the `_` postfix). The empty object group is always the form's first object group, so the form always has a top report named by the canonical name of the form alone, without a postfix; when no subreports are needed, this is the only template. Subreports arise only from object groups that are independent of each other: a chain of mutually dependent groups (each filtered by the objects of the previous one) stays a single flat report under the top template without a postfix, whereas each independent branch of the group hierarchy becomes a separate subreport. Each subreport adds its own template, named with the `_<group>` postfix after its first (non-empty) object group — so a form with two independent object groups, for example, produces three templates: the top one without a postfix and one for each of the two subreports.

When developing a template, the developer can use the object group properties that are included in the corresponding report or ancestor reports as fields. The names and types of fields will be equal to the names and types of properties on the form. If one report is a child of another report, then it should be inserted into the template as a subreport. In this case, the properties and filters in it will use the current values of the objects of the upper report as the values of their upper objects.

By default, the template is resolved by its name. The developer can also explicitly specify, for the form as a whole or for an individual report, a property whose value provides the template to use — either a file name to be resolved the same way, or the template file itself. The object values of the form can be used as the parameters of this property.

### Automatic design {#auto}

If at least one of the templates cannot be found when generating reports, an automatic template generation mechanism starts based on the current hierarchy: it creates a separate template for each report and adds all the necessary properties and subreports to it. At the same time, if several object groups are included in one report then the lowest group in the list will be used for detail, and a separate report group will be created with its own block for each upper group in the template, to which all the properties from this group will be added.

### Template format {#format}

The LGPL technology [JasperReports](https://community.jaspersoft.com/project/jasperreports-library) is used as a specific implementation of the document generation mechanism. Accordingly, templates are jrxml format files, which can be edited using the [JasperSoft Studio](https://community.jaspersoft.com/project/jaspersoft-studio) application. At the time of generating the report, template files with the name `<template name>.jrxml` are searched for in the server's current [classpath](https://docs.oracle.com/javase/8/docs/technotes/tools/windows/classpath.html) (including all subdirectories). If no template file is found, the platform will generate an automatic design. When starting the server from the IDE and generating a report in [preview mode](In_a_print_view_PRINT.md#interactive), you can use the corresponding buttons to save this design to the launch directory (and the source directory), and then edit it in line with the requirements of the task (in this case, the platform will automatically sync files in the launch directory and source directory).

### Language

Which template (report file) is used for the form and for its individual reports can be specified explicitly through the [`FORM` statement](../language/FORM_statement.md).
