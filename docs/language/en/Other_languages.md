---
slug: "/Other_languages"
title: 'Other languages'
---

This section describes the interfaces used by code, markup, and styles written in other languages, including JavaScript, HTML, and CSS in the client and Java and JRXML in the application server.

- [JavaScript](Client_JavaScript_modules.md) covers the client modules that hold all client code, including React components. It also covers the [render functions](Render_functions.md) and [edit functions](Edit_functions.md) of classic custom views, the [functions called by `INTERNAL CLIENT`](INTERNAL_CLIENT_functions.md), and the [form and navigator controllers](Form_and_navigator_controllers.md) that any view written in JavaScript receives. Its [React](React_views.md) subsection describes the calls a React component can make and the data it receives from the form for a container or component, or from the navigator and window for a window view.
- [HTML](HTML_templates.md) covers the markup of templates for form containers and navigator windows.
- [CSS](CSS_view_styles.md) covers the styles of forms, navigator elements, and windows drawn by the web client.
- [Java](Java_integration_API.md) covers the classes and methods for accessing lsFusion from `InternalAction` subclasses and from [Spring beans based on `EventServer`](Custom_Spring_bean_EventServer.md).
- [JRXML](JRXML_report_templates.md) covers the fields, subreport parameters, and markup used in report templates.
