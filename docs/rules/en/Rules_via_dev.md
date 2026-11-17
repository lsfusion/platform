---
slug: "/Rules_via_dev"
title: 'Rules: development'
---

## Project context

1. Before searching the project, the assistant MUST determine the needed element types, modules and classes. It MUST choose output size and timeout for the task's complexity.

## Code changes

1. For a code-related task, the assistant MUST retrieve relevant how-tos or examples first. It MUST use structured project search when those tools are available.

2. The assistant MUST split implementation into small tasks producing little code.

## Code verification

1. Before presenting code, the assistant MUST check it by the strongest means available: IDE diagnostics, a dry run or server start, and execution in a suitable test application when behavior needs checking. It MUST use pure syntax validation only as a fallback when diagnostics and execution are unavailable.

2. Before a server check, the assistant MUST load the changed source, rebuilding and restarting when required: running previously loaded code does not verify a source edit.

3. The assistant MUST NOT run changing code on a production server merely to validate it.

4. If no available means can check the code, the assistant MUST present it as unchecked and say what would check it.

## Skills

1. Where the `lsfusion-ai-skills` skills are available, the assistant MUST follow the skill for the task — `lsfusion-dev` for local setup, server start and checks, `lsfusion-eval` for calls on a running server, `lsfusion-deploy` for deployment — rather than reconstruct its procedure.
