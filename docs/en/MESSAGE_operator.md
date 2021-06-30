---
title: 'MESSAGE operator'
---

The `MESSAGE` operator creates an [action](Actions.md) that shows the user a [message](Show_message_MESSAGE_ASK.md).

### Syntax

    MESSAGE expression [syncType]

### Description

The `MESSAGE` operator creates an action which shows a window with a text message to the user. The text message can be a string constant or a more complex [expression](Expression.md) which value is or can be converted to a string.

### Parameters

- `expression`

    An expression which value is the message text.

- `syncType`

    Specifies when the created action should be completed:

    - `WAIT` - after the client completes the action (closes the message). This value is used by default.
    - `NOWAIT` - right after the information is ready for sending to the client (the message text is read). If several `MESSAGE` `NOWAIT` actions are called during the execution of some continuous action, they do not create separate messages but are concatenated with the previous messages of the same type. A single concatenated message will be shown to the user at the end of the continuous action as a result.

### Examples

```lsf
message  { MESSAGE 'Hello World!'; }                                // plain text message

isGood = DATA BOOLEAN (Item);
stringData(Item i)   {
    MESSAGE IF isGood(i) THEN 'Good' ELSE 'Bad';   // depending on which item will be passed to the action, a window will be shown either with the text 'Good' or with the text 'Bad'
}

testMessage()  {                    // In this case, five text messages will be shown to the user
    LOCAL i = INTEGER();
    i() <- 0;
    WHILE i() < 5 DO {
        i() <- i() + 1;
        MESSAGE i();
    }
}

testMessageNowait()  {              // In the case of NOWAIT, one text message combining messages from five MESSAGE calls will be shown to the user
    LOCAL i = INTEGER();
    i() <- 0;
    WHILE i() < 5 DO {
        i() <- i() + 1;
        MESSAGE i() NOWAIT;
    }
}
```
