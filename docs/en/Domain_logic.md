---
title: 'Domain logic'
---

[Properties](Properties.md) are the basic concept for business logic and are responsible for storing and calculating data. 

An important feature of properties is that they do not change the data in the system in any way – for this, the platform has [actions](Actions.md). 

Actions answer the question of what to do, but do not answer the question of when to do it. To define such moments, the platform has [events](Events.md). 

As a rule, the business logic does not allow just any data changes: only changes subject to certain rules. The platform uses [constraints](Constraints.md) to define these rules.

Accordingly, the summary table for all elements of the business logic is as follows:

|System elements|Answer the question        |Character           |
|---------------|---------------------------|--------------------|
|Properties     |What to store and calculate|static / declarative|
|Actions        |What to do                 |dynamic / imperative|
|Events         |When to do                 |dynamic / imperative|
|Constraints    |What can be done           |static / declarative|

### Stack

import DomainENSvg from './images/DomainEn.svg';

<DomainENSvg /> 
