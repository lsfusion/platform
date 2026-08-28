---
slug: "/Rules_modularity"
title: 'Rules: modules'
---

## Module design rules

1. The assistant MUST split lsFusion code into modules
   by domain logic or feature area,
   not by arbitrary technical grouping.

2. The assistant SHOULD prefer relatively short modules.

   A single broad module SHOULD NOT keep growing
   when the logic naturally separates
   into smaller cohesive modules.

3. The assistant MUST apply low coupling and high cohesion:
   closely related classes, properties, actions, and forms
   SHOULD stay together,
   and cross-module dependencies SHOULD remain narrow and explicit.

4. Module `NAMESPACE` SHOULD be chosen by shared business domain,
   not by the full module name.

5. When a module belongs to an existing domain family,
   the assistant SHOULD reuse that family namespace
   for all its elements.

   A new namespace SHOULD be created only
   for a genuinely new domain,
   not for each technical submodule.

6. If the module name already equals the intended domain namespace,
   omitting `NAMESPACE` is acceptable
   because lsFusion will use the module name as the default.

   Otherwise, the assistant SHOULD specify `NAMESPACE` explicitly.

7. The assistant SHOULD use `REQUIRE`, `EXTEND`,
   abstract properties / actions,
   and form extensions to connect modules
   instead of duplicating logic
   or creating a god module.

8. Before adding code to an existing module,
   the assistant MUST check whether the logic belongs
   to that module's domain.

   If not, the assistant SHOULD create
   or extend a more appropriate module.

9. When introducing a new module,
   the assistant MUST choose dependencies deliberately
   and avoid circular or unnecessary dependencies.

10. To use a property, action, class, or form
    from another module, that module MUST be reachable
    from the current module's `REQUIRE` chain — either
    directly, or transitively through other required modules.

    If the owning module is not in the transitive `REQUIRE`
    closure, the platform raises a "Property not found"
    (or analogous "not found") error at startup.

    The assistant MUST add the owning module
    (or any module that already requires it)
    to the current module's `REQUIRE` list before using
    its elements.

11. The server ships bundled system modules whose names
    MUST NOT be reused for application modules — the server
    fails at startup with `module '<name>' has already been
    added`. The bundled names are:
    System, Utils, UserEvents, Scheduler, Email, Time,
    Reflection, Security, Service, Icon, Authentication,
    SystemEvents, Word, WebSocket, Integration, Profiler,
    SQLUtils, ProcessMonitor, DefaultData, Image, Printer,
    Numerator, Chat, Eval, I18n, Com, Sound, Backup, OpenCV,
    Geo, Historizable, Schedule, Document, QZTray, Excel,
    Hierarchy, RabbitMQ, MasterData, Messenger, Whatsapp,
    Skype, Telegram, Viber, Slack.

    For generic domain names from this list (`MasterData`,
    `Document`, `Schedule`, `Numerator`), the assistant
    SHOULD add a project prefix to the module name.
