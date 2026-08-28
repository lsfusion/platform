---
slug: "/Rules_migration"
title: 'Rules: migration (migration.script)'
---

## Migration rules (`migration.script`)

1. Renaming a property or action, or moving it to another
   namespace, changes its canonical name. Whenever the assistant
   renames or re-namespaces an existing element, it MUST record
   the change in `migration.script` in the same edit; otherwise
   the platform treats the old and new names as unrelated
   elements — the old one is dropped and the new one starts empty.

2. For a primary (`DATA`) property this is silently destructive
   and the assistant MUST take special care. The rename / namespace
   change MUST be recorded as a `STORED PROPERTY` change
   (`old canonical name -> new canonical name`), which renames the
   underlying database column and preserves its data. A plain
   `PROPERTY` change carries over only the security-policy and
   reflection settings, NOT the stored data.

3. Without the `STORED PROPERTY` entry, on the next server start
   the old column is renamed to `_DELETED_` plus its old database
   name — or dropped outright if a column by that name is already
   there — and a fresh
   empty column is created for the new name, so all existing
   values of the property are lost. The assistant MUST NOT rename
   or move a `DATA` property to another namespace without adding
   this entry.

4. Renaming a custom class, or moving it to another namespace,
   MUST be recorded as a `CLASS` change to preserve its objects
   and their data. Such a class rename can also change the
   canonical names of its `DATA` properties; these are not tracked
   automatically and MUST be added as their own `STORED PROPERTY`
   changes.
