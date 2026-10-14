---
slug: "/Utils_Messengers"
title: 'Messengers'
---

`Messenger` is a [system module](System_modules.md) that holds the classes, properties, actions and forms for exchanging messages with external messengers: the accounts the application connects with, the chats an account takes part in, the stored messages, and the abstract actions of sending and chat synchronization that a messenger module implements. The modules `Telegram`, `Viber`, `Whatsapp` and `Slack` connect to the messengers of the same names: an application pulls in the ones it needs (`REQUIRE Telegram`), and `Messenger` comes along with each of them (`Reflection` and `Utils` are pulled in along with it). The `Skype` module in the same folder targets the Skype bot platform, which Microsoft retired in 2025, and is not covered here.

### Messengers

`Messenger` is the class of messengers. Each messenger module extends it with the [static object](Static_objects.md) of its messenger — `Messenger.telegram`, `Messenger.viber`, `Messenger.whatsapp`, `Messenger.slack` — and adds its own implementations to the abstract properties and actions described below. Receiving is implemented by each module on its own.

| Module     | Account settings                                                                                                                                                                                                | Receiving                                                                                                                                                           | Sending                                                                                            |
|------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------|
| `Telegram` | `token[Account]` — the bot token                                                                                                                                                                                | at the delivery address, registered when the account is created unless `skipSetWebHookTelegram[Account]` is set. Private chats and groups                           | text and files                                                                                     |
| `Viber`    | `token[Account]` — the authentication token of the bot                                                                                                                                                          | at the delivery address, registered when the account is created. A private chat is created when a user subscribes or writes, and deleted when the user unsubscribes | text                                                                                               |
| `Whatsapp` | `accessToken[Account]` — the temporary access token, `phoneNumberId[Account]` — the identifier of the sender phone number. The application-wide `verifyToken[]` must match the verification token entered together with the address | at the delivery address `webhookUrl[]`, entered in the Meta developer settings by hand. Private chats                                                          | text and files. `sendTemplateMessage[Chat]` sends the `hello_world` template without storing a message |
| `Slack`    | `token[Account]` — the user access token of the API                                                                                                                                                             | by request: `synchronizeChats[Account]` loads the users and channels, and `receiveMessages[Chat]` the messages of a chat that came after the last stored one      | text                                                                                               |

### Accounts, chats and messages

`Account` stores the connection settings for one messenger.

| Property                  | What it holds                                                                          |
|---------------------------|----------------------------------------------------------------------------------------|
| `name[Account]`           | account name                                                                           |
| `messenger[Account]`      | the messenger as a `Messenger` object. `captionMessenger[Account]` returns its caption |
| `token[Account]`          | the token of the messenger's API                                                       |

`account[LONG]` finds an account by its object identifier: the delivery addresses of Telegram and Viber carry it as the `botid` parameter.

`Chat` is a conversation of an account with one user, group or channel.

| Property                       | What it holds                                                                                                                                                   |
|--------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `account[Chat]`                | the account. Deleting the account deletes its chats                                                                                                             |
| `id[Chat]`                     | the identifier of the chat in the messenger                                                                                                                     |
| `chatType[Chat]`               | the kind of chat as a `ChatType` object: `ChatType.private`, `ChatType.group` or `ChatType.channel`. `captionChatType[Chat]` returns its caption                |
| `name[Chat]`, `title[Chat]`    | the name (the user or channel name) and the title of the chat. The title is shown for the messengers whose `showTitle[Messenger]` is set                        |

`chatIdAccount[Account, STRING]` finds a chat by account and identifier. A chat is created when its first message arrives (in Viber also when a user subscribes). A messenger that lists its chats (`Slack`, with `isSynchronize[Messenger]` set) loads them with `synchronizeChats[Account]`, which runs when an account is created and by the `synchronizeChatsAction[Account]` button of the form.

`Message` is a stored message of a chat.

| Property             | What it holds                                                                                                                                                                                                                                       |
|----------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `chat[Message]`      | the chat                                                                                                                                                                                                                                            |
| `message[Message]`   | the text                                                                                                                                                                                                                                            |
| `file[Message]`      | the file sent from the application with `sendFile[Chat, NAMEDFILE]`. The receiving actions do not store incoming files. `openFile[Message]` opens it                                                                                                                                                                                                     |
| `from[Message]`      | the sender: in Telegram the user name or, without one, the first name; in Viber the name of the subscribing user or of the message sender; in Whatsapp the phone number; in Slack the name of the user found among the loaded users, or `YOU` when none is found. `YOU` also marks a message sent from the application |
| `dateTime[Message]`  | the moment the message was stored. For a message received by Slack, the time of the message in the messenger                                                                                                                                        |

### Sending

`sendMessage[Chat, STRING]` sends a text to a chat and `sendFile[Chat, NAMEDFILE]` a file. Both are abstract actions of the conditional form: a messenger module adds the implementation for the chats of its accounts. A sent message is stored as a `Message` with `YOU` in `from[Message]`. The implementation then applies the changes of the current session, together with any other changes pending in it, whether or not the messenger reported an error. A failed request (no connection, or an error status in the response) ends the action with an error. An error that the messenger reports in a successful response is written to the local property `sendMessageResult[]` by Telegram, Viber and Slack; Whatsapp does not check the response. The `sendMessage[Chat]` action of the form sends the entered text `newMessage[Chat]` and, when that gave no error, the attached file `newAttachment[Chat]`, then shows the error if there is one. Viber and Slack do not send files: for them `sendFile[Chat, NAMEDFILE]` only puts a warning into `sendMessageResult[]`.

### Receiving

Telegram, Viber and Whatsapp deliver incoming messages themselves: the messenger sends an HTTP request to the web server of the application at the *delivery address* registered for the account. The address is built from `webServerUrl[]` of the [`Reflection`](System_Reflection.md) module and the name of the action that receives the request: `<webServerUrl>/exec/webHookTelegram?botid=<account identifier>` for Telegram, the same with `webHookViber` for Viber, and `<webServerUrl>/exec/webHookWhatsapp` for Whatsapp, which identifies the account by the phone number identifier in the request (`phoneNumberId[Account]`). The receiving actions `webHookTelegram[JSONFILE]`, `webHookViber[JSONFILE]` and `webHookWhatsapp[JSONFILE]` are declared `@@noauth`, so the messenger calls them [from an external system](Access_from_an_external_system.md) without authentication. Each reads the message from the request body, finds or creates the chat, stores a `Message` and applies the changes; Whatsapp answers the address verification request (a request without a body) instead, when its token matches `verifyToken[]`. The web server must be reachable by the messenger at that address over HTTPS, which the messengers require. Without such an address a Telegram account can only send, with the registration turned off as described below, and a Viber account cannot be created at all.

Telegram and Viber register the delivery address themselves when an account is created (`setWebHookTelegram[Account]`, `setWebHookViber[Account]`, run by an event on the new account): when the messenger rejects the registration, its message is shown and the creation is cancelled. For a Telegram account that only sends, the registration is turned off by an implementation of the abstract property `skipSetWebHookTelegram[Account]`. For Whatsapp the address `webhookUrl[]` is entered in the messenger's own settings together with the verification token that `verifyToken[]` must match.

Slack receives by its own requests instead and needs no delivery address: `receiveMessages[Chat]` requests from the messenger the messages of the chat that came after the last stored one. It runs from the button of the form and before a text is sent.

### Forms

The `messengers` form, placed in the `notification` folder of the navigator, shows the accounts, the chats of the selected account with the fields of a new message and an attachment, and the messages of the selected chat. The form reloads its data every 20 seconds, which shows the messages delivered to the application meanwhile. Slack messages are loaded only by `receiveMessages[Chat]`. The `messenger` form is the edit form of an account. The messenger modules add their own settings and actions to these forms.

### Examples

```lsf
MODULE Notifications;

REQUIRE Telegram;

CLASS Order 'Order';
number 'Number' = DATA STRING[20] (Order);

// an account that only sends: the flag is set on its form before the account is saved,
// so no delivery address is registered for it
sendOnly 'Send only' = DATA BOOLEAN (Account);
skipSetWebHookTelegram(Account a) += sendOnly(a);
EXTEND FORM messenger PROPERTIES(a) sendOnly;

// a notification to every Telegram chat named operator: such a chat appears when that user writes to the account,
// or is created by hand with the account, the Telegram chat identifier in id[Chat] and the name in name[Chat]
notifyOperator 'Notify the operator' (Order o) {
    FOR isTelegram(account(Chat c)) AND name(c) = 'operator' DO
        sendMessage(c, 'Order ' + number(o) + ' is ready');
}
```
