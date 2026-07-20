---
slug: "/Chat"
title: 'Chat'
---

The *chat* is a built-in platform mechanism for exchanging text messages between system users and for broadcasting system notifications. The chat supports group rooms and one-to-one dialogs, file attachments, replies, editing and deletion within a bounded time window, read-status tracking, and push notifications.

### Chats, dialogs and participants

A *chat* is an entity with a set of participants. A user becomes a participant when added to the chat; for each participant a per-user *read-only* flag is stored independently, forbidding that participant from sending messages.

A *dialog* is the special case of a chat with exactly two participants. The dialog between two users is unique: an attempt to open a dialog with an interlocutor with whom one already exists uses the existing dialog rather than creating a new one.

### Messages

A *message* has an author, formatted text, a send moment, and a last-edit moment. A message may be a *reply* to another message in the same chat — in that case it references the source message and is shown with a quote. A message may carry exactly one attached file with a specified name.

The author can *edit* or *delete* their own message within a bounded time window — one hour from the send moment. An edited message is annotated with an edit marker carrying the last-edit moment. After the editing window closes, the message becomes immutable.

### Statuses and unread messages

For each *(message, recipient)* pair a delivery status of one of three levels is stored:

|Status|Meaning|
|---|---|
|Sent|The message has been recorded on the server|
|Delivered|The message has been received by the recipient's client|
|Read|The recipient has marked the message as read — typically by opening the corresponding chat form|

In the list of their own messages the author sees an aggregated status — the highest level reached by any recipient: *read* once at least one recipient has read the message, otherwise *delivered* once at least one has received it, otherwise *sent*.

For each user and each chat an *unread* message count is maintained — the number of messages with status *delivered* or *sent* not yet marked read. This count appears in the chat list and serves as an indicator of dialogs requiring attention.

### System messages

The platform uses the chat for its own notifications — for example, about the start and restart of the application server. System messages are broadcast to all users into a dedicated system chat; a separate retention period of 30 days applies to them, so the service event log does not accumulate indefinitely.

### Push notifications

When a new message appears, push notifications are sent to the participants' currently connected clients. Delivery runs on a separate thread, so as not to delay the send action. Having received a notification, the client redraws the chat list and updates the unread counter; if the form of the corresponding chat is open, the message appears in the feed without a reload.

### Forms

The platform provides two chat forms:

|Form|Purpose|
|---|---|
|Full form|The chat list, the message feed of the selected chat, an input field with attachment — the main interface|
|Compact form|A notification popup surfacing the most recent unread messages across all the user's chats — for a quick reply or for opening the full interface on the related chat|

Chat is available in both the web and desktop clients and works on top of the same data. In the message feed, text formatting and links are rendered automatically.
