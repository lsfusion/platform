---
title: 'Change sessions'
---

[Actions](Actions.md) can change the state of the system in which they are executed. It is not always desirable to write these changes directly to the database, both from the perspective of integrity and from the perspective of the ergonomics of the system. The platform therefore allows to accumulate these changes locally in *change sessions.* 

Changes in a session may be changes in [data](Data_properties_DATA.md) properties, including local ones, as well as changes in [classes](User_classes.md) of objects. The former are done by actions created using the [property change operator](Property_change_CHANGE.md), the latter using the [add](New_object_NEW.md)/[change](Class_change_CHANGECLASS_DELETE.md) object class operators.

Each time an action is executed, the *current* session is determined depending on the execution context. For example, if the action is called as the handler of some event on a form (the most common case), then the session of that form will be the current session for it. The current session may also change when the [new session](New_session_NEWSESSION_NESTEDSESSION.md) operator is used, for example.

If an action refers to some property during the execution, then its value is calculated taking into account the changes made in the current session of that action.

Two basic operations are supported for a session - [application](Apply_changes_APPLY.md) and [cancellation](Cancel_changes_CANCEL.md) *-* as well as a set of operators for [working with changes](Change_operators_SET_CHANGED_etc.md) (including getting a [previous value](Previous_value_PREV.md) in the session).

Note that change sessions are not thread safe, therefore when using operators that explicitly or implicitly perform actions in a new thread and do not block their execution flow, it is highly recommended not to access the current session after they are executed (such "multi-threaded" operators include the [new thread](NEWTHREAD_operator.md) and [asynchronous](In_an_interactive_view_SHOW_DIALOG.md#flow) [form opening](In_an_interactive_view_SHOW_DIALOG.md) operators). In this case, it is recommended that you always create a new session.
