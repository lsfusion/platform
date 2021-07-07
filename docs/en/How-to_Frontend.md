---
title: 'How-to: Frontend'
---

The easiest way to organize the interaction of a React application with an application based on **lsFusion** is [communication via the HTTP protocol](Access_from_an_external_system.md) through the JSON API. To export a list of objects meeting a specified condition, it is most convenient to use the interface located at the eval/action Url. You can pass lsFusion program code to it in BODY, and it will then be executed. If you need to return data from your request, use the [`EXPORT` operator](EXPORT_operator.md). By default it returns data in JSON format, which is then easily processed using JavaScript.


:::info
In these examples we will make requests without authorization. For the server to start accepting requests without authorization, you need to set the `enableAPI` option in the `Administration > Settings` form on the `Settings` tab to the value `2`.

A safer way would be to create a separate action for each request, with an @@api marker. and set `enableAPI` to the value `0`. In this case, the server will only accept requests for these specific actions and from an authorized user. Inside these actions, before starting processing you can check for availability to the user using the `currentUser()` property.
:::

###  Example 1

For clarity, we implement the simple form from the [Score table](Score_table.md) example (in read-only mode).

We will implement reading flat data set as a function with one parameter – the query text, – to which `EXPORT FROM` is added on the left:
```js
const url = "https://demo.lsfusion.org/hockeystats/eval/action";
function select(script) {
  const params = {
    method: "post",
    body: "EXPORT FROM " + script
  }
  return fetch(url, params).then(response => response.json());
}
```

For example, a request with the following content
```js
select("date(Game g), hostTeamName(g), hostGoals(g), guestGoals(g), guestTeamName(g), resultName(g)")
```

will return JSON of the form
```json
[
   {
      "date":"05.02.19",
      "hostGoals":3,
      "guestTeamName":"New York Rangers",
      "hostTeamName":"Detroit Red Wings",
      "guestGoals":2,
      "resultName":"ПО"
   },
   {
      "date":"13.02.19",
      "hostGoals":2,
      "guestTeamName":"Toronto Maple Leafs",
      "hostTeamName":"Montreal Canadiens",
      "guestGoals":0,
      "resultName":"П"
   },
   {
      "date":"15.02.19",
      "hostGoals":3,
      "guestTeamName":"Montreal Canadiens",
      "hostTeamName":"New York Rangers",
      "guestGoals":5,
      "resultName":"П"
   },
   {
      "date":"17.02.19",
      "hostGoals":2,
      "guestTeamName":"Detroit Red Wings",
      "hostTeamName":"Toronto Maple Leafs",
      "guestGoals":1,
      "resultName":"ПБ"
   }
]
```

It only remains to use this data as a state(s) for React component(s) (for example, using [Material-UI](https://material-ui.com/)):

<iframe src="https://codesandbox.io/embed/wnx876z56k?fontsize=14" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Example 2

Let's consider a slightly more complicated case, when you need to make parameterized queries depending on data selected by the user. For backend logic we'll take the [Material Flow Management](Materials_management.md) example.

Suppose we need to build a form in which we need to show shipments, with the ability to filter by date and warehouse. And when the user selects a specific document, its lines should be displayed.

To implement a request to the backend to receive shipments with filtering, we declare a function:

```js
const url = "https://demo.lsfusion.org/mm/eval/action";
function select(script, data) {
  var formData = new FormData();

  formData.append("script", "EXPORT FROM " + script);
  for (var name in data) {
    formData.append(name, data[name]);
  }
  const params = {
    method: "post",
    headers: {
      "Content-type": "multipart/form-data"
    },
    body: formData
  };
  return fetch(url, params).then(response => response.json());
}
```

The first `select` function makes a POST request with content type multipart/form-data, passing the text of the request to the server as its first parameter and the values of the request as its other parameters.

For example, a function call of the form

```js
select("id = Shipment s, number(s) WHERE date(s) = $1", { date: new Date().toISOString().substr(0, 10) })
```

will return all shipments for today's date. It should be noted that the name of the date parameter is not used in the backend, and can be absolutely anything. Only the order of the parameters is important.

For convenience, we also declare a function that will generate a request by adding to the filter only those parameters whose values are not null:

```js
function selectWhere(script, wheres) {
  var exprs = [], params = {};
  for (var i = 0; i < wheres.length; i++) {
    if (wheres[i].value != null) {
      exprs.push(wheres[i].expr + "=$" + (i + 1));
      params = { ...params, ...{ ["p" + i]: wheres[i].value } };
    }
  }
  return select(script + (exprs.length > 0 ? " WHERE " : "") + exprs.join(" AND "), params);
}
```

The corresponding function call above can be replaced with:
```js
selectWhere("id = Shipment s, number(s)", [{ expr: "date(s)", value : new Date().toISOString().substr(0, 10) }])
```

Using the requests described above, we implement the required logic. To do this, we define two components:

-   `Shipments`, which will display the list of documents using the List component (from Material-UI, as mentioned above). It will also contain a `Filters` component, using which the user will set the parameters for filtering.
-   `Details`, which will display the lines of the selected document. If needed, it could be embedded in the `Shipments` component.

The rest of the code will look like this:

<iframe src="https://codesandbox.io/embed/zxzmmk9n9l?fontsize=14&module=%2Fsrc%2FApp.js" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Example 3

We implement a simple CRUD interface for entering goods.

Since this will require changing the information in the database, we declare a function that will execute arbitrary code on the platform (with error handling):
```js
function handleErrors(response) {
  if (!response.ok) {
    response.text().then(text => console.log(text));
    throw Error(response);
  }
  return response;
}

function evaluate(script, data) {
  var formData = new FormData();


  formData.append("script", script);
  for (var name in data) {
    formData.append(name, data[name]);
  }
  const params = {
    method: "post",
    headers: {
      "Content-type": "multipart/form-data"
    },
    body: formData
  };
  return fetch(url, params).then(handleErrors);
}
```
For example, to create an item in the database you would need to execute the following code:
```js
evaluate("NEW s = Item { name(s) <- $1; barcode(s) <- $2; salePrice(s) <- $3; APPLY; }", { name : "My item", barcode : "4341", salePrice : 2.34 } )
```
As in the examples above, the names of the parameters are not important: their order is what matters. The [`APPLY` operator](APPLY_operator.md) saves changes to the database. Without it, the data will not be saved and will be discarded upon completion of the request.

To change the attributes of the product, you can use the following code (where id is the product's internal ID):
```js
evaluate("FOR Item s = $0 DO { name(s) <- $1; barcode(s) <- $2; salePrice(s) <- $3; APPLY; }", { id : 32494, name : "My item", barcode : "4341", salePrice : 2.34 })
```
To delete a given product:
```js
evaluate("DELETE s WHERE s AS Item = $1; APPLY; ", { id : 32494 })
```
The code implementing the user interface may look like this:

<iframe src="https://codesandbox.io/embed/7rl7n2rj0?fontsize=14&module=%2Fsrc%2FItems.js" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Example 4

In this example, we implement the ability to view and edit a list of documents with lines.

It will be necessary to pass the set of lines from a specific document to the server. So as to perform all the changes with one request, rather than having to make separate requests for each line, we will pass them as a parameter in a JSON-format file. To do this, we modify our `evaluate` function as follows:
```js
function isObject(obj) {
  return obj === Object(obj);
}
function evaluate(script, data) {
  var formData = new FormData();


  formData.append("script", script);
  console.log(data);
  for (var name in data) {
    if (isObject(data[name]))
      formData.append(
        name,
        new Blob([JSON.stringify(data[name])], { type: "application/json" })
      );
    else formData.append(name, data[name]);
  }
  const params = {
    method: "post",
    headers: {
      "Content-type": "multipart/form-data"
    },
    body: formData
  };
  return fetch(url, params).then(handleErrors);
}
```
If an object is passed as a field in the `data` object, it is now converted to JSON and passed as a file of type application/json.

In order to accept this file as the final parameter (`$5` in this example) and update the lines by deleting the old ones and creating new ones, we will use the following code on the platform:

```lsf
IMPORT JSON FROM $5 AS FILE FIELDS LONG item, NUMERIC[16,3] quantity, NUMERIC[16,3] price, NUMERIC[16,3] sum DO 
    NEW d = ShipmentDetail { 
        shipment(d) <- s; 
        item(d) <- GROUP MAX i AS Item IF LONG(i) = item; 
        quantity(d) <- quantity; 
        price(d) <- price; 
        sum(d) <- sum; 
    }
```
To parse the JSON we use the special [`IMPORT` operator](IMPORT_operator.md). It assumes that it will receive a flat JSON as an array of objects with numerical fields `item`, `quantity`, `price` and `sum`. A `ShipmentDetail` will be created for each object. It is then linked to the corresponding object `s` of class `Shipment`. After this the SKU with the internal code equals to the passed one is written, and then the other properties are written. Parsing of more complex JSON can be found in examples [How-to: Data import](How-to_Data_import.md) and [How-to: Interaction via HTTP protocol](How-to_Interaction_via_HTTP_protocol.md).

Saving changes may violate a [constraint](Constraints.md). In this case, the changes will not be saved to the database (however, they will remain in the [change session](Change_sessions.md)). In this event the value `TRUE` will be written to the `canceled` property, and a constraint message will be written to the `applyMessage` property. In order to handle this situation, we will use the following code on the platform:
```lsf
APPLY; 
IF canceled() THEN 
    EXPORT FROM message = applyMessage();
```

If the changes fail the message is returned in the BODY of the response in JSON form with a single field `applyMessage`.

The final code that is passed to `evaluate` for, e.g., creating a document will look like this:
```lsf
NEW s = Shipment {
    number(s) <- $1; date(s) <- $2; customer(s) <- $3; stock(s) <- $4;
    IMPORT JSON FROM $5 AS FILE FIELDS LONG item, NUMERIC[16,3] quantity, NUMERIC[16,3] price, NUMERIC[16,3] sum DO 
        NEW d = ShipmentDetail { 
            shipment(d) <- s; 
            item(d) <- GROUP MAX i AS Item IF LONG(i) = item; 
            quantity(d) <- quantity; 
            price(d) <- price; 
            sum(d) <- sum; 
        }
    APPLY; 
    IF canceled() THEN 
        EXPORT FROM message = applyMessage();
}
```
For editing (first, existing lines are deleted), accordingly:
```lsf
FOR Shipment s = $0 DO {
    number(s) <- $1; date(s) <- $2; customer(s) <- $3; stock(s) <- $4;
    DELETE ShipmentDetail d WHERE shipment(d) = s;
    IMPORT JSON FROM $5 AS FILE FIELDS LONG item, NUMERIC[16,3] quantity, NUMERIC[16,3] price, NUMERIC[16,3] sum DO 
        NEW d = ShipmentDetail { 
            shipment(d) <- s; 
            item(d) <- GROUP MAX i AS Item IF LONG(i) = item; 
            quantity(d) <- quantity; 
            price(d) <- price; 
            sum(d) <- sum; 
        }
    APPLY; 
    IF canceled() THEN 
        EXPORT FROM message = applyMessage();
}
```
To handle the constraint violation error (since the response status will be 200 in this implementation in both cases), we assume that if the BODY of the answer is empty then there is no error. If there is any text, then this is the JSON containing the text of the constraint. For example, we can use the following code:
```js
evaluate("...").then(response =>
    response.text().then(text => {
        if (text === "") {
            // ok
        } else {
            console.log(JSON.parse(text).applyMessage);
        }
      })
    );
```
The implementation of the entire task may look as follows:

<iframe src="https://codesandbox.io/embed/suspicious-shockley-r16kv?fontsize=14" title="suspicious-shockley-r16kv" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Example 5

By analogy with [**Example 1**](#example-1), we implement the Score table form with editing ability using the [Form API](Access_from_an_external_system.md#form). To use it, you need to link the [@lsfusion/core](https://www.npmjs.com/package/@lsfusion/core) library.

First of all, you need to initialize the form using the `create` function after loading the main component:
```js
componentDidMount() {
  create(updateState => this.setState(updateState), url, {
    name: "MainForm"
  });
}
```
The first parameter passed is the callback function to which the initial state of the form will be passed after the response from the server:
```json
{
   "game":{
      "list":[
         {
            "date":"05.02.19",
            "hostGoals":3,
            "guestTeamName":"New York Rangers",
            "hostTeamName":"Detroit Red Wings",
            "guestGoals":2,
            "value":6054,
            "resultName":"ПО"
         },
         {
            "date":"13.02.19",
            "hostGoals":2,
            "guestTeamName":"Toronto Maple Leafs",
            "hostTeamName":"Montreal Canadiens",
            "guestGoals":0,
            "value":6063,
            "resultName":"П"
         },
         {
            "date":"15.02.19",
            "hostGoals":3,
            "guestTeamName":"Montreal Canadiens",
            "hostTeamName":"New York Rangers",
            "guestGoals":5,
            "value":6072,
            "resultName":"П"
         },
         {
            "date":"17.02.19",
            "hostGoals":2,
            "guestTeamName":"Detroit Red Wings",
            "hostTeamName":"Toronto Maple Leafs",
            "guestGoals":1,
            "value":6075,
            "resultName":"ПБ"
         }
      ],
      "value":6054
   },
   "team":{
      "list":[
         {
            "gamesLostSO":0,
            "goalsConceded":3,
            "gamesLostOT":0,
            "goalsScored":7,
            "gamesWon":2,
            "points":6,
            "gamesWonOT":0,
            "gamesLost":0,
            "gamesPlayed":2,
            "name":"Montreal Canadiens",
            "gamesWonSO":0,
            "place":1,
            "value":6064
         },
         {
            "gamesLostSO":1,
            "goalsConceded":4,
            "gamesLostOT":0,
            "goalsScored":4,
            "gamesWon":0,
            "points":3,
            "gamesWonOT":1,
            "gamesLost":0,
            "gamesPlayed":2,
            "name":"Detroit Red Wings",
            "gamesWonSO":0,
            "place":2,
            "value":6057
         },
         {
            "gamesLostSO":0,
            "goalsConceded":3,
            "gamesLostOT":0,
            "goalsScored":2,
            "gamesWon":0,
            "points":2,
            "gamesWonOT":0,
            "gamesLost":1,
            "gamesPlayed":2,
            "name":"Toronto Maple Leafs",
            "gamesWonSO":1,
            "place":3,
            "value":10993
         },
         {
            "gamesLostSO":0,
            "goalsConceded":8,
            "gamesLostOT":1,
            "goalsScored":5,
            "gamesWon":0,
            "points":1,
            "gamesWonOT":0,
            "gamesLost":1,
            "gamesPlayed":2,
            "name":"New York Rangers",
            "gamesWonSO":0,
            "place":4,
            "value":6061
         }
      ],
      "value":6064
   }
}
```

The returned JSON also has other utility fields.

Object tables can be read from `game.list` and `team.list` respectively. Current objects are stored in `game.value` and `team.value`. We use this data to form two tables, of games and of teams. To mark the current row we use the equals function, which compares, for example, the values from `game.value` and `game.list[<row>].value`. This is necessary because `value` may contain more complex objects, if several objects are declared in the object group.

Until the form has loaded, the helper function `formCreated` returns `false` and tables are not displayed.
```js
if (!formCreated(this.state)) return <div>Loading</div>;
```
The `numberOfPendingRequests` function also lets us display on the form the number of requests to the server for which no response has yet been received.
```js
<div>
{numberOfPendingRequests(this.state) > 0
    ? "Loading, number of requests : " +
    numberOfPendingRequests(this.state)
    : ""}
</div>
```
Changing the current object on the form and the values of its properties is done using the `change` function.
```js
change(updateState => this.setState(updateState), changes);
```
It takes a callback that will change the current state and an object that stores a list of changes. The current state of the form (the App object) is constantly updated after each call to `change` with new values, taking into account changes made earlier by the user.

For example, to change the currently selected game we can use the following call:
```js
change(updateState => this.setState(updateState), { game : { value : 6063 } });
```
Here 6063 is the `value` of the selected object.

To change the value of a property, we can use the following code:
```js
change(updateState => this.setState(updateState), { game : { value : 6063, hostGoals : 3 } });
```
This call changes the current object to the game with the ID 6063 and the value of the host team's goals to the passed value (3). If you do not pass the `value` tag, then the goal change will be made for the previously selected game.

The same scheme is used to change properties that are not data, but are a simple [composition](Composition_JOIN.md) of object properties: 
```js
change(updateState => this.setState(updateState), { game : { value : 6063, hostTeamName: "Montreal Canadiens" } });
```
The system itself will find the team with the given name and change the value of the host team to the one it finds.

To select a team from the list, you can use the [react-select](https://github.com/JedWatson/react-select) component. Reading of many teams can be organized using the `select` function declared in the first example.

The following shows the fully working source code:

<iframe src="https://codesandbox.io/embed/vibrant-tharp-trcqt?fontsize=14" title="vibrant-tharp-trcqt" allow="geolocation; microphone; camera; midi; vr; accelerometer; gyroscope; payment; ambient-light-sensor; encrypted-media" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>
