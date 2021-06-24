---
title: 'View logic'
sidebar_label: Overview
---

The key element of the view logic is the [form](Forms.md). Forms are used in the platform to organize the input and output of information into the system (from the system).

### Navigator

In the simplest version of the user interface the user opens several fixed forms with which he will work, switching between tabs. However, if the user works with a large number of forms, opening them all at once is not very convenient. In such cases you can use the so-called [navigator](Navigator.md) to work with forms. In this case, when the client starts, only the navigator itself is shown to the user (no forms are opened), and the user can open the forms on his own as needed. Moreover, in the navigator, the user can not only open forms [in the interactive view](In_an_interactive_view_SHOW_DIALOG.md), but execute any [actions](Actions.md) at large.

### Stack

import PresentationENSvg from './images/PresentationEn.svg';

<PresentationENSvg />