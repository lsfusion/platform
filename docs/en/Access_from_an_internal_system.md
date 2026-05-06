---
title: 'Access from an internal system'
---

### Java interaction

In this type of interaction the internal system reaches the Java elements of the lsFusion system directly — as ordinary Java objects. This allows the same operations as over network protocols, but without the significant overhead: no serialization of parameters or deserialization of the result.

This way is especially convenient and efficient when the interaction is very close — a single operation needs constant back-and-forth between the lsFusion system and the other system — and/or requires access to particular platform units.

It is worth noting that in order to access the Java elements of the lsFusion system directly, you must first obtain a link to an object that will have interfaces for finding these Java elements. This is usually done in one of two ways:

1.  If the initial call comes from the lsFusion system via the [Java interaction](Access_to_an_internal_system_INTERNAL_FORMULA.md#javato) mechanism, the action object "through which" the call is done may be used as the "search object" (the class of this action must be inherited from `lsfusion.server.physics.dev.integration.internal.to.InternalAction`, which in turn has all the required interfaces). What exactly is available in such a call and an example Java class are covered in [internal call (`INTERNAL`)](Internal_call_INTERNAL.md#java).
2.  If the object from whose method the lsFusion system must be accessed is a Spring bean, the required platform dependencies are obtained through dependency injection. The canonical pattern for such a component is to extend the `EventServer` hierarchy; for details see [custom Spring bean (`EventServer`)](Custom_Spring_bean_EventServer.md).

The Java classes and methods shared by both paths (`LP` / `LA`, `DataSession`, `ExecutionStack`, `InternalAction` / `EventServer`, and so on) are catalogued in [Java API for integrations](Java_integration_API.md).

### SQL interaction

Systems that have access to the SQL server of the lsFusion system (one such system, for example, is the SQL server itself) can directly access [tables](Tables.md) and [fields](Materializations.md) created by the lsFusion system using SQL server means. It should be kept in mind that while reading data is relatively safe (except for possible deletion/modification of tables and their fields), when writing data no [events](Events.md) will be triggered (including all elements that use them - [constraints](Constraints.md), [aggregations](Aggregations.md), etc.), and also no [materializations](Materializations.md) will be recalculated. For this reason writing data directly to lsFusion system tables is highly discouraged. If doing so is necessary, all of the above factors should be taken into account.
