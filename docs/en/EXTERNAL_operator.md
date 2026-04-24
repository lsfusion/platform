---
title: 'EXTERNAL operator'
---

The `EXTERNAL` operator creates an [action](Actions.md) that implements [accessing to an external system](Access_to_an_external_system_EXTERNAL.md). 

### Syntax

```
EXTERNAL externalCall [PARAMS paramExpr1, ..., paramExprN] [TO propertyId1, ..., propertyIdM]
```

`externalCall` - an external call defined by one of the following syntaxes:

```
HTTP [CLIENT] [requestType] connectionStrExpr httpOption1 ... httpOptionN
TCP [CLIENT] connectionStrExpr
UDP [CLIENT] connectionStrExpr
SQL connectionStrExpr EXEC execStrExpr
LSF connectionStrExpr lsfExecType execStrExpr
DBF connectionStrExpr APPEND [CHARSET charsetLiteral]
```

The HTTP options (in any order, separated by spaces or line feeds) are:

```
BODYURL bodyStrExpr
BODYPARAMNAMES bodyParamNameExpr1, ..., bodyParamNameExprK
BODYPARAMHEADERS bodyParamHeadersPropertyId1, ..., bodyParamHeadersPropertyIdK
HEADERS headersPropertyId
COOKIES cookiesPropertyId
HEADERSTO headersToPropertyId
COOKIESTO cookiesToPropertyId
NOENCODE
```

### Description

The `EXTERNAL` operator creates an action that makes a request to an external system.

### Parameters

- `HTTP`, `TCP`, `UDP`, `SQL`, `LSF`, `DBF`

    Keywords. Select the type of external call; see [Access to an external system](Access_to_an_external_system_EXTERNAL.md) for the semantics of each.

- `requestType`

    Keyword. Defines the [method](https://en.wikipedia.org/wiki/Hypertext_Transfer_Protocol#Request_methods) of the HTTP request:

    - `POST`
    - `GET`
    - `PUT`
    - `DELETE`
    - `PATCH`

  The default value is `POST`.

- `CLIENT`

  Keyword. Runs the call on the user's client. Without `CLIENT` the call runs on the application server.

- `APPEND`

    Required keyword in the `DBF` call syntax.

- `charsetLiteral`

    [String literal](Literals.md#strliteral) with the charset used for the `.dbf` file. Defaults to `UTF-8`.

- `connectionStrExpr`  

    [Expression](Expression.md). `HTTP`: http request string. `TCP` / `UDP`: `host:port` of the target socket. `SQL`: DBMS connection string. `LSF`: URL of an lsFusion application or web server. `DBF`: path to the `.dbf` file.

- `bodyStrExpr`

    [Expression](Expression.md). BODY string with `$N` parameter substitutions. All parameters remaining after URL substitution must be consumed inside this string, otherwise the call fails. For `GET` `BODYURL` has no effect and any remaining parameters are silently dropped.

- `bodyParamNameExpr1, ..., bodyParamNameExprK`

    List of [expressions](Expression.md). Each evaluates to a BODY part name in the form `'name'` or `'name;filename'` (the part after `;` sets the multipart file name). Without `BODYPARAMNAMES` BODY parts get default auto-names `param0`, `param1`, ... (file parts without an explicit filename similarly get `file0`, `file1`, ...).

- `bodyParamHeadersPropertyId1, ..., bodyParamHeadersPropertyIdK`

    List of [property IDs](IDs.md#propertyid). Each property has one string-class parameter (the header name) and a string-class value (the header value). Without `BODYPARAMHEADERS` no extra headers are attached to BODY parts.

- `headersPropertyId`, `headersToPropertyId`

    [Property IDs](IDs.md#propertyid). Each property has one string-class parameter (the header name) and a string-class value (the header value). Without `HEADERS` no custom request headers are sent; without `HEADERSTO` response headers are not captured.

- `cookiesPropertyId`, `cookiesToPropertyId`

    [Property IDs](IDs.md#propertyid). Each property has one string-class parameter (the cookie name) and a string-class value (the cookie value). Without `COOKIES` no custom cookies are sent; without `COOKIESTO` cookies are not captured.

- `NOENCODE`

    Keyword. Skips URL pre-encoding of the literal text of the connection string and `BODYURL` string (parameter values substituted via `$N` are still URL-encoded separately). Without `NOENCODE` the literal text is URL-encoded before sending.

- `lsfExecType`

    Keyword. Specifies the [way of defining](Access_from_an_external_system.md#actiontype) the action:

    - `EXEC` – the name of the action is specified.
    - `EVAL` – the code of the action is specified in the **lsFusion** language. It is assumed that this code contains a declaration of an action named `run`. This is the action that will be called.
    - `EVAL ACTION` – the action code in the **lsFusion** language is specified. To access a parameter, the special character `$` and the parameter number (starting from `1`) are used.

- `execStrExpr`  

    [Expression](Expression.md). `SQL`: SQL command(s) — an expression ending in `.sql` is treated as a path to a classpath resource whose contents are used as the command. `LSF`: action name or code, per `lsfExecType`.

- `paramExpr1, ..., paramExprN`

    List of expressions whose values will be used as the call parameters.

- `propertyId1, ..., propertyIdM`

    List of property IDs (without parameters) to which the results will be written.

### Examples

```lsf
externalHTTP()  {
    // HTTP method + TO result
    EXTERNAL HTTP GET 'https://www.cs.cmu.edu/~chuck/lennapg/len_std.jpg' TO exportFile;

    // BODYURL: second and third parameters are consumed into a url-encoded BODY via $N
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=doSomething&someprm=$1' 
             BODYURL 'otherprm=$2&andonemore=$3' PARAMS 1, 2, '3';

    // BODYPARAMNAMES with 'name;filename' + manual Content-Type via HEADERS
    LOCAL headers = TEXT(STRING[100]);
    headers('Content-Type') <- 'multipart/form-data; charset=UTF-8';
    EXTERNAL HTTP POST 'https://api.example/upload'
             BODYPARAMNAMES 'document;report.pdf'
             HEADERS headers
             PARAMS exportFile();

    // HEADERSTO / COOKIESTO capture response headers and cookies
    LOCAL respHeaders = TEXT(STRING[100]);
    LOCAL respCookies = TEXT(STRING[100]);
    EXTERNAL HTTP GET 'https://api.example/login'
             HEADERSTO respHeaders
             COOKIESTO respCookies
             TO exportFile;
}
externalTCP()  {
    EXTERNAL TCP 'example.com:9100' PARAMS RAWFILE('payload');
    MESSAGE STRING(responseTcp());
}
externalSQL ()  {
    // inline SQL command with a TABLE parameter loaded into a temporary table
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Meat%';
    EXTERNAL SQL 'jdbc:mysql://$1/test?user=root&password=' 
             EXEC 'select price AS pc, articles.barcode AS brc from $2 x JOIN articles ON x.bc=articles.barcode' 
             PARAMS 'localhost', exportFile() 
             TO exportFile;
}
externalLSF()  {
    EXTERNAL LSF 'http://localhost:7651' EXEC 'System.testAction[]';
}
externalDBF()  {
    EXPORT TABLE FROM bc=barcode(Article a), nm=name(a);
    EXTERNAL DBF '/tmp/articles.dbf' APPEND CHARSET 'CP866' PARAMS exportFile();
}
```
