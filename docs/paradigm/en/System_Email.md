---
slug: "/System_Email"
title: 'Email'
---

`Email` is a [system module](System_modules.md) that holds the classes, properties, and actions for electronic mail: the mail accounts and their connection settings, the folder tree, stored messages with their headers, body, and attachments, the actions that receive mail from a server and import `.eml` files, and the compose-and-send surface built on the `EMAIL` operator. It is pulled in via `REQUIRE Email` (`System`, `Reflection`, and `SystemEvents` are pulled in along with it).

### Account

`Account` is the class of a mail account — one set of credentials and connection settings for sending and receiving mail.

| Property                              | What it holds                                                                 |
|---------------------------------------|--------------------------------------------------------------------------------|
| `name[Account]`                       | login name                                                                     |
| `password[Account]`                   | password (declared `ECHO`, so it is masked in the interface)                   |
| `disable[Account]`                    | flag that turns the account off                                                |
| `insecureSSL[Account]`                | flag that allows an untrusted SSL certificate                                  |

#### Sending

| Property                                      | What it holds                                                              |
|-----------------------------------------------|----------------------------------------------------------------------------|
| `smtpHost[Account]` / `smtpPort[Account]`     | SMTP server host and port                                                  |
| `encryptedConnectionType[Account]`            | encryption mode as an `EncryptedConnectionTypeStatus` object               |
| `nameEncryptedConnectionType[Account]`        | the encryption mode by name                                                |
| `fromAddress[Account]`                         | sender address put into outgoing mail                                      |
| `isDefaultInbox[Account]`                     | flag marking the account as the default one for sending                    |

`EncryptedConnectionTypeStatus` is a static [class](User_classes.md#static) with two objects — `SSL` and `TLS` — for the SMTP encryption mode. The `encryptedConnectionTypeStatuses` form lists them.

`inboxAccount[STRING]` picks the account used to send a message from a given sender address: it returns the account whose `fromAddress[Account]` equals that address (`accountFromAddress`), and falls back to the account flagged `isDefaultInbox[Account]` (`defaultInboxAccount`) when no account matches the address.

#### Receiving

| Property                                      | What it holds                                                              |
|-----------------------------------------------|----------------------------------------------------------------------------|
| `receiveHost[Account]` / `receivePort[Account]` | incoming-mail server host and port                                       |
| `receiveAccountType[Account]`                 | receive protocol as a `ReceiveAccountType` object                          |
| `nameReceiveAccountType[Account]`             | the receive protocol by name                                               |
| `startTLS[Account]`                           | flag enabling STARTTLS on the receive connection                           |
| `deleteMessages[Account]`                     | flag to delete messages on the server after they are received             |
| `lastDays[Account]`                           | receive only messages from the last N days                                 |
| `maxMessages[Account]`                        | cap on the number of messages received in one pass                        |
| `unpack[Account]`                             | flag to unpack nested messages                                            |
| `ignoreExceptions[Account]`                   | flag to keep going past a message that fails to process                   |
| `readAllFolders[Account]`                     | flag to read every server folder, not just the inbox                      |

`ReceiveAccountType` is a static [class](User_classes.md#static) with four objects for the receive protocol: `POP3`, `POP3S`, `IMAP`, `IMAPS`. The `receiveAccountTypes` form lists them.

### Folders

`Folder` is the class of a mail folder. `account[Folder]` is the account it belongs to (with cascade `DELETE`), `id[Folder]` is the server-side folder identifier, and `parent[Folder]` is the parent folder, so folders form a tree under an account. `folder` looks a folder up by its `account[Folder]` and `id[Folder]`.

`newFolder[Folder]` creates a child folder under the given one — a new `Folder` with the same account and the given parent — and activates it for editing on the account form.

A sent folder is marked per account: `sentFolder[Account]` is the folder that outgoing mail is saved to, and `isSent[Folder]` is the flag that the given folder is that account's sent folder.

### Message

`Email` is the class of a stored mail message.

| Property                              | What it holds                                                                 |
|---------------------------------------|--------------------------------------------------------------------------------|
| `account[Email]`                      | the account the message belongs to (with cascade `DELETE`)                     |
| `nameAccount[Email]`                  | the account name                                                               |
| `folder[Email]`                       | the folder holding the message                                                 |
| `idFolder[Email]`                     | the folder's `id`                                                              |
| `id[Email]`                           | server-side message identifier                                                 |
| `uid[Email]`                          | server-side message UID                                                        |
| `subject[Email]`                      | subject                                                                        |
| `fromAddress[Email]`                  | sender address                                                                 |
| `toAddress[Email]` / `ccAddress[Email]` / `bccAddress[Email]` | recipient, carbon-copy, and blind-copy addresses               |
| `dateTimeSent[Email]` / `dateSent[Email]` | send date and time, and the date part of it                                |
| `dateTimeReceived[Email]`             | receive date and time                                                          |
| `message[Email]`                      | body as `HTMLTEXT`                                                              |
| `emlFile[Email]`                      | the raw message as an `.eml` `FILE`                                            |

`openEMLFile[Email]` opens the stored `.eml` file on the client.

### Attachments

`AttachmentEmail` is the class of a message attachment. `email[AttachmentEmail]` is the message it belongs to (with cascade `DELETE`), `name[AttachmentEmail]` is the attachment name, `file[AttachmentEmail]` is its contents, and `filename[AttachmentEmail]` joins the name with the file extension. `attachment0[Email, INTEGER]` returns the attachment at a given zero-based position within a message, by the per-message order in `index`.

`openFile[AttachmentEmail]` opens the attachment on the client; `saveFile[AttachmentEmail]` saves it through a client save dialog under the attachment name.

### Receiving mail

| Action                                | What it does                                                                  |
|---------------------------------------|--------------------------------------------------------------------------------|
| `receiveEmailAction[]`                | receive mail for every account at once                                         |
| `receiveEmailAction[Account]`         | receive mail for the given account                                            |
| `receiveEML[Account]`                 | fetch raw `.eml` files from the account's server into `emlFile[LONG]`, keyed by message UID |
| `importEML[Account, LONG, FILE]`      | import one fetched `.eml` file into an `Email` and its attachments            |
| `receiveMail[Account]`                | runs `receiveEML[Account]`, then `importEML[Account, LONG, FILE]` for every fetched file |

`receiveEmailAction[]`, `receiveEmailAction[Account]`, `receiveEML[Account]`, and `importEML[Account, LONG, FILE]` are native actions bound through the `INTERNAL` operator to the platform's mail-receiving Java implementation.

### Sending and composing

`send[Email]` sends a stored message through the `EMAIL` operator: it takes the message's `fromAddress[Email]`, `subject[Email]`, `toAddress[Email]`, `ccAddress[Email]`, `bccAddress[Email]`, and `message[Email]` body, and attaches every file from `attachment0[Email, INTEGER]`.

`writeMessage` opens the compose dialog for an `Email` and sends it. It comes in two forms:

- `writeMessage[STRING, STRING, STRING, STRING, STRING, RICHTEXT, STRING, STRING]` — the full form; creates a new `Email` in a fresh session, fills its from / to / cc / bcc / subject / body and the quoted reply text, opens the `writeMessage` dialog, and on confirmation builds the final HTML body (the message followed by the quoted reply) and calls `send[Email]`.
- `writeMessage[Account]` — composes a new message from the given account, taking the sender address from `fromAddress[Account]` (or the account `name[Account]`).

`reply[Email]` opens the compose dialog as a reply to a message: it swaps sender and recipient, prefixes the subject with `Re:`, and quotes the original body and its send date and sender as the reply header.

### Forms and navigator

| Form / element     | What it shows                                                                       |
|--------------------|-------------------------------------------------------------------------------------|
| `account`          | a single account with its sending and receiving settings and its folder tree        |
| `accounts`         | the list of accounts                                                                 |
| `mail`             | the mail client: accounts, the folder tree, the message list, the selected message with its body and attachments, and the receive / compose / reply actions |
| `writeMessage`     | the compose dialog — headers, body, the quoted reply, and the attachment list       |

The navigator gets the `mail` form under the `notification` folder.

### Language

- [`EMAIL` operator](../language/EMAIL_operator.md) — sends a message; `send[Email]` is written with it.
- [`INTERNAL` operator](../language/INTERNAL_operator.md) — binds the mail-receiving actions to their Java implementation.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`SystemEvents`](System_SystemEvents.md) — the server-lifecycle module pulled in with `Email`.
- [`Reflection`](System_Reflection.md) — the metadata module pulled in with `Email`.
- [`Access to an external system (EXTERNAL)`](Access_to_an_external_system_EXTERNAL.md) — calling out to external services; mail is one such external system.
- [`Security`](System_Security.md) — uses email to send a password-reset message.
