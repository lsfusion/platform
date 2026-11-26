---
slug: "/Access_from_an_internal_system"
title: 'Access from an internal system'
---

### Java interaction

In this type of interaction the internal system reaches the Java elements of the lsFusion system directly — as ordinary Java objects. This allows the same operations as over network protocols, but without the significant overhead: no serialization of parameters or deserialization of the result.

This way is especially convenient and efficient when the interaction is very close — a single operation needs constant back-and-forth between the lsFusion system and the other system — and/or requires access to particular platform units.

To find lsFusion elements from Java, first obtain a reference to an object that can look them up. There are two usual ways to do this.

1.  If the initial call comes from lsFusion through [Java interaction](Access_to_an_internal_system_INTERNAL_FORMULA.md#javato), you can use the called action object to find lsFusion elements. Its class extends `InternalAction`. See [internal call (`INTERNAL`)](Internal_call_INTERNAL.md#java) for how the action runs and for a Java class example.
2.  When calling from a Spring bean, use a bean that extends `EventServer`. Spring supplies the platform dependencies through dependency injection.

### SQL interaction

Systems that have access to the SQL server of the lsFusion system (one such system, for example, is the SQL server itself) can directly access [tables](Tables.md) and [fields](Materializations.md) created by the lsFusion system using SQL server means. It should be kept in mind that while reading data is relatively safe (except for possible deletion/modification of tables and their fields), when writing data no [events](Events.md) will be triggered (including all elements that use them - [constraints](Constraints.md), [aggregations](Aggregations.md), etc.), and also no [materializations](Materializations.md) will be recalculated. For this reason writing data directly to lsFusion system tables is highly discouraged. If doing so is necessary, all of the above factors should be taken into account.

### Language

The [Java API for integrations](../language/Java_integration_API.md) describes both `InternalAction` and `EventServer` and the types used by both, including `LP`, `LA`, `DataSession`, and `ExecutionStack`.

The [custom Spring bean (`EventServer`)](../language/Custom_Spring_bean_EventServer.md) article describes the Spring configuration and how to access lsFusion from the bean's lifecycle methods.
