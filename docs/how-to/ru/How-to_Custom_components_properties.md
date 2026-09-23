---
slug: "/How-to_Custom_components_properties"
title: 'How-to: Пользовательские компоненты (свойства)'
---

Для каждого типа [свойства](../paradigm/Properties.md) по умолчанию используется свой предопределенный визуальный компонент для отображения и 
редактирования данных. Однако, существует возможность переопределять компоненты на свои собственные, создаваемые при 
помощи JavaScript. Эта функциональность поддерживается только в веб-клиенте.

Рассмотрим задачу по созданию чата для общения между пользователями с целью демонстрации этой возможности.

### Доменная логика

Для начала создадим доменную логику, в которой определена сущность _Сообщение_. 
Каждое сообщение содержит плоский текст, а также информацию об авторе и времени отправки.

```lsf
CLASS Message 'Message';

dateTime 'Time' = DATA DATETIME (Message);
text 'Text' = DATA TEXT (Message);

author = DATA CustomUser (Message);
nameAuthor 'Author' (Message m) = name(author(m));
own (Message m) = author(m) = currentUser();

replyTo = DATA Message (Message);
nameAuthorReplyTo (Message m) = nameAuthor(replyTo(m));
textReplyTo (Message m) = text(replyTo(m)); 
```

### Отображение списка сообщений

Список сообщений в чате на форме будем отображать компонентом, написанным на JavaScript.
Каждое сообщение показывает сразу несколько значений — автора, время, текст, цитируемое сообщение, — а компонент свойства получает только значение своего свойства.
Поэтому список сообщений отображается [пользовательским компонентом группы объектов](How-to_Custom_components_objects.md): все нужные свойства добавляются на форму как обычно, и компонент получает их значения по именам свойств на форме.
Компонент свойства понадобится ниже — для поля ввода нового сообщения.

Создадим форму чата. При помощи ключевого слова **CUSTOM** указывается, что список сообщений должен отображаться при помощи функции _chatMessages_, которая будет написана на JavaScript:

```lsf
FORM chat 'Chat'
    OBJECTS msg = Message CUSTOM 'chatMessages' LAST
    PROPERTIES(msg) READONLY nameAuthor, dateTime, text, own, nameAuthorReplyTo, textReplyTo
;
```

Далее настраиваем дизайн формы, помещая список сообщений в новый контейнер с идентификатором _chat_, а также удаляем ненужные компоненты, созданные автоматически:
```lsf
DESIGN chat {
    OBJECTS {
        NEW chat {
            fill = 1; 
            MOVE GRID(msg);
            REMOVE BOX(msg);
        }
    }
    REMOVE TOOLBARBOX;       
}
```

Добавляем форму в навигатор:
```lsf
NAVIGATOR {
    NEW chat;
}
```

Далее создадим при помощи JavaScript и CSS компонент, который будет отображать сообщения в браузере.
Компонент создадим в файле chat.js, который расположим в папке _resources/web_. Это путь без сборки — обычный файл `.js`, без JSX и упаковки; где размещается пользовательский JS и о варианте со сборкой см. [How-to: Пользовательские клиентские JS-модули](How-to_Custom_client_JS_modules.md). `controller`, который получают эти классические компоненты, описан в [How-to: API контроллера пользовательского представления](How-to_Custom_view_controller.md). 

Внутри файла chat.js создадим функцию _chatMessages_. Она будет возвращать объект, состоящий из двух функций: _render_ и _update_.

Функция _render_ принимает на вход элемент, внутри которого должны создаваться новые элементы, необходимые для отображения данных, а также контроллер. В ней создается и запоминается контейнер, в котором будут отображаться сообщения:
```js
render: function (element, controller) { 
    let messages = document.createElement("div");
    messages.classList.add("chat-messages");

    element.messages = messages;
    element.appendChild(messages);
}
```

Для обновления отображаемых значений платформа будет каждый раз вызывать функцию _update_, в которую будет передан тот же _element_, 
что и в функции _render_, контроллер, а также список сообщений _list_. Каждый элемент списка содержит значения свойств, добавленных на форму, в полях с именами этих свойств: _nameAuthor_, _dateTime_, _text_ и так далее.
Функция удаляет ранее созданные элементы и создает для каждого сообщения из списка свою структуру элементов:
```js
update: function (element, controller, list) {
    while (element.messages.lastElementChild) {
        element.messages.removeChild(element.messages.lastElementChild);
    }

    for (let item of list) {
        let message = document.createElement("div");
        message.classList.add("chat-message");
        if (item.own)
            message.classList.add("chat-message-own");
        if (controller.isCurrent(item))
            message.classList.add("chat-message-current");

        let header = document.createElement("div");
        header.classList.add("chat-header");

        let author = document.createElement("div");
        author.classList.add("chat-author");
        author.innerText = item.nameAuthor || '';
        header.appendChild(author);

        let replyAction = document.createElement("a");
        replyAction.classList.add("chat-reply-action");
        replyAction.appendChild(document.createTextNode("Reply"));
        header.appendChild(replyAction);

        message.appendChild(header);

        let replyContent = document.createElement("div");
        replyContent.classList.add("chat-reply-content");

        let replyAuthor = document.createElement("div");
        replyAuthor.classList.add("chat-reply-author");
        replyAuthor.innerText = item.nameAuthorReplyTo || '';
        replyContent.appendChild(replyAuthor);

        let replyText = document.createElement("div");
        replyText.classList.add("chat-reply-text");
        replyText.innerText = item.textReplyTo || '';
        replyContent.appendChild(replyText);

        message.appendChild(replyContent);

        let text = document.createElement("div");
        text.classList.add("chat-text");
        text.innerText = item.text || '';
        message.appendChild(text);

        let time = document.createElement("div");
        time.classList.add("chat-time");
        time.innerText = item.dateTime ? item.dateTime.toLocaleString() : '';
        message.appendChild(time);

        element.messages.appendChild(message);
    }

    let current = element.messages.querySelector(".chat-message-current");
    if (current)
        current.scrollIntoView({ block: "nearest" });
}
```
Значения свойств приходят преобразованными в значения JS: текстовые — строками, _own_ — логическим значением, _dateTime_ — объектом `Date`, поэтому время форматируется средствами браузера.
Текущее сообщение группы определяется методом _isCurrent_ контроллера и выделяется классом _chat-message-current_; после обновления оно прокручивается в видимую область.
В результате для каждого сообщения будет создана следующая структура элементов:
```html
<div class="chat-message chat-message-own">
   <div class="chat-header">
      <div class="chat-author">John Doe</div>
      <a class="chat-reply-action">Reply</a>
   </div>
   <div class="chat-reply-content">
      <div class="chat-reply-author"></div>
      <div class="chat-reply-text"></div>
   </div>
   <div class="chat-text">Hello world !</div>
   <div class="chat-time">05.10.2021, 15:28:05</div>
</div>
```
Для каждого элемента задается свой класс, который используется для дизайна при помощи CSS :
```css
.chat-messages {
    display: flex;
    flex-direction: column;
}

.chat-message {
    margin: 6px;
    border: 1px solid;
    border-radius: 10px;
    padding: 6px;

    display: flex;
    flex-direction: column;
}

.chat-message-current {
    border-color: blue;
}

.chat-header {
    display: flex;
    align-content: stretch;
    justify-content: space-around;
}

.chat-author {
    font-weight: bold
}

.chat-reply-action {
    cursor: pointer;
    margin-left: 4px;
}

.chat-reply-content {
    border-left: 2px solid;
    padding-left: 4px;
    margin: 4px;
    border-color: blue;
    cursor: pointer;
    flex: 1;
}

.chat-reply-author {
    color: grey
}

.chat-reply-text {
    white-space: pre-wrap;
    max-height: 100px;
    overflow: clip;
}

.chat-text {
    white-space: pre-wrap;
}

.chat-message-own {
    background-color: lightblue;
    margin-left: 100px;
}

.chat-time {
    color: grey
}
```

Чтобы объединить эти две функции в одну, создается новая функция _chatMessages_, которая возвращает их внутри одного объекта:
```js
function chatMessages() {
    return {
        render: function (element, controller) {
            ...
        },
        update: function (element, controller, list) {
            ...
        }
    }
}
```

Для того, чтобы при открытии страницы в браузере, загрузились созданные js и css файлы, нужно добавить их инициализацию в действии _onWebClientInit_ путем добавления имени файла в свойство _onWebClientInit(STRING)_. Числовое значение необходимо для задания порядка загрузки:
```lsf
onWebClientInit() + {
    onWebClientInit('chat.js') <- 1;
    onWebClientInit('chat.css') <- 2;
}
```

Сообщение, отображаемое при помощи созданного компонента, будет выглядеть следующим образом:

![](../images/How-to_Custom_components_message.png)

### Обработка действий пользователя {#handling-user-actions}

В этом примере будем обрабатывать два действия пользователей для любого из сообщений: нажатие на цитируемое сообщение и нажатие на кнопку Reply.
В первом случае будет осуществлен переход к исходному сообщению, а во втором - запоминание этого сообщения в [локальное свойство](../paradigm/Data_properties_DATA.md#---local) 
и установка фокуса в поле ввода нового сообщения.

Объявим для них [действия](../paradigm/Actions.md) и добавим их на форму:
```lsf
replyTo = DATA LOCAL Message ();

goToReply (Message m) { seek(replyTo(m)); } // переходим к цитируемому сообщению
reply (Message m) { replyTo() <- m; } // запоминаем текущее сообщение в локальное свойство

EXTEND FORM chat
    PROPERTIES(msg) goToReply, reply
;
```

Для выполнения этих действий используется параметр _controller_, передаваемый в функцию _update_: его метод _changeProperty_ выполняет действие, добавленное на форму, для переданного сообщения.
Обработчики добавляются в функции _update_ при создании элементов сообщения:
```js
replyAction.onclick = function(event) {
    controller.changeProperty('reply', item);
    $(this).closest("div[lsfusion-container='chat']").find(".chat-message-input-area").focus();
}

replyContent.onmousedown = function(event) {
    controller.changeProperty('goToReply', item);
}
```
По нажатию на кнопку Reply также происходит поиск поля для ввода сообщения при помощи jQuery и установка в него текущего фокуса.
Элемент DOM с классом chat-message-input-area будет создан позднее.

### Отправка нового сообщения

Осталось добавить на форму возможность пользователю создавать новые сообщения. 

Для начала создадим действие `send[]`, которое будет создавать новое сообщение в отдельной [сессии](../paradigm/Change_sessions.md) 
на основе локального свойства `message[]` и определенного ранее свойства `replyTo[]`, а затем очищать их:
```lsf
message = DATA LOCAL TEXT ();

send 'Send' () { 
    NEWSESSION NESTED LOCAL {
        NEW m = Message {
            dateTime(m) <- currentDateTime();
            author(m) <- currentUser();
            replyTo(m) <- replyTo();
            text(m) <- message();
            seek(m);
            APPLY;
        }
    }
    message() <- NULL;
    replyTo() <- NULL;
} 
```

Цитируемое сообщение покажем над полем ввода обычными свойствами формы, а для отмены цитирования объявим действие:
```lsf
replyAuthor 'Reply to' () = nameAuthor(replyTo());
replyText '' () = STRING(text(replyTo()));

removeReply 'Cancel reply' () { replyTo() <- NULL; }
```

Поле ввода нового сообщения — это компонент свойства `message[]`: платформа передает в него текущее значение свойства, а введенный текст компонент возвращает через контроллер.
Создадим функцию _chatMessageInput_, которая будет генерировать этот компонент. Для ввода будем использовать элемент _div_ с атрибутом _contentEditable_:
```js
function chatMessageInput() {
    return {
        render: function (element, controller) {
            let text = document.createElement("div");
            text.classList.add("chat-message-input-area");
            text.contentEditable = "true";

            element.text = text;
            element.appendChild(text);
        },
        update: function (element, controller, value) {
            element.text.innerText = value || '';
        }
    }
}
```
В функцию _update_ параметром _value_ передается значение свойства `message[]` — строка либо `null`, если свойство пусто.

CSS для создаваемого элемента будет выглядеть следующим образом:
```css
.chat-message-input-area {
    flex: 1;
    align-self: stretch;
    max-height: 300px;
    min-height: 90px;
    padding: 4px;
    overflow: auto;
}
```

В результате компонент будет выглядеть следующим образом:

![](../images/How-to_Custom_components_input.png)

Далее добавляем обработчики событий, которые будут отсылать сообщение по нажатию CTRL+ENTER,
а также записывать введенное сообщение в свойство `message[]` при потере компонентом фокуса.
Введенный текст передается методом _change_ контроллера: он попадает в обработку изменения свойства `message[]` так же, как значение, введенное штатным редактором, — для первичного свойства это запись значения в него.
Действие `send[]` выполняется методом _changeProperty_ [контроллера формы](How-to_Custom_view_controller.md), доступного как `controller.form`; запросы выполняются на сервере в порядке вызова, поэтому к моменту выполнения `send[]` введенный текст уже записан в `message[]`.
Обработчики добавляются в функции _update_:
```js
element.text.onkeydown = function(event) {
    if (event.keyCode == 10 || event.keyCode == 13)
        if (event.ctrlKey) {
            controller.change(element.text.innerText);
            controller.form.changeProperty('send');
        } else
            event.stopPropagation(); // останавливаем дальнейшую обработку нажатия клавиши ENTER
}

element.text.onblur = function (event) {
    controller.change(element.text.innerText);
}
```

Добавляем поле для ввода и цитируемое сообщение на форму, а также кнопку _Send_.
При помощи ключевого слова **CUSTOM** указывается, что значение свойства `message[]` должно отображаться при помощи созданной ранее функции _chatMessageInput_.
Если после свойства указано действие с ключевым словом **ON CHANGE**, вместо стандартной обработки изменения выполняется оно, а значение, переданное методом _change_, подставляется вместо ввода пользователя в его [запрос значения](../paradigm/Value_request_REQUEST.md):

```lsf
EXTEND FORM chat
    PROPERTIES replyAuthor() READONLY SHOWIF replyTo(), replyText() READONLY SHOWIF replyTo(), removeReply() SHOWIF replyTo(),
               message() CUSTOM 'chatMessageInput', 
               send()
;
```

Изменяем дизайн формы, чтобы цитируемое сообщение, поле для ввода сообщения и кнопка _Send_ располагались под списком сообщений:
```lsf
DESIGN chat {
    chat {
        NEW reply {
            horizontal = TRUE;
            MOVE PROPERTY(replyAuthor());
            MOVE PROPERTY(replyText());
            MOVE PROPERTY(removeReply());
        }
        NEW chatMessage {
            horizontal = TRUE;
            alignment = STRETCH;
            MOVE PROPERTY(message()) {
                fill = 1;
                autoSize = TRUE;
                width = 0;
                caption = '';
            }
            MOVE PROPERTY(send()) { fontSize = 32; alignment = STRETCH; }
        }
    }  
}
```
Благодаря установке атрибутов _autoSize_ и _width_ компонент ввода будет растягиваться по мере увеличения размера сообщения.

Итоговая форма будет выглядеть следующим образом:

![](../images/How-to_Custom_components_form.png)

### Методы контроллера {#controller-methods}

Методы контроллера, передаваемого в функцию _update_, не считая служебных (необязательные аргументы — в скобках):

| метод | что делает |
| --- | --- |
| `change([value])` | передает изменение значения (см. выше); без аргумента действие изменения вызывается без значения |
| `getValues(value, ok[, fail][, count])` | список подсказок с сервера для вводимого значения `value` — тех же, что при штатном редактировании свойства |
| `isReadOnly()` | доступность свойства для правки: `null` — редактируемое, `false` — только чтение, `true` — отключено |
| `diff(list, fnc[, noDiffObjects][, removeFirst])` | вычисляет изменения массива значений — как метод `diff` в [Пользовательские компоненты (объекты)](How-to_Custom_components_objects.md); элементы сопоставляются по полю `objects` |
| `getColorThemeName()` | имя текущей цветовой темы: `'LIGHT'` или `'DARK'` |
| `form` | [контроллер формы](How-to_Custom_view_controller.md) |

Обработчик `ok` метода `getValues` получает результат в том же формате, что и `getPropertyValues` контроллера формы. Функция _render_ и необязательная функция _clear_, вызываемая при очистке ячейки, получают вторым аргументом сокращенный контроллер — из его методов полезен `clearDiff()`, сбрасывающий запомненный методом `diff` список.

### Пользовательский редактор {#custom-editor}

По умолчанию значение редактируется штатным механизмом свойства — например, текстовым полем ввода. Ключевое слово `CHANGE` [представления значения `CUSTOM`](../language/Properties_and_actions_block.md) позволяет заменить или дополнить этот механизм собственным редактором на JavaScript:

```lsf
PROPERTIES(o) address CUSTOM CHANGE 'googleAutocomplete'
```

Функция редактора, как и функция отображения, регистрируется оберткой и возвращает объект из нескольких функций. Вид редактора определяется тем, какая функция отрисовки есть в этом объекте:

| функция | редактор |
| --- | --- |
| `renderInput(element, controller, value)` | дополняет штатное текстовое поле ввода; `element` — само это поле |
| `renderDialog(element, controller, value)` | редактор-окно |
| `render(element, controller, value)` | редактор, замещающий содержимое ячейки |

`value` — текущее значение свойства, преобразованное в значение JS. Кроме функции отрисовки, объект может содержать:

- `getValue(element)` — возвращает значение для фиксации; строка `'canceled'` отменяет правку. Для редактора поля ввода при отсутствии этой функции берется текст самого поля.
- `clear(element, cancel)` — очистка при завершении правки; `cancel` истинен при отмене.
- `onBrowserEvent(event, element)` — обработка событий браузера во время правки.

Контроллер редактора предоставляет:

| метод | что делает |
| --- | --- |
| `commit([value])` | фиксирует правку: переданное значение либо, без аргумента, текущее (из `getValue` или поля ввода) |
| `cancel()` | отменяет правку |
| `setDeferredCommitOnBlur(true)` | откладывает фиксацию при потере фокуса — для выбора из списка подсказок, отрисованного вне `element` |
| `getColorThemeName()` | имя текущей цветовой темы |
| `form` | [контроллер формы](How-to_Custom_view_controller.md) |

Зафиксированное значение проходит обычный канал изменения свойства — так же, как значение, введенное штатным редактором.

Например, встроенный редактор `googleAutocomplete` дополняет поле ввода адреса подсказками Google Maps:

```js
function googleAutocomplete() {
    return {
        renderInput: (element, controller) => {
            // список подсказок отрисовывается вне element, и выбор из него завершается потерей фокуса,
            // поэтому фиксация по потере фокуса откладывается до установки значения
            controller.setDeferredCommitOnBlur(true);

            new google.maps.places.Autocomplete(element, { types: ['address'] });
        },
        clear: (element, cancel) => {
            // удаляем элементы списка подсказок, добавленные в <body>
            $(".pac-container").remove();
        }
    };
}
```
