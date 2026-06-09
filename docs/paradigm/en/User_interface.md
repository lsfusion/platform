---
slug: "/User_interface"
title: 'User interface'
---

The platform lets the user adjust the graphical interface to their own preferences — the appearance of the client and the behavior of forms. The chosen settings are saved and reapplied the next time the user logs in, layered on top of the [form design](Form_design.md) and the [navigator design](Navigator_design.md) defined by the developer, and on top of the default values set by the administrator.

By default the settings are stored separately for each user. Some of the appearance settings can be stored for the computer rather than for the user — they then become shared by everyone working at that computer.

### Appearance

Appearance settings are set in the user profile and in the appearance settings form; the color theme and the navigator pinning can also be switched directly from the system toolbar.

| Setting | Values | Purpose |
|---|---|---|
| Color theme | light, dark, system | light or dark interface background; the system theme is taken from the client side |
| Theme | Excel, Classic, Flatly, Lumen, Quartz, Simplex, Sketchy, Yeti | the style of interface elements — fonts, colors, paddings |
| Size | normal, mini, tiny | the scale of interface elements |
| Navigation bar | horizontal, vertical | the placement of the navigation bar |
| Navigator pinning | all, navigation bar only, none | what part of the navigator is pinned (permanently shown) |
| Text wrapping | yes, no | wrapping of long text in table cells |
| Highlighting of duplicates | yes, no | highlighting of repeated values in a column |
| Manual filter applying | yes, no | filters are applied by a separate command rather than immediately on input |
| Mobile mode | yes, no | interface layout for mobile devices |
| Font size | percent | the font size of the interface |
| Colors | color choice | the color of the selected row and cell, the active cell, and the table grid lines |

### Regional settings

The user can set the interface language, country, time zone, and the date and time formats. Each of these values is taken from the client side (the browser or the operating system) if the user allows it; otherwise the value set by the user explicitly is used, then the default value set by the administrator, and finally the server value.

### Form table settings

For each table on a form the user can change the set of displayed columns, their order, width, sorting, captions, and value display format, as well as the row font, the page size, and the header height. These settings are saved personally and override the general table settings and the [form design](Form_design.md).

The user can save their table settings or reset them to the general ones. The administrator can set general settings that apply to all users who have none of their own.
