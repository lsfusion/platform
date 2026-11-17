---
slug: "/Brief_operation"
title: 'Brief: operation'
---

## System parameters

[System parameters](../paradigm/System_parameters.md) configure how an installation starts and how it behaves while running. [Launch parameters](../paradigm/Launch_parameters.md) select the database, loaded modules and server connections at startup. [Working parameters](../paradigm/Working_parameters.md) tune the running mechanisms and can be set globally or for a role in the system settings form. Most take effect when changed, while some are read only at startup.

Use these settings for an installation's behavior, rather than changing application declarations. A temporary override for an action's thread is available through `Service.pushSetting[STRING, STRING]` and `Service.popSetting[STRING]`. The [management](../paradigm/Management.md) facilities below cover user preferences, access and administration of the running system.

## Database maintenance

[Database maintenance](../paradigm/System_Service.md) is available through the `Service` module and its `maintenance` form. It includes checks of stored object classes, materialized values, tables and indices, recalculation of classes and materializations, reapplication of `RESOLVE` rules, and maintenance of the platform's optimizer statistics. It also provides database `ANALYZE`, `VACUUM` and physical removal of rows marked as deleted. Heavier operations have multi-threaded variants.

Use these facilities to check and restore the consistency of stored data and maintain query performance. After a first start or a large schema synchronization, `ANALYZE` collects PostgreSQL's planner statistics. The platform's optimizer statistics are maintained separately.

## User interface

[User interface settings](../paradigm/User_interface.md) let a user choose appearance, regional settings and the tables they work with. They cover theme, scale, navigation-bar placement, language and time zone, and a table's visible columns, order, widths, captions, formats and sortings. Personal settings are saved for the user and override general table settings; an administrator can provide general settings for users without personal ones.

Use these facilities when the request concerns a user's existing interface. A saved choice of visible columns or their order is a table setting. Adding a property to the form's structure or changing its design is [form customization](#customization) or application source work, covered by the [view brief](Brief_view.md).

## Customization

The [`SystemEvents` module](../paradigm/System_SystemEvents.md) supports saved customization code for each form, held in `dataExtendCode[Form]` for all users and `dataExtendCode[Form, User]` for one user. This extends a form without editing the project's source files. `customize[STRING, STRING]` opens the customization dialog; the forms metadata interface also exposes the saved code and lets an administrator remove it.

The common and personal code fragments are joined and compiled when the form opens. If they do not compile, the error is reported and the form opens without customization. This mechanism is reached for when a form needs an added property or a design change that table preferences cannot express. Its effect is tied to creation of the form, rather than to editing values in an already open one.

The application as a whole is customized the same way: `customize[]` edits the code held in `dataExtendCode[]` (all users) and `dataExtendCode[User]` (one user), which adds and moves navigator elements and windows and declares what they need. It is compiled when a connection starts: `customize[]` saves the code and starts its client again, and the other clients see the change when they start again.

## Interpreter

The [interpreter](../paradigm/Interpreter.md) executes lsFusion code on a running application without rebuilding or restarting it. Its modes accept a script declaring `run[]`, an action body, a form body or Java code. It is useful for inspecting data, invoking existing actions and temporarily defining computations; the script log keeps the entered code and its execution history.

Execution uses a new change session. Reads can use the loaded project, and data changes reach the database through `APPLY`. The same [eval restrictions](../paradigm/Eval_EVAL.md) apply: temporary code cannot change the project's persistent structure. The `EVAL` operator provides runtime compilation and execution from an application action. Neither mechanism replaces a source edit and deployment for a lasting change to application logic.

## Security policy

The [security policy](../paradigm/Security_policy.md) configures users' access through roles. Users have a primary role and can have additional roles. The administration forms manage users and role assignments, visibility of navigator forms, and the view, change, follow and group-change permissions of form properties and actions. These are the facilities for requests such as granting a role a form or making a field unavailable for editing.

Users and role assignments are also ordinary objects and properties of the `Authentication` and `Security` system modules, so existing actions or administrative scripts can configure them. Changes saved on the security-policy form take effect at the next user login. The policy article covers how permissions from several roles combine and which settings require a server restart.

## Scheduler

The server [scheduler](../paradigm/Scheduler.md) runs actions without a user's client. Its task form configures active tasks, repetition, time and day restrictions, and an ordered list of actions or scripts. It also supports manual execution, restart and execution logs. Use it for background processing and recurring maintenance, rather than a timer that exists only while a user's form or connection is open.

Tasks and their action lists can also be created through the `Scheduler` module's `UserScheduledTask` and `UserScheduledTaskDetail` objects. Form and navigator `SCHEDULE` events and `NEWTHREAD ... SCHEDULE` serve other execution contexts; their choice is covered in [periodic execution](Brief_logic.md#periodic-execution).

## Backups and recovery

[Backup management](../paradigm/Backup_restore.md) provides full and partial database backups, retention settings, download and selective recovery. A partial backup omits selected tables' data. Selective recovery reads chosen tables and columns from a backup and writes their values through a regular change session, with the application's events and constraints. It is useful when particular data must be recovered without replacing the whole database.

Full recovery replaces the working database and is an administrator's operation outside the running interface, with the server stopped. The backup and recovery article covers that procedure and the required PostgreSQL utilities. A backup can be created manually or by the scheduler; its existence and successful completion are visible in the backup list.

## Journals and logs

[Journals and logs](../paradigm/Journals_and_logs.md) record errors, connections, startups and data changes, alongside the technical logs of the application server, web server and desktop client. Use the journals to find who connected, when the server restarted or which request failed. Their retention is configurable. Per-property change history is a separate logging choice; the change journal's summary is not a complete history of every dependent value.

Technical logs provide the next level of diagnosis: startup and access errors, SQL execution, query plans and application-call timings. Diagnostic modes and thresholds determine what is recorded. They are the place to investigate a failed or slow request after it has finished, alongside the profiler for measurements collected over an interval.

## Process monitor and profiler

The [process monitor](../paradigm/Process_monitor.md) shows a snapshot of current server threads, SQL queries and blocking relationships. Use it to identify an operation running now, inspect a lock chain or cancel a stuck process. Refreshing collects another snapshot. Forced termination is a separate last-resort operation and may leave the servers needing a restart.

The [profiler](../paradigm/Profiler.md) collects property and action execution statistics between start and stop. Its hot spots and call trees show where time was spent, with SQL and user-interaction time distinguished and filters by user and form. Use it for a slow workflow that can be measured over an interval. Query-plan logging is configured separately, on the profiler form's SQL tab.

## Chat

The built-in [chat](../paradigm/Chat.md) lets application users communicate in group rooms and one-to-one dialogs. Messages support a file attachment, replies, delivery and read status, and time-limited editing or deletion by the author. The full form provides the chat list and message feed; the compact form surfaces unread messages for quick replies.

Chat also carries system notifications, including application-server starts and restarts. Push notifications update connected participants' clients, so it is a way to reach users within the running system. This is communication between application users, distinct from an action's `MESSAGE` output or an administrator's technical logs.
