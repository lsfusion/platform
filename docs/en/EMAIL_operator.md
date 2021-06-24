---
title: 'EMAIL operator'
---

The `EMAIL` operator creates an [action](Actions.md) that [sends email](Send_mail_EMAIL.md).

### Syntax

    EMAIL [FROM fromExpr] 
    [SUBJECT subjExpr]
    recipientType1 recipientExpr1
    ...
    recipientTypeN recipientExprN
    [BODY bodyExpr]
    ATTACH attachFileExpr1 [NAME attachNameExpr1]
    ...
    ATTACH attachFileExprM [NAME attachNameExprM]

### Description

The `EMAIL` operator creates an action that sends emails. 

It is assumed that the file extension is not specified in the attachment name, i.e. period `.` is also considered part of the file name. This extension is automatically determined similar to the [`WRITE` operator](WRITE_operator.md).

### Parameters

- `fromExpr`

    An [expression](Expression.md) which value determines the sender address. 

- `subjExpr`

    An expression which value determines the email subject.

- `recipientType1 ... recipientTypeN`

    Recipient types. N>=1. Each of them is specified by one of the keywords:

    -   `TO` - message recipient
    -   `СС` - secondary message recipient to whom a copy is sent
    -   `BCC` - message recipient whose address is not shown to other recipients

- `recipientExpr1 ... recipientExprN`

    Expressions which values determine the addresses of the message recipients.

- `bodyExpr`

    An expression which value is the mail body. Can be either a string or file type.

- `attachFileExpr1 .... attachFileExprM`

    Expressions which values are files attached to the message.

- `attachNameExpr1 ... attachNameExprM`

    An expression which values determine the names of the attachments.

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
    BODY bodyFile();
}

justSendEmail 'Send letter' ()  {
    stringToFile('<font color=#FF0000 size=+3>big red text</font>');
    EMAIL
    FROM 'luxsoft@adsl.by'
    SUBJECT 'Letter subject'
    TO 'xxx@tut.by'
    BODY resultFile();
}
```
