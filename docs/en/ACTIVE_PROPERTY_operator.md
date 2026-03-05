---
title: 'ACTIVE PROPERTY operator'
---

The `ACTIVE PROPERTY` operator creates a [property](Properties.md) that checks if specified property is [active](Activity_ACTIVE.md).

### Syntax 

```
ACTIVE PROPERTY formPropertyId
```

### Description

The `ACTIVE PROPERTY` operator creates a property that returns `TRUE` if the specified property is active on a [form](Forms.md). 

### Parameters

- `formName`

    Form name. [Composite ID](IDs.md#cid).

- `formPropertyId`  

    The global [ID of a property or action on a form](IDs.md#formpropertyid) which activity is checked.

### Examples

```lsf
FORM users
OBJECTS c = CustomUser
PROPERTIES(c) name, login;

activeLogin = ACTIVE PROPERTY users.login(c);
EXTEND FORM users
PROPERTIES() activeLogin;
```
