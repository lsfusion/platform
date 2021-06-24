---
title: 'Navigator design'
---

*Navigator design* determines how the navigator is displayed to the user on the screen of his device (we will call the screen space on which the navigator is displayed the *desktop*).

The navigator design consists of multiple *windows*, desktop components, each of which displays certain [navigator](Navigator.md) elements. 

Each window must have a *caption* which will be used when displaying the window in the user interface.

### Navigator elements layout in windows

For each navigator element, you can specify the window in which its descendants should be displayed. If necessary, the element itself can be displayed there. Thus, the set of *subtrees* (navigator *elements*) that is displayed in each window is uniquely determined. Graphically, this can be represented as follows:


import ThemedImage from '@theme/ThemedImage';

<ThemedImage
    alt="Docusaurus themed image"
    sources={{
        light: require('./images/Navigator_design.png').default,
        dark: require('./images/Navigator_design_dark.png').default,
    }}
/>

### Window layout on the desktop

Each window occupies a predefined section of the desktop. Graphically, this can be represented as follows:

<ThemedImage
    alt="Docusaurus themed image"
    sources={{
        light: require('./images/Navigator_design_light_En.png').default,
        dark: require('./images/Navigator_design_dark_En.png').default,
    }}
/>

The entire desktop is `100 x 100` *pixels* in size. When creating a window, you must specify the window's upper left coordinate, width and height, expressed in *pixels*. It is desirable that windows should "cover" the entire area of the desktop. If this does not happen, then the free area will be given to one of the windows (there is no guarantee as to which one). Two windows are allowed to have absolutely identical coordinates and sizes. In this case they will be displayed in the same place, but switching between them will be possible using tabs.

### Selected folder {#selectedfolder}

At any moment in time in each window there can be one current *user-selected* navigator folder. Accordingly, if the element item belongs to a window other than the window of its parent folder, then this element is shown in its window if and only if its parent folder is selected in its window. The predefined `System.root` folder is always considered to be selected. If at some point a window does not display any navigator element, then that window is automatically hidden.

### Types

There are several window *types* that determine which component in the interface will be used to display navigator elements.

-   *Toolbar*: a container consisting of buttons, each of which corresponds to one element of the navigator. The vertical toolbar places all buttons from top to bottom, indenting each element from the left depending on its position in the tree. The horizontal toolbar shows the buttons from left to right, without indentation.
-   *Panel*: a container in which the hierarchy of components corresponds to navigator elements, where for [forms](Forms.md) and *[actions](Actions.md)* buttons are created, and for navigator folders – nested containers with the corresponding caption. The vertical panel places all the nested containers and their buttons from top to bottom, and the horizontal panel places them from left to right.
-   *Tree*: a tree in which each node corresponds to a navigator element.
-   *Menu*: a menu in which a popup menu corresponds to each navigator folder, and the items on that menu correspond to the forms and actions.

By default, a UI component that displays navigator elements is wrapped into scrollbars. They appear when the component does not fit in the window. If necessary, this behavior can be disabled.

### System windows

There are several predefined system windows that are necessary for the client application to work:

-   `forms`: a window in which user forms open.
-   `log`: a window in which messages to the user are displayed. If this window is invisible, messages will be shown to the user in the form of system dialog forms.
-   `status`: a window in which various system information is displayed.

Also, three additional windows are automatically created for ease of development:

-   `root`: a horizontal toolbar in which it is recommended to display navigator element `root` children. The navigator folder `System.root` is displayed here by default.
-   `toolbar`: a vertical toolbar in which it is recommended to display some of the descendants of the navigator elements that are displayed in the `root` window.
-   `tree`: a tree in which it is recommended to display some of the descendants of the navigator elements displayed in the `root` window

### Default layout

By default, the desktop has the following layout (the left coordinate, upper coordinate, width, and height are indicated in brackets):


<ThemedImage
    alt="Docusaurus themed image"
    sources={{
        light: require('./images/Navigator_design_default.png').default,
        dark: require('./images/Navigator_design_default_dark.png').default,
    }}
/>

### Language

To manage windows, use the [`WINDOW` statement](WINDOW_statement.md).

### Examples

```lsf
// creating system windows in the System module
WINDOW root 'Root' TOOLBAR HIDETITLE HIDESCROLLBARS HORIZONTAL POSITION(0, 0, 100, 6);
WINDOW toolbar 'Toolbar' TOOLBAR HIDETITLE VERTICAL POSITION(0, 6, 20, 64);
WINDOW tree 'Tree' TOOLBAR HIDETITLE POSITION(0, 6, 20, 64);

// menu without scrollbars under the root window
WINDOW menu MENU HIDESCROLLBARS DRAWROOT POSITION(20, 6, 80, 4);

// a horizontal toolbar at the bottom of the desktop, in which all buttons will be centered and text will be aligned up
// in this toolbar, for example, it is possible to place forms for quick opening
WINDOW hotforms TOOLBAR HORIZONTAL VALIGN(CENTER) TEXTVALIGN(START) BOTTOM;
```

  
