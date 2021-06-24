---
title: 'How-to: Фронтенд'
---

Наиболее простым способом организации взаимодействия React приложения с приложением на базе **lsFusion** является [общение через HTTP-протокол](Access_from_an_external_system.md) посредством JSON API. Для выгрузки списка объектов по заданному условию удобнее всего использовать интерфейс, который находится по Url'у eval/action. На него в BODY можно передать программный код lsFusion, который будет выполнен. При необходимости вернуть данные по запросу необходимо использовать [оператор `EXPORT`](EXPORT_operator.md). По умолчанию, он возвращает данные в формате JSON, которые затем легко обрабатываются при помощи JavaScript.


:::info
В данных примерах мы будем делать запросы без авторизации. Для того, чтобы сервер начал принимать запросы без авторизации, в форме `Администрирование > Настройки` на вкладке `Параметры` нужно установить опцию `enableAPI` в значение `2`.

Более безопасным способом будет создание для каждого запроса отдельного действия с пометкой @@api и установкой `enableAPI` в значение `0`. В таком случае, сервером будут приниматься запросы только к этим действиям и при авторизованном пользователе. Внутри этих действий перед началом обработки можно проверять на доступность его пользователю, которого можно получить при помощи свойства `currentUser()`.
:::

###  Пример 1

Для наглядности реализуем простую форму из примера [Турнирная таблица](Score_table.md) (без возможности редактирования).

Оформим считывание плоского набора данных при помощи функции с одним параметром : текстом запроса, к которому добавляется слева `EXPORT FROM`:
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

Например, запрос следующего содержания
```js
select("date(Game g), hostTeamName(g), hostGoals(g), guestGoals(g), guestTeamName(g), resultName(g)")
```
вернет JSON вида

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
Остается только подложить эти данные в качестве состояний для React компонент (например, с использованием [Material-UI](https://material-ui.com/)) :

<iframe src="https://codesandbox.io/embed/wnx876z56k?fontsize=14" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Пример 2

Рассмотрим немного более сложный случай, когда нужно делать параметризованные запросы, в зависимости от выбранных пользователем данных. В качестве логики backend'а возьмем пример [Управление материальными потоками](Materials_management.md).

Предположим, что нужно построить форму, в которой нужно показать расходные документы, с возможностью фильтровать по дате и складу. При этом, когда пользователь выбирает конкретный документ, должны отобразиться его строки.

Для реализации запроса к backend'у по получению расходных документов с фильтрацией объявим функцию:
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

Первая функция `select` будет делать POST запрос с типом содержимого multipart/form-data, передавая первым параметром на сервер текст запроса, а остальными параметрами - значения запроса.

Например, вызов функции вида
```js
select("id = Shipment s, number(s) WHERE date(s) = $1", { date: new Date().toISOString().substr(0, 10) })
```
вернет все расходные документы за сегодняшнее число. Следует отметить, что название параметра date не используется на backend'е и может быть абсолютно любым. Важен только порядок следования параметров.

Для удобства также объявим функцию, которая будет формировать запрос, добавляя в фильтр только те параметры, значения которых не являются `null`:
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
Соответствующий вызов функции выше может быть заменен на:
```js
selectWhere("id = Shipment s, number(s)", [{ expr: "date(s)", value : new Date().toISOString().substr(0, 10) }])
```
Используя описанные выше запросы реализуем требуемую логику. Для этого определим две компоненты:

-   `Shipments`, которая будет отображать при помощи компонента List (из упомянутого Material-UI) список документов. Также в ней будет содержаться компонент `Filters`, при помощи которого пользователь будет задавать параметры для фильтрации.
-   `Details`, который будет отображать строки выбранного документа. При желании его можно было бы вложить в компонент `Shipments`.

Остальной код будет выглядеть следующим образом:

<iframe src="https://codesandbox.io/embed/zxzmmk9n9l?fontsize=14&module=%2Fsrc%2FApp.js" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Пример 3

Реализуем простой CRUD интерфейс для ввода товаров.

Так как для этого понадобится изменять информацию в базе данных, то объявим функцию, которая будет выполнять произвольный код на платформе (с обработкой ошибок):
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

Например, для создания товара в базе данных нужно будет выполнить следующий код :
```js
evaluate("NEW s = Item { name(s) <- $1; barcode(s) <- $2; salePrice(s) <- $3; APPLY; }", { name : "My item", barcode : "4341", salePrice : 2.34 } )
```
Как и в примерах выше, названия параметров не важны, а важен их порядок. [Оператор `APPLY`](APPLY_operator.md) сохраняет изменения в базу данных. Без него данные сохранены не будут и сбросятся по завершению выполнения запрос.

Для изменения атрибутов товара можно использовать следующий код (где `id` - это внутренний идентификатор товара) :
```js
evaluate("FOR Item s = $0 DO { name(s) <- $1; barcode(s) <- $2; salePrice(s) <- $3; APPLY; }", { id : 32494, name : "My item", barcode : "4341", salePrice : 2.34 })
```
Для удаления заданного товара:
```js
evaluate("DELETE s WHERE s AS Item = $1; APPLY; ", { id : 32494 })
```
Код по реализации непосредственно пользовательского интерфейса может выглядеть следующим образом:

<iframe src="https://codesandbox.io/embed/7rl7n2rj0?fontsize=14&module=%2Fsrc%2FItems.js" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Пример 4

В этом примере реализуем возможность просмотра и редактирования списка документов со строками.

Для этого необходимо будет передавать на сервер набор строк конкретного документа. Чтобы не делать отдельные запросы для каждых строк и производить все изменения одним запросом, будем передавать их параметром в JSON формате в виде файла. Для этого модифицируем нашу функцию `evaluate` следующим образом :
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
Теперь он смотрит, что если в объекте `data` в качестве поля передается объект, то он преобразуется в JSON и передается как файл с типом application/json.

Для того, чтобы принять этот файл последним параметром (в данном примере `$5`) и обновить строки путем удаления старых и создания новых, будем использовать следующий код на платформе :
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
Для разбора JSON используем специальный [оператор `IMPORT`](IMPORT_operator.md). Он предполагает, что ему на вход будет плоский JSON в виде массива объектов с числовыми полями `item`, `quantity`, `price` и `sum`. Для каждого объекта в нем будет создан свой `ShipmentDetail`. Затем он привязывается к соответствующему объекту `s` класса `Shipment`. После этого проставляется товар, у которого внутренний код совпадает с переданным, и остальные свойства. Разбор более сложных JSON можно найти в примерах [How-to: Импорт данных](How-to_Data_import.md) и [How-to: Взаимодействие через HTTP-протокол](How-to_Interaction_via_HTTP_protocol.md).

При сохранении изменений может нарушиться [ограничение](Constraints.md). В этом случае изменения не будут сохранены в базу данных (при этом они останутся в [сессии изменений](Change_sessions.md)). При этом в свойство `canceled` будет записано `TRUE`, а в свойство `applyMessage` сообщение ограничения. Для того, чтобы обработать эту ситуацию будем использовать следующий код на платформе:
```lsf
    APPLY; 
    IF canceled() THEN 
        EXPORT FROM message = applyMessage();
```

Если изменения не прошли, то сообщение возвращается в BODY ответа в виде JSON с единственным полем `applyMessage`.

Общий код, который передается в `evaluate` для, например, создания документа будет выглядеть следующим образом :
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

Для редактирования (в нем предварительно удаляются уже существующие строки), соответственно:
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

Для того, чтобы обработать ошибку о нарушении ограничения (так как статус ответа в обоих случаях в данной реализации будет 200), будем считать, что если BODY ответа пустое, то ошибки нету. Если есть какой-то текст, то это JSON с текстом ограничения. Например, можно использовать следующий код:
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

Реализация всей задачи может выглядеть следующим образом :

<iframe src="https://codesandbox.io/embed/suspicious-shockley-r16kv?fontsize=14" title="suspicious-shockley-r16kv" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>

### Пример 5

По аналогии с [**Примером 1**](#пример-1) реализуем форму Турнирная таблица с возможностью редактирования при помощи [Form API](Access_from_an_external_system.md#form). Для его использования требуется подключить библиотеку [@lsfusion/core](https://www.npmjs.com/package/@lsfusion/core).

В первую очередь нужно инициализировать форму при помощи функции `create` после загрузке основного компонента :
```js
componentDidMount() {
  create(updateState => this.setState(updateState), url, {
    name: "MainForm"
  });
}
```

Первым параметром передается callback функция, в которую после ответа с сервера будет передано начальное состояние формы:
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

В возвращаемом JSON есть также другие служебные поля.

Таблицы объектов можно считывать из `game.list` и `team.list` соответственно. Текущие объекты хранятся в `game.value` и `team.value`. Эти данные мы используем для формирования двух таблиц с играми и командами. Для отметки текущего ряда используется функция equals, которая сравнивает, например, значения из `game.value` и `game.list[<row>].value`. Это нужно, так как в `value` могут быть более сложные объекты, если в группе объектов объявлено несколько объектов.

До тех пор, пока форма не загружена вспомогательная функция `formCreated` возвращает `false`, и таблицы не отображаются.
```js
if (!formCreated(this.state)) return <div>Loading</div>;
```
Также при помощи функции `numberOfPendingRequests` мы выводим на форму количество запросов на сервер, на которые не получен еще ответ.
```js
<div>
{numberOfPendingRequests(this.state) > 0
    ? "Loading, number of requests : " +
    numberOfPendingRequests(this.state)
    : ""}
</div>
```
Изменение текущего объекта на форме и значений свойств осуществляется при помощи функции `change`.
```js
change(updateState => this.setState(updateState), changes);
```
Она принимает на вход callback, который изменит текущее состояние и объект, хранящий список изменений. При таком использовании, после каждого вызова `change` текущее состояние формы (объект App) будет постоянно обновляться новыми значениями с учетом сделанных ранее пользователем изменений.

Например, для изменения текущей выбранной игры можно использовать следующий вызов :
```js
change(updateState => this.setState(updateState), { game : { value : 6063 } });
```
Здесь 6063 - это `value` выбираемого объекта.

Для изменения значения свойства можно использовать следующий код:

    change(updateState => this.setState(updateState), { game : { value : 6063, hostGoals : 3 } });

Этот вызов изменит текущий объект на игру с идентификатором 6063 и значение голов хозяев на переданное значение (3), Если не передавать тэг `value`, то изменение голов будет сделано для ранее выбранной игры.

Такая же схема используется для изменений свойств, которые не являются первичными, а являются простой [композицией](Composition_JOIN.md) от объектных свойств: 
```js
change(updateState => this.setState(updateState), { game : { value : 6063, hostTeamName: "Montreal Canadiens" } });
```
Система сама найдет команду с заданным именем и изменит значение команды хозяйки на найденное.

Чтобы сделать выбор команды из списка можно использовать компонент [react-select](https://github.com/JedWatson/react-select). Чтение множества команд можно организовать при помощи объявленной в первом примере функции `select`.

Ниже показан полностью работающий исходный код:

<iframe src="https://codesandbox.io/embed/vibrant-tharp-trcqt?fontsize=14" title="vibrant-tharp-trcqt" allow="geolocation; microphone; camera; midi; vr; accelerometer; gyroscope; payment; ambient-light-sensor; encrypted-media" width="100%" height="500px" border="0" border-radius="4px" overflow="hidden" sandbox="allow-modals allow-forms allow-popups allow-scripts allow-same-origin"></iframe>
