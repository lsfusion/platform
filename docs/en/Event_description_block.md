---
title: 'Event description block'
---

*Event description block* describes an [event](Events.md) in different statements.

### Syntax

    [GLOBAL | LOCAL] [FORMS formName1, ..., formNameN] [GOAFTER propertyId1, ..., propertyIdM]

### Parameters

- `GLOBAL`

    The keyword specifying that the described event will be global. This is the default behavior.

- `LOCAL`

    The keyword specifying that the described event will be local.

- `formName1, ..., formNameN`

    A list of names of the  [forms](Forms.md) in which the event will occur. Each element of the list is a  [composite ID](IDs.md#cid). If the list is not defined, the event will occur in all forms.

- `propertyId1, ..., propertyIdM`

    List of [property IDs](IDs.md#propertyid). This list means that all event handlers that change one of the specified properties must be executed earlier than the handlers that will be defined in the statement for which this event description block is being defined.
