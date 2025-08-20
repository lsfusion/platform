---
title: 'MESSAGE operator'
---

The `MESSAGE` operator creates an [action](Actions.md) that shows the user a [message](Show_message_MESSAGE_ASK.md).

### Syntax

```
MESSAGE expression options
```

Operator options `options` can be listed one after another in any order. The following set of options is supported:

```
syncType
messageType
```

### Description

The `MESSAGE` operator creates an action that shows a message to the user either as a dialogue box with a text message or in the [system window `System.log`](Navigator_design.md#systemwindows). The text message can be represented either by a string constant or by another more complex [expression](Expression.md) whose value is a string or a value that can be converted to a string.

### Parameters

- `expression`

    An expression which value is the message text.

- `syncType`

    Synchronisation type. Specifies when the execution of the created action completes. Specified by one of the keywords:

    - `WAIT` - after the end of the message showing (in the case of displaying a dialog box with a message - after the window is closed). This value is used by default.

    - `NOWAIT` - right after the information is ready for sending to the user (the message text is read). If several `MESSAGE` `NOWAIT` actions are called during the execution of some continuous action, they do not create separate messages but are concatenated with the previous messages of the same type. A single concatenated message will be shown to the user at the end of the continuous action as a result.

- `messageType`

    Message type. Specifies how the message will be displayed on the screen. Specified by one of the keywords:

    - `LOG` - message in the `System.log` window.
  
    - `INFO` - information message.
	
	- `SUCCESS` - success message.
	
	- `WARN` - warning message.
	
	- `ERROR` - error message.

	- `DEFAULT` - plain message. This value is used by default.	

### Examples

```lsf
message { MESSAGE 'Hello World!'; } // plain text message

isGood = DATA BOOLEAN (Item);
stringData(Item i) {
    // depending on which item will be passed to the action, a window will be shown either 
    // with the text 'Good' or with the text 'Bad'
    MESSAGE IF isGood(i) THEN 'Good' ELSE 'Bad';   
}

// In this case, five text messages will be shown to the user
// and the third one will be additionally written to the log
testMessage() {
    LOCAL i = INTEGER();
    i() <- 0;
    WHILE i() < 5 DO {
        i() <- i() + 1;
        MESSAGE i();
        IF i() == 3 THEN {
            MESSAGE i() LOG;
        }
    }
}

// In the case of NOWAIT, one text message combining messages
// from five MESSAGE calls will be shown to the user
testMessageNowait() {              
    LOCAL i = INTEGER();
    i() <- 0;
    WHILE i() < 5 DO {
        i() <- i() + 1;
        MESSAGE i() NOWAIT;
    }
}
```
