---
slug: "/Send_mail_EMAIL"
title: 'Send mail (EMAIL)'
---

The *send mail* operator creates an [action](Actions.md) that sends an email message. The message has a sender address, a list of recipient addresses each with a visibility flag, a subject, a body, and a list of attachments; in addition, the action itself is configured with a completion mode — synchronous or asynchronous.

### Sender address

The sender address is specified by a separate property. This address is used to select an existing email account in the system (an object of class `Email.Account`) on whose behalf the message is sent. If the sender address is not specified, or no account with that address is found, the default is the account whose `Email.isDefaultInbox[Email.Account]` value is not `NULL`. If no suitable account is chosen, executing the action raises an exception.

### Recipients

Recipients are given as a list; each list entry consists of an address and a visibility flag. Three flags are supported:

|Flag|Meaning|
|---|---|
|primary|ordinary recipient|
|copy|secondary recipient whose address is visible to the others|
|blind copy|recipient whose address is hidden from the others|

The list must contain at least one *primary* recipient; a list consisting only of *copy* or *blind copy* entries is not allowed — the action raises an exception when executed.

### Subject and body

The subject is given by a property returning a string. If the subject is not specified, the value `'{mail.nosubject}'` is used.

The body is given by a property returning either a string or a file value; the message content is taken from that value. The message is always sent as HTML.

### Attachments

Any number of attachments can be added to the message. An attachment is specified in one of two forms:

-   *single attachment* — a pair of a file value and an (optional) name;
-   *attachment list* — a pair of a property listing the files (the property is indexed by sequence number and returns a file) and an (optional) property providing names under the same index.

In both forms the name is given without an extension: the extension is derived from the [file class](Built-in_classes.md), as for [writing a file](Write_file_WRITE.md). If no name is specified, the attachment is named `'attachmentK'`, where `K` is its sequence number.

### Completion mode

The mode determines when the action completes:

|Mode|Completes when|Behavior on failure|
|---|---|---|
|synchronous (default)|the message has actually been sent|an exception is raised|
|asynchronous|immediately after the message is queued; sending runs on a separate thread|retries are performed automatically, with their information written to the system logs|

### Language

To declare an action that sends mail, use the [`EMAIL` operator](../language/EMAIL_operator.md).

### Examples

```lsf
FORM remindUserPass
    OBJECTS u = CustomUser PANEL
    PROPERTIES(u) READONLY login, name[Contact]
;

emailUserPass 'Login reminder' (CustomUser user)  {
    LOCAL bodyFile = FILE ();
    PRINT remindUserPass OBJECTS u = user HTML TO bodyFile;
    // body is a file, asynchronous sending
    EMAIL
        SUBJECT 'Login reminder'
        TO email(user)
        BODY bodyFile()
        NOWAIT
    ;
}

sendInvoice 'Send invoice' (Invoice inv)  {
    EMAIL
        FROM 'sales@company.com'
        SUBJECT 'Invoice ' + number(inv)
        TO email(customer(inv))
        CC 'manager@company.com'
        BODY noteText(inv)
        // single attachment with a specified name
        ATTACH printInvoice(inv) NAME ('invoice_' + number(inv))
        // attachment list: properties are indexed by sequence number (a single INTEGER parameter)
        ATTACH LIST attachFile[INTEGER] NAME attachFileName[INTEGER]
    ;
}
```