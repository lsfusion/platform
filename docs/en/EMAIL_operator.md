---
title: 'EMAIL operator'
---

The `EMAIL` operator creates an [action](Actions.md) that [sends email](Send_mail_EMAIL.md).

### Syntax

```
EMAIL [FROM fromExpr] [SUBJECT subjExpr]
recipientType1 recipientExpr1
...
recipientTypeN recipientExprN
[BODY bodyExpr]
ATTACH attachDescription1
...
ATTACH attachDescriptionM
[syncType]
```

Each `attachDescription` describes either a single attachment or a list of attachments and has one of two corresponding syntaxes:

```
attachFileExpr [NAME attachNameExpr]
LIST attachFilePropertyID [NAME attachNamePropertyID]
```

### Description

The `EMAIL` operator creates an action that sends an email.

The `FROM` option specifies the sender's email address. This address is used to identify an existing email account in the system (an object of `Email.Account` class) that will be used to send the email. If this option is not specified, or an account with the specified address is not found, then by default, the account for which the value of the `Email.isDefaultInbox[Email.Account]` property is not `NULL` is selected. An exception will be thrown if a matching account is not found.

If the `SUBJECT` option is not specified, the subject value will be equal to `'{mail.nosubject}'`.

In the `NAME` option within `attachDescription`, file names are specified without extensions (the dot `.` will be considered part of the file name). The extension will be automatically determined similar to the [`WRITE` operator](WRITE_operator.md). If the `NAME` option is not specified, the default attachment name will be `'attachmentK'`, where `K` is the index number of the attachment.

### Parameters

- `fromExpr`

    An [expression](Expression.md) which value determines the sender's email address. 

- `subjExpr`

    An expression which value determines the email subject.

- `recipientType1 ... recipientTypeN`

    Recipient types. At least one must be specified. Each of them is specified by one of the keywords:

    - `TO` - message recipient
    - `СС` - secondary message recipient to whom a copy is sent
    - `BCC` - message recipient whose address is not shown to other recipients

- `recipientExpr1 ... recipientExprN`

    Expressions which values determine the addresses of the message recipients.

- `bodyExpr`

    An expression which value determines the message body. Can be of either a string or file class.

- `attachFileExpr`

    An expression which value determines the file to be attached to the email.  The value must belong to a file class.

- `attachNameExpr`

    An expression which value determines the name of the attachment.

- `attachFilePropertyID`

    [Property ID](IDs.md#propertyid) which determines a list of files to be attached to the message. The property must have exactly one parameter of the `INTEGER` class and return a value of a file class

- `attachNamePropertyID`

    Property ID which determines a list of attachment names. The property must have exactly one parameter of the `INTEGER` class.

- `syncType`

    Synchronisation type. Specifies when the execution of the created action completes, allowing you to choose between synchronous and asynchronous approaches. Specified by one of the keywords:

    - `WAIT` - the action completes after the email is sent in the current thread. In case of unsuccessful sending, an exception is thrown. This is the default behavior. 
    - `NOWAIT` - The email is sent in a new thread, allowing the action to complete immediately without waiting for the sending result. In case of failure, the system will automatically make several resend attempts, information about which can be found in the system logs.

### Example

```lsf
FORM remindUserPass
    OBJECTS u=CustomUser PANEL
    PROPERTIES(u) READONLY login, name[Contact]
;

emailUserPassUser 'Login reminder' (CustomUser user)  {
    LOCAL bodyFile = FILE ();
    PRINT remindUserPass OBJECTS u = user HTML TO bodyFile;
    EMAIL
        SUBJECT 'Login reminder'
        TO email(user)
        BODY bodyFile()
        NOWAIT
    ;
}

justSendEmail 'Send letter' ()  {
    stringToFile('<font color=#FF0000 size=+3>big red text</font>');
    EMAIL
        FROM 'luxsoft@adsl.by'
        SUBJECT 'Letter subject'
        TO 'xxx@tut.by'
        BODY resultFile()
    ;
}
```
