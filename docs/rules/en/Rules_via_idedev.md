---
slug: "/Rules_via_idedev"
title: 'Rules: IntelliJ tools'
---

## IDE diagnostics

1. The assistant MUST use IDE diagnostics or error checking when available. It MUST run diagnostics before the edit as well and compare the results: a large module can carry problems not caused by the edit.

2. If a diagnostics call is cancelled or aborted just after a file was created or changed outside the IDE, the assistant MUST pause and repeat it before falling back: indexing can interrupt the call. It MUST NOT assume the user cancelled it without confirmation.

3. If diagnostics exceed the client's time limit, the assistant MUST repeat the call after indexing settles. Inspections of a large project can outlast that limit even when the call requests a longer timeout. Without returned diagnostics it MUST treat the file as unchecked. If the repeated call still does not return, it MUST check the file by a dry run or server start before presenting the result.

4. If diagnostics of an `.lsf` file return an empty problem list while the IDE is indexing or just after the file was created or changed outside the IDE, the assistant MUST NOT treat that result as confirmation that the file has no errors: it does not show whether the current content of the file was analyzed. The assistant MUST call `lsfusion_find_elements` with the module of the file in `modules` and `requiredModules` set to `false`, and for a large module also with the changed declarations in `names`. If the search returns declarations of the file with their current code, the assistant MUST repeat diagnostics. If it does not, the assistant MUST check the file by a dry run or server start before presenting the result.

## File edits

1. If an IDE call that creates, changes, moves or deletes files times out or fails, the assistant MUST compare every path the call was to affect with the intended result before repeating the call, making the change another way or presenting the result. The call may have applied every change, only some of them or none, and a file it was creating can be left empty.

## Metacode visibility

1. If the IDE cannot resolve an element declared by a metacode usage, the assistant MUST expand the usage with `lsfusion_set_meta_visibility` (`show`) and repeat diagnostics. It MUST expand the file where the metacode is applied, which may be in a required module rather than the file being edited.

2. After checking, the assistant MUST collapse the usage (`hide`) before committing.

## Element search

1. The assistant SHOULD prefer `lsfusion_find_elements` over plain text search. Having identified the needed element types, modules and classes, it MUST try to find the elements in one structured search call with the correct filters.

2. If the target cannot be found, the assistant MUST perform at least one fallback: a minimal-filter search for a project overview, or a related-element search from elements already found.

3. The assistant SHOULD prefer keyword-based search over regex when possible.
