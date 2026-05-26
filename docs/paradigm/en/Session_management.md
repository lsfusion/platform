---
slug: "/Session_management"
title: 'Session management'
---

*Session management* covers the operators that work with a [change session](Change_sessions.md) — applying or cancelling its accumulated changes, running an action in a different session, or accessing those changes:

-   [Apply changes (`APPLY`)](Apply_changes_APPLY.md)
-   [Cancel changes (`CANCEL`)](Cancel_changes_CANCEL.md)
-   [New session (`NEWSESSION`, `NESTEDSESSION`, `NEWSQL`)](New_session_NEWSESSION_NESTEDSESSION.md)
-   [Previous value (`PREV`)](Previous_value_PREV.md)
-   [Change operators (`SET`, `CHANGED`, ...)](Change_operators_SET_CHANGED_etc.md)


:::info
Note that the latter two operators create properties, not actions.
:::

### Nested local properties {#nested}

When changes are [applied](Apply_changes_APPLY.md), [canceled](Cancel_changes_CANCEL.md), or [a new session is created](New_session_NEWSESSION_NESTEDSESSION.md), all local properties are reset to `NULL` by default — applying or canceling clears the current session, and creating a new session starts with an empty one. (Creating a [nested session](New_session_NEWSESSION_NESTEDSESSION.md#nested) is different: it copies the current session into the nested one, so local property values are visible there from the start.) This is not always convenient. Besides, you may often need to pass data between different sessions or "life cycles" of the same session. To do that, you can mark specific local properties as *nested*. In this case:

1.  When a new session is created, all values of the local property are copied to it and are copied back when it closes.
2.  When changes are applied, all values of the local property are preserved after the transaction is completed (by default, after applying changes the session is cleared along with the values of all local properties).
3.  When changes are canceled, all values of the local property will remain the same as they were before the cancellation.

The nesting mark can be added globally on a local property's declaration (and applies to all of its uses), or specified separately on each session-management operator — including a blanket mark covering all local properties of the current session.
