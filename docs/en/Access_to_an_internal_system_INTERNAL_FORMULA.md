---
title: 'Access to an internal system (INTERNAL, FORMULA)'
---

By *internal system* we mean the deployment components of the lsFusion system itself — the application-server JVM, the user's web client, and the platform's own database. To reach these components from lsFusion code the platform provides two separate operators: [`INTERNAL`](Internal_call_INTERNAL.md) — execution of Java / JavaScript / SQL code as an [action](Actions.md), and [`FORMULA`](Custom_formula_FORMULA.md) — wrapping an SQL expression as a [property](Properties.md).

### Java interaction {#javato}

To implement this type of interaction, platform uses the [internal call (`INTERNAL`)](Internal_call_INTERNAL.md) operator, which allows calling Java code inside the JVM lsFusion server. What is available on the far side of such a call — from the Java code itself — is covered in [access from an internal system](Access_from_an_internal_system.md).

### SQL interaction

To implement this type of interaction, the platform uses the [custom formula (`FORMULA`)](Custom_formula_FORMULA.md) operator, which allows accessing the objects/syntax constructs of the SQL server used by the developed lsFusion system. For arbitrary SQL commands run as an action inside the current change [session](Change_sessions.md), use the [`INTERNAL DB`](Internal_call_INTERNAL.md#db) form.
