---
title: 'Value request (REQUEST)'
---

The *value request* operator creates an [action](Actions.md) in which *requesting* value(s) is separated from directly *processing* the value(s). Accordingly, this operator allows not to perform a value request if its result *is known in advance* in the context of the call (the value of the `System.requestPushed` property is equal to `TRUE`). This, in turn, automatically gives you the following capabilities in various use cases:

- Asynchronous input

    If the user initiates a [property](Form_events.md) [change event](Form_events.md) (`CHANGE`), and processing it contains exactly one request for the value of a builtin class (the most common case), then the platform does not call it immediately, but first asks the user to input the value of the builtin class. As soon as this input is completed, the user can immediately continue his work and the input result asynchronously (in a new thread) is sent to the server, where the processing of the occurred event is started only at that moment (and not when the event actually occurred). Moreover (we call this technique *pushing* the query value), the query result is marked as known in advance (the value of the `System.requestPushed` property is set to `TRUE`), and the input value is written to the special property family: `requested`. Accordingly, since this property family is used as the default result of all [value input](Value_input.md) operators, synchronous processing is emulated; however, the ergonomics of changing data on the form is significantly improved (for example, the user can continue to input data without waiting for the calculation of all properties on the form).

- PASTE

    When the user inserts a value into a property cell using the OS tools, the platform triggers a WYSIWYG event to change this property (`CHANGEWYS`), and calls the corresponding handler on the server. In this case, the value that the user inserted is pushed as the request value.

<a className="lsdoc-anchor" id="defaultChange"/>

- Change event (`CHANGEWYS` and `CHANGE`) [default handler](Form_events.md#default) for composition

    If a property being changed is created using the [composition](Composition_JOIN.md) operator with one argument (most often a name or ID), and change event handler is not explicitly defined for it, the platform automatically creates this handler as follows: 

    - CHANGE

        The user is [requested](Form_events.md#queryValue) an object of the property value class which is used as an argument, and this property value changes to the received object 

    - CHANGEWYS

        The user is requested for an object of the class of the value of the edited property, after which the platform finds the first object which composition property value is equal to the value input by the user and then changes the value of the property used as an argument to this object.

  :::info
    In both cases, a property change means calling the `CHANGE` event handler, while the value to which the property changes is pushed as the request value.
  :::

    Creating such default handlers allows to use PASTE "out of the box" for properties whose values belong not only to built-in but also to custom classes.

- Group change (`GROUPCHANGE`) default handler

    By default, group change handler is created as follows: `CHANGE` is called first for objects' [current values](Form_structure.md#currentObject), then if the input has not been canceled, the same handler is called for all other values of objects (matching the filter), with the value of the `System.requestPushed` property set to `TRUE` (it is assumed that the result of the input does not change during its handling, thus the behavior is emulated as if the first value was pushed).


:::info
In fact, the value request operator performs only two operations: it [checks](Branching_CASE_IF_MULTI.md) `System.requestPushed` (pushing the value) for the value request action and `System.requestCanceled` (canceling the value) for the request processing action, and it is also responsible for determining the possibility of asynchronous input of the property being changed. At the same time, using this operator makes the code clearer and more readable, therefore it is recommended to use it (instead of explicit checks and options).
:::

As with other value input operators, it is possible to define [main and alternative](Value_input.md#result) actions. The first is called if the input was successfully completed, the second if not (i.e. if the input was canceled). Accordingly, it is these two actions in this operator that are responsible for processing request values.

### Implicit use {#implicit}

Note that all [value input](Value_input.md) operators can be automatically "wrapped" in the value request operator by using the corresponding option (`DO`). And since, as a rule, a value is input using one input operator, it is recommended to use this option by default, and use the value request operator explicitly only in really complex cases, for example, if there can be several input options (depending on the condition, different forms, etc.)

### Language

The syntax of the value request operator is described by the [`REQUEST` operator](REQUEST_operator.md).

### Examples

```lsf

requestCustomer (Order o)  {
    LOCAL resultValue = STRING[100] ();
    REQUEST {
        ASK 'Choose from list?' DO
            DIALOG customers OBJECTS c = resultValue() CHANGE;
        ELSE
            INPUT = resultValue() CHANGE;
    } DO
        customer(o) <- resultValue();
}

FORM request
    OBJECTS o = Order
    PROPERTIES(o) customer ON CHANGE requestCustomer(o) // for example, group adjustment will be performed
;
```
