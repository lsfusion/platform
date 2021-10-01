---
title: 'EXTERNAL operator'
---

The `EXTERNAL` operator creates an [action](Actions.md) that implements [accessing to an external system](Access_to_an_external_system_EXTERNAL.md). 

### Syntax

    EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1. ..., propertyIdM]

`externalCall` - an external call defined by one of the following syntaxes:

    HTTP [requestType] connectionStrExpr [BODYURL bodyStrExpr] [HEADERS headersPropertyId] [COOKIES cookiesPropertyId] [HEADERSTO headersToPropertyId] [COOKIESTO cookiesToPropertyId]
    TCP [CLIENT] connectionStrExpr
    UDP [CLIENT] connectionStrExpr
    SQL connectionStrExpr EXEC execStrExpr
    LSF connectionStrExpr lsfExecType execStrExpr

### Description

The `EXTERNAL` operator creates an action that makes a request to an external system.

### Parameters

- `HTTP`

    Keyword. Specifies that the operator is executing a web server HTTP request.

- `requestType`

    Keyword. Defines the [method](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods) of the HTTP request:

    - `POST`
    - `GET`
    - `PUT`
    - `DELETE`

  The default value is `POST`.

- `TCP`

  Keyword. Specifies that the operator is executing a TCP request.

- `UDP`

  Keyword. Specifies that the operator is executing a UDP request.

- `CLIENT`

  Keyword. Specifies that the request is executing on the client. By default, the request is executed on the server.

- `SQL`

    Keyword. Specifies that the operator executes an SQL server command or commands.

- `LSF`

    Keyword. Specifies that the operator executes an action of another lsFusion server.

- `connectionStrExpr`  

    [Expression](Expression.md). `HTTP`: http request string. `SQL`: DBMS connection string. `LSF`: URL of an lsFusion server (application).

- `bodyStrExpr`

    [Expression](Expression.md). Continuation of http request string in BODY. Relevant when BODY has > 1 parameter. If not specified, the parameters are passed in multipart format.

- `headersPropertyId`
- `headersToPropertyId`

    [Property ID](IDs.md#propertyid) containing request headers. The property must have exactly one parameter: the name of the request's header. This parameter must belong to a string class. If the property is not specified, headers are ignored/not set.

- `cookiesPropertyId`
- `cookiesToPropertyId`

    [Property ID](IDs.md#propertyid) containing request cookies. The property must have exactly one parameter: the name of the cookie. This parameter must belong to a string class. If the property is not specified, cookies are ignored/not set.

- `lsfExecType`

    Keyword. Specifies the [way of defining](Access_from_an_external_system.md#actiontype) the action:

    - `EXEC` – the name of the action is specified.
    - `EVAL` – the code of the action is specified in the **lsFusion** language. It is assumed that this code contains a declaration of an action named `run`. This is the action that will be called.
    - `EVAL ACTION` – the action code in the **lsFusion** language is specified. To access a parameter, the special character `$` and the parameter number (starting from `1`) are used.

- `execStrExpr`  

    Expression. `SQL`: SQL query command(s). `LSF`: The name of an action or code, depending on how the action is defined.

- `paramExpr1, ..., paramExprN`

    List of expressions whose values will be used as the call parameters.

- `propertyId1, ..., propertyIdM`

    List of property IDs (without parameters) to which the results will be written.

### Examples

```lsf
testExportFile = DATA FILE ();

externalHTTP()  {
    EXTERNAL HTTP GET 'https://www.cs.cmu.edu/~chuck/lennapg/len_std.jpg' TO exportFile;
    open(exportFile());
    // braces are escaped as they are used in internationalization
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=getExamples' 
             PARAMS JSONFILE('\{"mode"=1,"locale"="en"\}') TO exportFile; 
    IMPORT FROM exportFile() FIELDS () TEXT caption, TEXT code DO
        MESSAGE 'Example : ' + caption + ', code : ' + code;

    // passes the second and third parameters to BODY url-encoded
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=doSomething&someprm=$1' 
             BODYURL 'otherprm=$2&andonemore=$3' PARAMS 1,2,'3'; 
}
externalSQL ()  {
    // getting all barcodes of products with the name meat
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Meat%';
    // reading prices for read barcodes 
    EXTERNAL SQL 'jdbc:mysql://$1/test?user=root&password=' 
             EXEC 'select price AS pc, articles.barcode AS brc from $2 x JOIN articles ON x.bc=articles.barcode' 
             PARAMS 'localhost', exportFile() 
             TO exportFile; 

    // writing prices for all products with received barcodes
    LOCAL price = INTEGER (INTEGER);
    LOCAL barcode = STRING[30] (INTEGER);
    IMPORT FROM exportFile() TO price=pc,barcode=brc;
    FOR barcode(Article a) = barcode(INTEGER i) DO
        price(a) <- price(i);
}
externalLSF()  {
    EXTERNAL LSF 'http://localhost:7651' EXEC 'System.testAction[]';
};
```
