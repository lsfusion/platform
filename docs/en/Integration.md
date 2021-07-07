---
title: 'Integration'
sidebar_label: Overview
---

Integration includes everything related to interaction of the lsFusion system with other systems. In terms of the direction of this interaction, integration can be divided into: 

1.  Access to the lsFusion system from another system.
2.  Access from the lsFusion system to another system.

In terms of the physical model, integration can be divided into:

1.  Interaction with systems running in "the same environment" as the lsFusion system (that is, in the Java virtual machine (JVM) of the lsFusion server and/or using the same SQL server as the lsFusion system).
2.  Interaction with remote systems via network protocols.

Accordingly, we will call the first systems *internal*, and the second *external*. In turn, interaction with internal systems using Java tools we will call *Java interaction*, and using SQL tools – *SQL interaction*.

Thus, the platform has four different types of integration:

-   [Access from an external system](Access_from_an_external_system.md)
-   [Access from an internal system](Access_from_an_internal_system.md)
-   [Access to an external system (`EXTERNAL`)](Access_to_an_external_system_EXTERNAL.md) 
-   [Access to an internal system (`INTERNAL`, `FORMULA`)](Access_to_an_internal_system_INTERNAL_FORMULA.md)


:::info
Additionally, it is worth noting that the ability to interact with internal systems can be used not only for purposes of integration but also for purposes of extensibility when platform capabilities are insufficient for some reason.
:::
