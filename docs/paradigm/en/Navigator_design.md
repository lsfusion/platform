---
slug: "/Navigator_design"
title: 'Navigator design'
---

*Navigator design* determines how the navigator is displayed to the user on the screen of his device (we will call the screen space on which the navigator is displayed the *desktop*).

The navigator design consists of multiple *windows*, desktop components, each of which displays certain [navigator](Navigator.md) elements. A window, and the layout of the windows on the desktop, can also be drawn by the application's client code instead of the platform: these are [custom views](Custom_views.md).

Each window must have a *caption* which will be used when displaying the window in the user interface.

### Navigator elements layout in windows

For each navigator element, you can specify the window in which its descendants should be displayed. If necessary, the element itself can be displayed there. Thus, the set of *subtrees* (navigator *elements*) that is displayed in each window is uniquely determined. Graphically, this can be represented as follows:


import ThemedImage from '@theme/ThemedImage';

<ThemedImage
    alt="Docusaurus themed image"
    sources={{
        light: require('../images/Navigator_design.png').default,
        dark: require('../images/Navigator_design_dark.png').default,
    }}
/>

### Window layout on the desktop

Each window occupies a predefined section of the desktop. Graphically, this can be represented as follows:

<ThemedImage
    alt="Docusaurus themed image"
    sources={{
        light: require('../images/Navigator_design_light_En.png').default,
        dark: require('../images/Navigator_design_dark_En.png').default,
    }}
/>

The entire desktop is `100 x 100` *points* in size. When creating a window, you must specify the window's upper left coordinate, width and height, expressed in *points*. It is desirable that windows should "cover" the entire area of the desktop. If this does not happen, then the free area will be given to one of the windows (there is no guarantee as to which one). Two windows are allowed to have absolutely identical coordinates and sizes. In this case they will be displayed in the same place, but switching between them will be possible using tabs.

This is the standard layout. The windows can be laid out by a React component instead: it decides where each window goes and how large it is, and the content of each window is drawn as without it, in the place the component gives the window. The coordinates and sizes of the windows are then not used.

### Selected folder {#selectedfolder}

At any moment in time in each window there can be one current *user-selected* navigator folder. Accordingly, if the element item belongs to a window other than the window of its parent folder, then this element is shown in its window if and only if its parent folder is selected in its window. The predefined `System.root` folder is always considered to be selected. If at some point a window does not display any navigator element, then that window is automatically hidden. A window drawn by the application's client code is not hidden this way: it is drawn with no elements too.

A React component drawing a window can select a navigator element as the user does, including a folder, an action or a form.

### System windows {#systemwindows}

There are several predefined system windows that are necessary for the client application to work:

-   `System.forms`: a window in which user forms open. It draws them as tabs. An application can declare windows of its own that hold forms: a form is opened into such a window when its [opening](In_an_interactive_view_SHOW_DIALOG.md#location) names the window, which is how a form is drawn as a header or a side panel of the application. Such a window draws one form at a time, unless it is declared to draw its forms as tabs or is drawn by a React component, which places them as it chooses. A form it places nowhere stays open and hidden.
-   `System.log`: a window in which messages to the user are displayed. If this window is invisible, its messages are logged and shown nowhere.

Also, additional windows are automatically created for ease of development:

-   `System.root`: a horizontal toolbar in which it is recommended to display navigator element `System.root` children. The navigator folder `System.root` is displayed here by default.
-   `System.toolbar`: a vertical toolbar in which it is recommended to display some of the descendants of the navigator elements that are displayed in the `System.root` window.
-   `System.system`: a toolbar with system buttons providing convenient access to the main interface functions.
-   `System.logo`: a window for displaying the logo.

### Default layout

By default, the desktop has the following layout (the left coordinate, upper coordinate, width, and height are indicated in brackets):


<ThemedImage
    alt="Docusaurus themed image"
    sources={{
        light: require('../images/Navigator_design_default.png').default,
        dark: require('../images/Navigator_design_default_dark.png').default,
    }}
/>

### Language

To manage windows, use the [`WINDOW` statement](../language/WINDOW_statement.md). A React component is given the layout of the windows with the `CUSTOM` option of the [`NAVIGATOR` statement](../language/NAVIGATOR_statement.md).

### React {#react}

- [Windows in React](../language/React_windows.md) describes how a React component draws a navigator window or the log window, or lays out the windows on the desktop in place of the standard layout.
- [The navigator controller](../language/Form_and_navigator_controllers.md#navigator-controller) shows how a React component drawing a window selects a navigator element as the user does.
- [The forms window](../language/React_windows.md#forms-window) explains how a React component draws a forms window and places the forms opened in it.

### Examples

```lsf
// creating system windows in the System module
WINDOW logo HORIZONTAL POSITION(0, 0, 10, 6) VALIGN(CENTER) HALIGN(START) HIDETITLE HIDESCROLLBARS CLASS logoWindowClass();
WINDOW root HORIZONTAL POSITION(10, 0, 70, 6) VALIGN(CENTER) HALIGN(CENTER) HIDETITLE HIDESCROLLBARS CLASS rootWindowClass();
WINDOW system HORIZONTAL POSITION(80, 0, 20, 6) VALIGN(CENTER) HALIGN(END) HIDETITLE HIDESCROLLBARS CLASS systemWindowClass();

WINDOW toolbar VERTICAL POSITION(0, 6, 20, 94) HIDETITLE CLASS toolbarWindowClass();

// a horizontal toolbar at the bottom of the desktop, in which all buttons will be centered and text will be aligned up
// in this toolbar, for example, it is possible to place forms for quick opening
WINDOW hotforms HORIZONTAL BOTTOM VALIGN(CENTER) TEXTVALIGN(START);
```

In the desktop web client, a React component draws the `toolbar` window instead of the platform, a button for each navigator element of the window that is not hidden:

```lsf
EXTEND WINDOW System.toolbar CUSTOM 'AppMenu';
```

```jsx
const { Caption } = window.lsfusion;

export function AppMenu({ data, controller }) {
    const item = name => {
        const e = data.byName[name];
        return !e.hidden && <li key={name}>
            <button onClick={ev => controller.activate(name, ev)}><Caption value={e.caption}/></button>
            <ul>{e.children.map(item)}</ul>
        </li>;
    };
    return <nav><ul>{data.root.map(item)}</ul></nav>;
}
```

  
