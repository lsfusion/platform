---
title: 'Form views'
---

A [form](Forms.md) view can be classified as follows:

#### [Interactive](Interactive_view.md)

A view in which the user can interact with an open form: select current objects, call [actions](Actions.md), change [data properties](Data_properties_DATA.md), and so on. Data is usually read as needed depending on user actions. It is this view (along with the [programming interface](Integration.md)) that is responsible for data input into the system.

#### [Static](Static_view.md)

In this view, when the [form opens](Open_form.md) all its data is read at once, after which this data is converted/sent to the client. This is a one-way view type.


:::info
By default, depending on the implementation specifics, conversion can take place either on the server (before sending to the client) or directly on the client itself.
:::

From a data flow standpoint, the interactive view is internal, meaning that the data remains inside the server/native client, while the static view is external, with the data converted and sent to the reporting subsystem, or to the operating system in the form of files in various formats. 

### Graphic view {#graphic}

Some views are *graphic*, meaning that to display them the data read must be placed in two-dimensional space: on paper or the screen of the device. Accordingly, for these views a design may/must be defined:

-   [Form design](Form_design.md) - for [interactive](Interactive_view.md) view.
-   [Report design](Report_design.md) - for [print](Print_view.md) view.

### Stack

import FormPresentationENSvg from './images/FormPresentationEn.svg';

<FormPresentationENSvg />