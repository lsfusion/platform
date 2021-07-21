---
title: 'Show message (MESSAGE, ASK)'
---

The *show message* operator creates an [action](Actions.md) which shows the user a window with a text message. The message is defined as some [property](Properties.md) whose value will be used as a message.

### Flow control

By default, the created action stops the thread until the user closes the message. However, this behavior can be changed using the corresponding option – in this case, the created action is completed immediately and the message is shown to the user as soon as possible (that is, the next user interaction). The first mode shall be called *synchronous* and the second *asynchronous*.

### Dialog form {#dialog}

It is also often necessary not only to inform the user about something, but also, for example, to request confirmation to continue an action. For such cases, the operator allows, instead of simply displaying the message (with a single `OK` button), to ask a question with the option of canceling (`OK`/`Cancel`) and thereby essentially to implement [a value input](Value_input.md). This input is considered to have been [canceled](Value_input.md#result) if the `Cancel` button is pressed (there is no input value in that case).

In addition, a third option can be added to the question: `Yes` / `No` / `Cancel`. In this case, it is considered that [the input result](Value_input.md#result) will be a value of logical class (`Yes` - `TRUE`, `No` - `NULL`). As in the first case, the input is considered to be canceled if the `Cancel` button is selected.

The form of the operator in which the user is asked a question shall be called the *dialog* form. 

As with other value input operators, in the dialog form of this operator you can define [main and alternative](Value_input.md#result) actions. The first is called if the input was successfully completed, the second if not (i.e. if the input was canceled).

The operator dialog form is available in synchronous mode only.

### Language

To declare an action showing a message, use the [`MESSAGE` operator](MESSAGE_operator.md). To display the message in dialog form, use the [`ASK` operator](ASK_operator.md).

### Examples

```lsf
message  { MESSAGE 'Hello World!'; } // plain text message

isGood = DATA BOOLEAN (Item);
stringData(Item i)   {
    // depending on which item will be passed to the action, a window will be shown 
    // either with the text 'Good' or with the text 'Bad'
    MESSAGE IF isGood(i) THEN 'Good' ELSE 'Bad';   
}

// In this case, five text messages will be shown to the user
testMessage()  { 
    LOCAL i = INTEGER();
    i() <- 0;
    WHILE i() < 5 DO {
        i() <- i() + 1;
        MESSAGE i();
    }
}

// In the case of NOWAIT, one text message combining messages from five MESSAGE calls will be shown to the user
testMessageNowait()  {              
    LOCAL i = INTEGER();
    i() <- 0;
    WHILE i() < 5 DO {
        i() <- i() + 1;
        MESSAGE i() NOWAIT;
    }
}
```

  
```lsf
testAsk ()  {
    ASK 'Are you sure you want to continue?' DO {
        MESSAGE 'You continued';
    }

    ASK 'Use old values?' useOld = YESNO DO {
        IF useOld THEN
            MESSAGE 'Using old values';
        ELSE
            MESSAGE 'Using new values';
    }
}
```
