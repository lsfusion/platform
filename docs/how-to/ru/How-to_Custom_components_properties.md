---
slug: "/How-to_Custom_components_properties"
title: 'How-to: Пользовательские компоненты (свойства)'
---

Эта статья описывает классические JavaScript-представления для существующего кода. Для нового представления используйте React-компонент в контейнере `custom` или зарегистрированный как `reactView` для `CUSTOM` ([Представления React](../language/React_views.md)).

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
Компонент создадим в файле chat.js, который расположим в папке _resources/web_. Это путь без сборки — обычный файл `.js`, без JSX и упаковки. Где размещается пользовательский JS и о варианте со сборкой см. [Клиентские модули JavaScript](../language/Client_JavaScript_modules.md#without-the-build). `controller`, который получают эти классические компоненты, описан в статье [Контроллеры формы и навигатора](../language/Form_and_navigator_controllers.md#how-a-view-gets-its-controller). 

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

[Функция `update`](../language/Render_functions.md#render-function) получает список сообщений _list_. Каждый элемент списка содержит значения свойств, добавленных на форму, в полях с именами этих свойств: _nameAuthor_, _dateTime_, _text_ и так далее.
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
Текущее сообщение группы определяется методом _isCurrent_ контроллера и выделяется классом _chat-message-current_. После обновления оно прокручивается в видимую область.
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

Загрузим `chat.js` и `chat.css` через [`onWebClientInit`](../language/Client_JavaScript_modules.md#without-the-build):
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
В первом случае будет осуществлен переход к исходному сообщению, а во втором - запоминание этого сообщения в [локальное свойство](../paradigm/Data_properties_DATA.md#local) 
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
В функцию _update_ параметром _value_ передается значение свойства `message[]` — строка либо `null`, если свойство пусто ([Значение свойства](../language/Render_functions.md#property)).

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
Введенный текст передается методом _change_ контроллера ([Контроллер свойства](../language/Render_functions.md#property-controller)).
Действие `send[]` выполняется методом _exec_ [контроллера формы](../language/Form_and_navigator_controllers.md), доступного как `controller.form`: это вызов сервера, который форма разрешает, перечисляя действие в своей секции `CUSTOMS`. По нажатию CTRL+ENTER обработчик вызывает _change_ раньше _exec_, поэтому к моменту выполнения `send[]` введенный текст уже записан в `message[]`.
Обработчики добавляются в функции _update_:
```js
element.text.onkeydown = function(event) {
    if (event.keyCode == 10 || event.keyCode == 13)
        if (event.ctrlKey) {
            controller.change(element.text.innerText);
            controller.form.exec('send');
        } else
            event.stopPropagation(); // останавливаем дальнейшую обработку нажатия клавиши ENTER
}

element.text.onblur = function (event) {
    controller.change(element.text.innerText);
}
```

Добавляем поле для ввода и цитируемое сообщение на форму, а также кнопку _Send_. **CUSTOMS** разрешает компоненту вызывать `send[]` через `controller.form.exec`.
При помощи ключевого слова **CUSTOM** указывается, что значение свойства `message[]` должно отображаться при помощи созданной ранее функции _chatMessageInput_:

```lsf
EXTEND FORM chat
    PROPERTIES replyAuthor() READONLY SHOWIF replyTo(), replyText() READONLY SHOWIF replyTo(), removeReply() SHOWIF replyTo(),
               message() CUSTOM 'chatMessageInput', 
               send()
    CUSTOMS send
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
                valueHeight = -1;
                width = 0;
                caption = '';
            }
            MOVE PROPERTY(send()) { fontSize = 32; alignment = STRETCH; }
        }
    }  
}
```
Благодаря установке атрибутов _valueHeight_ (`-1` — высота по содержимому) и _width_ компонент ввода будет растягиваться по мере увеличения размера сообщения.

Итоговая форма будет выглядеть следующим образом:

![](../images/How-to_Custom_components_form.png)
