---
slug: "/Brief_integration"
title: 'Brief: integration'
---

## Access from outside

From outside, an [action](../paradigm/Actions.md) is called with parameters, and the values of the listed properties without parameters come back as the result. The action is specified in one of three ways: `EXEC` — by name, `EVAL` and `EVAL ACTION` — by supplied lsFusion code (from inside, the same code is run by the [`EVAL` operator](../language/EVAL_operator.md)).

Requests are served by the application server (port `7651`) and by the web server:

- Action API — `/exec`, `/eval`, `/eval/action`, both servers;
- Form API — `/form`, web server only: driving a form in the interactive view;
- File API — `/files/list`, `/files/read`, `/files/search`, web server only: reading the application's classpath.

Whether such a call is accepted at all is set by a working parameter — [`enableAPI`](../paradigm/Working_parameters.md) for the Action and File APIs, `enableUI` for the Form API, which the platform counts as user interface rather than as a program interface. An action that turns out to be interactive is routed to a running client of the user, and needs `enableUI` on top of the `enableAPI` check. The [`@@api`](../language/Action_options.md) action option allows one specific action at `enableAPI=0` (an authenticated user is still required), and `@@noauth` bypasses both the authentication check and `enableAPI`. The annotation marks a named action, so it does not extend to `/eval` and `/eval/action`, which run arbitrary code, nor to the File API, where no action is called. The mechanism is [access from an external system](../paradigm/Access_from_an_external_system.md).

From the same JVM or the same SQL server the elements are reached directly — by Java code ([Java API for integrations](../paradigm/Java_integration_API.md)) or by SQL against the platform's tables ([access from an internal system](../paradigm/Access_from_an_internal_system.md)).

## External calls (EXTERNAL)

The [`EXTERNAL` operator](../language/EXTERNAL_operator.md) creates an action performing a single call to an external system: parameters go in `PARAMS`, results into the properties without parameters listed in `TO`.

```
EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

`externalCall`:

```
HTTP [CLIENT] [requestType] connectionStrExpr httpOption1 ... httpOptionN
TCP [CLIENT] connectionStrExpr
UDP [CLIENT] connectionStrExpr
SQL connectionStrExpr EXEC execStrExpr
LSF connectionStrExpr lsfExecType execStrExpr
DBF connectionStrExpr APPEND [CHARSET charsetLiteral]
```

`HTTP` is a request to the given string, `TCP` and `UDP` send a file's bytes to a socket, `SQL` runs a command on a third-party SQL server, `LSF` calls an action on another lsFusion server, `DBF` appends table rows to a `.dbf` file ([access to an external system](../paradigm/Access_to_an_external_system_EXTERNAL.md)). `SQL`, `TCP` and `DBF` connections are reused inside a [`NEWCONNECTION`](../language/NEWCONNECTION_operator.md) block; a file is fetched by URL with the [`READ` operator](../language/READ_operator.md).

```lsf
readRate () {
    EXTERNAL HTTP GET r'https://www.lsfusion.org/rate?cur=$1' PARAMS r'USD' TO exportFile;
}
```

## Internal calls (INTERNAL)

The [`INTERNAL` operator](../language/INTERNAL_operator.md) runs code inside the deployment's own components: Java in the application-server JVM, a JavaScript function or a resource in the user's web client (`CLIENT`), SQL against the platform's own database (`DB`).

```
INTERNAL [CLIENT] [syncType] className [(classId1, ..., classIdN)] [NULL]
INTERNAL [syncType] <{anyTokens}> [NULL]
INTERNAL internalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

The Java target is a class extending `InternalAction`, given by its name or as an inline code fragment in `<{ }>`. Such code writes values straight into lsFusion properties within the same [change session](Brief_sessions.md); what it can reach is in [Java API for integrations](../paradigm/Java_integration_API.md). The mechanism of all three types is [internal call (`INTERNAL`)](../paradigm/Internal_call_INTERNAL.md), and the operator's place next to `FORMULA` is [access to an internal system](../paradigm/Access_to_an_internal_system_INTERNAL_FORMULA.md).

```lsf
setNoCancelInTransaction() INTERNAL <{ context.getSession().setNoCancelInTransaction(true); }>;
```

## Declarative integration (FORMULA, CUSTOM, JSON)

The [`FORMULA` operator](../language/FORMULA_operator.md) creates a property computed by an SQL expression, possibly a different one per DBMS; the table-valued form maps the property onto a whole table ([custom formula](../paradigm/Custom_formula_FORMULA.md)).

```
FORMULA [NULL] [className [valueId]] implList [( paramList )] [NULL]
```

`CUSTOM` hands rendering to a JavaScript function in the client: on an object group as `CUSTOM renderFunction [OPTIONS optionsExpr]` ([object blocks](../language/Object_blocks.md)), on a property as `CUSTOM renderFunction [CHANGE [editFunction]]` ([properties and actions block](../language/Properties_and_actions_block.md)). The function is given the view's own local controller, which reads and changes what this view shows; the form controller — and with it the server calls — is reached from it as `controller.form` ([How-to: Custom Components](../how-to/How-to_Custom_components_objects.md)).

The [`JSON` and `JSONTEXT` operators](../language/JSON_operator.md) create a property building JSON out of a list of properties or out of a form.

```
jsonKeyword FROM [columnId1 =] propertyExpr1, ..., [columnIdN =] propertyExprN
  [WHERE whereExpr]
  [ORDER orderExpr1 [DESC], ..., orderExprL [DESC]]
  [TOP topExpr] [OFFSET offsetExpr]
jsonKeyword ( formName [OBJECTS objName1 = expr1, ..., objNameK = exprK]
  [FILTERS filterExpr1, ..., filterExprP]
  [TOP topSelect] [OFFSET offsetSelect] )
```

`jsonKeyword` is `JSON` or `JSONTEXT`.
