---
slug: "/How-to_Custom_components_objects"
title: 'How-to: Custom Components (objects)'
---

This article describes classic JavaScript views for existing code. For a new view, use a React component in a `custom` container or registered as `reactView` for `CUSTOM` ([React views](../language/React_views.md)).

By default, each object on a form with GRID view is displayed on the form as a flat table with columns.
However, it is possible to create your own components in the platform to visualize the list of objects.

As an illustrative example, let's consider the task of displaying a list of products with images in the form of "tiles".

### Domain Logic

First let's create classes and properties of items, as well as edit form:

```lsf
CLASS Item 'Item';

name 'Name' = DATA STRING (Item) NONULL;
price 'Price' = DATA NUMERIC[12,2] (Item) NONULL;
picture '' = DATA IMAGEFILE (Item);

FORM item 'Item'
    OBJECTS i = Item PANEL
    PROPERTIES(i) name, price, picture
    
    EDIT Item OBJECT i
;

DESIGN item {
    OBJECTS {
        MOVE PROPERTY(picture(i)) {
            fill = 1;
        }
    }
}
```
The name, price and image must be specified for each item.

### Interface

Let's create a form with a list of items. To do this let's add to the form an object _Item_, its properties, and actions to add, edit, and delete:
```lsf
FORM items 'Items'
    OBJECTS i = Item CUSTOM 'itemCards'
    PROPERTIES(i) READONLY picture, price, name
    PROPERTIES(i) NEWSESSION new = NEW, edit = EDIT GRID, DELETE GRID
;

NAVIGATOR {
    NEW items;
}
```
The keyword **CUSTOM** specifies that not the standard tabular interface should be used to draw the list of items,
but the components created by the function _itemCards_. Let's declare this function in the file _itemcards.js_, which we'll place in the folder _resources/web_. This is the no-build path — a plain `.js` file, no JSX or bundling. See [Client JavaScript modules](../language/Client_JavaScript_modules.md#without-the-build) for where custom JS goes and for the with-build alternative.
It will return an object consisting of two functions: _render_ and _update_ ([The render function](../language/Render_functions.md#render-function)).

Let's write the _render_ function:
```js
render: (element, controller) => {
    let cards = document.createElement("div")
    cards.classList.add("item-cards");

    element.cards = cards;
    element.appendChild(cards);
},
```
In this example we create a new _div_ _cards_, remember it, and append it to _element_.

Let's write the _update_ function:
```js
update: (element, controller, list) => {
    while (element.cards.lastElementChild) {
        element.cards.removeChild(element.cards.lastElementChild);
    }

    for (let item of list) {
        let card = document.createElement("div")
        card.classList.add("item-card");

        if (controller.isCurrent(item))
            card.classList.add("item-card-current");

        let cardImage = document.createElement("img")
        cardImage.classList.add("item-card-image");
        cardImage.src = item.picture;
        card.appendChild(cardImage);

        let cardPrice = document.createElement("div")
        cardPrice.classList.add("item-card-price");
        cardPrice.innerHTML = item.price;
        card.appendChild(cardPrice);

        let cardName = document.createElement("div")
        cardName.classList.add("item-card-name");
        cardName.innerHTML = item.name;
        card.appendChild(cardName);

        element.cards.appendChild(card);

        card.onclick = function(event) {
            if (!controller.isCurrent(item)) controller.changeObject(item);
        }
        card.ondblclick = function(event) {
            controller.changeProperty('edit', item);
        }
    }
}
```
Because the _update_ function is called whenever the data changes, the first thing that happens is that all previously created elements (namely, item cards) are deleted.

This example uses the simplest update scheme, but if necessary, it can be optimized by updating the DOM only for changed values with the controller's [_diff_](../language/Render_functions.md#diff) method.

After removing the old elements, for each object in the _list_ array a _div_ _card_ is created, in which the desired display elements of each property are placed ([An object group's rows](../language/Render_functions.md#object-group)).

At the very end of the function, mouse click handlers are added to the item card. 

On a single click _changeObject_ method is called on the controller, which changes the current object.
The second parameter (_rendered_) is not specified, so the server will eventually call the _update_ function again ([An object group's controller](../language/Render_functions.md#controller-methods)).
Since the value of the _isCurrent_ method will change, re-creating the item cards will change the currently selected object in the interface.

On double-click, the _changeProperty_ method is called, which changes the current value of the _edit_ property for the object passed in the second parameter.
Since _edit_ is an action, the third parameter, the value by which the current value of the property should be changed, is not passed, and the action will be called instead of the change.
In this case the item editing form will be opened.

To combine the _render_ and _update_ functions into one, _itemCards_ function is created, which returns them within the same object:
```js
function itemCards() {
    return {
        render: (element, controller) => {
            ...
        },
        update: (element, controller, list) => {
            ...
        }
    }
}
```

To complete the design setup, create a file _itemcards.css_, which we will also place in the _resources/web_ folder:
```css
.item-cards {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
    grid-auto-rows: 200px;
    grid-gap: 10px;
}

.item-card {
    cursor: pointer;
    display: flex;
    flex-direction: column;
    overflow: hidden;
    align-items: center;
    padding: 8px;
}
.item-card-current {
    background-color: lightblue;
}

.item-card-image {
    flex: 1;
    min-height: 100px;
}

.item-card-price {
    font-weight: bold;
}

.item-card-name {
    color: gray;
}
```

In order to load created js and css files when the page opens in the browser, you must add their initialization to the action _onWebClientInit_  by adding the file name to the _onWebClientInit(STRING)_ property. A numeric value is needed to specify the order of loading:
```lsf
onWebClientInit() + {
    onWebClientInit('itemcards.js') <- 1;
    onWebClientInit('itemcards.css') <- 2;
}
```

The resulting form will look like this:
![](../images/How-to_Custom_components_objects.png)
