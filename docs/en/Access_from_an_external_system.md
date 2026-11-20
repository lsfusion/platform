---
title: 'Access from an external system'
---

## Action API

The platform allows external systems to access an lsFusion-based system using various network protocols. The interface of such interaction is a call for an action with specified parameters and, if necessary, the return of certain property values (without parameters) as *results*. It is assumed that all parameter and result objects are objects of [built-in classes](Built-in_classes.md).

### Defining an action {#actiontype}

An action being called can be defined in one of the three ways:

-   `EXEC` – the name of the action is specified.
-   `EVAL` – code in the lsFusion language is specified. It is assumed that this code contains a declaration of an action named `run`. This is the action that will be called.
-   `EVAL ACTION` – action code in the lsFusion language is specified. To access a parameter, the special character `$` and the parameter number (starting from `1`) are used.

### Protocols

The platform currently supports the following network protocols:

#### HTTP {#http}

Communication over this protocol is supported both for an application server on port `7651`, as well as a web server (if any) on the same port, that has a web client installed.

The URL format, depending on the method of [action definition](#actiontype), looks as follows:

-   `EXEC` - `http://server address:port/exec?action=<action name>`. The `action` parameter must always be specified.
-   `EVAL` - `http://server address:port/eval?script=<code>`. If the `script` parameter is not specified, it is assumed that the code is passed in the first BODY parameter.
-   `EVAL ACTION` – `http://server address:port/eval/action?script=<action code>`. If the `script` parameter is not specified, it is assumed that the code is passed in the first BODY parameter.

##### Parameters {#url}

Parameters can be passed both in the request string (by appending constructs like `&p=<parameter value>` to the end of the string), as well as in the request body (BODY). It is assumed that URL parameters are substituted (in the order of their appearance in the request) for the executed action before BODY parameters.

When processing BODY parameters, parameters with the content type from the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties) are considered files and are passed to the action parameters as objects of the file class (`FILE`, `PDFFILE`, etc.). During this process, the corresponding file extension is taken from the table mentioned above. If a particular content type is not found in the table, but it starts with `application`, the parameter is still considered a file, and the file extension is taken from the right part of the content type (for example, it will be `abc` for the `application/abc` content type). Parameters with the `application/null` content type are considered to be equal to `NULL`.

BODY parameters with types of content different from the ones mentioned above are considered strings and are automatically converted into parameter classes of the called action upon being called. Empty strings are converted into `NULL`.

[Headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) of an executed request are automatically saved to the `System.headers[TEXT]` property. The name of the header is written to the only parameter of this property, and the value of the header is written to the property's value.

##### Results {#httpresult}

Properties whose values must be returned as the result are passed in the request string by adding strings like `&return=<property name>` to its end. It is assumed that the values of specified properties are returned in the order of their appearance in the request string. By default, if no result properties are specified, the resulting property is the first one with a non-`NULL` value from the following [list](Built-in_classes.md#export). 

If the result of a request is a file (`FILE`, `PDFFILE`, etc.), the response [content type](https://en.wikipedia.org/wiki/Media_type) , depending on the file extension, is determined in accordance with the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties). If the file extension is not found in this table, the content type is set to `application/<file extension>`.

The file extension in this case is determined automatically, similarly to the [`WRITE` operator](WRITE_operator.md).

In all of the three cases above, if the result value is `NULL`, a `null` string (for example, `application/null`) is substituted for the file extension in the content type, and an empty string is returned as the response itself.

Request results different from files are converted into strings and are passed as a `text/plain` content type. `NULL` values are returned as empty strings.

The values of the `System.headersTo[TEXT]` property are automatically written to the [headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) of the request result. So, the header name is read from the only parameter of this property, and the header value is read from the property value.

##### Several results / parameters in BODY

If the type of request BODY is `multipart/*` or `application/x-www-form-urlencoded`, it will be split into parts, and each part will be considered a separate request parameter. In this case, the order of these parameters is equal to the order in corresponding parts of the request BODY.

At the same time, if the number of results being returned is more than one, then the following happens:

-   If the request has a `returnmultitype=bodyurl` parameter, the response content type on transmission is set to `application/x-www-form-urlencoded` and the results are encoded as if they were [passed in the request string](#url).
-   Otherwise, the response content type during transmission is set to `multipart/mixed`, and the results are passed as internal parts of this response. 


:::info
Note that the processing of parameters and request results is largely similar to their processing during [access to an external system](Access_to_an_external_system_EXTERNAL.md) over the HTTP protocol (in this case, parameters are processed as results and vice versa, results are processed as parameters).
:::

##### Stateful API

The API described above is a REST API. Accordingly, the [change session](Change_sessions.md) is created when a call is initiated, and closes immediately after the call ends. However, there are situations where such behavior is undesirable, and you need to accumulate changes for a certain period of time (for example, while the user is inputting data), which means that the session must be saved and handed over between sessions. In order to do this, you can add a string of the following format to the end of the query string: `&session=<session ID>`, where `<session ID>` is any non-empty string. In this case, the session will not be closed after the call, but will be associated with a previously passed ID, so that all subsequent calls with this ID will be executed in this session. In order to close a session (after the end of a call), you need to add the `_close` postfix (for example,`&session=0_close`) to its ID in the request string.


:::info
Since cookie files are implicitly used for working with HTTP sessions, it is important not to forget to save / pass cookies between stateful http calls (this, however, is typically done automatically by a browser, the HttpClient in Java, etc.)
:::


:::info
The current implementation of the platform assumes that if sessions are used, the elements of the system (for example, local properties) created in the current call are deleted — that is, they are not visible in subsequent calls.
:::

##### Authentication

When executing an http request, it is often necessary to identify the user on whose behalf the specified action will be executed. At the moment, two types of authentication are supported by the platform:

-   [Basic identification](https://en.wikipedia.org/wiki/Basic_access_authentication) - the user name and password are passed in an encoded form in the `Authorization: Basic <credentials>` heading.
-   Token-based authentication consists of two stages:
    -   At the first stage, you need to execute the `Authentication.getAuthToken[]` action with basic authentication. The result of this action will be an authentication token with a fixed lifetime (one day [by default](Working_parameters.md#authTokenExpiration)). An example of a request:  `http://localhost/exec?action=getAuthToken`.
    -   The token you receive can be used for authentication during its lifetime by passing it in the `Authorization: Bearer <token>` header (similarly to JWT which is used in the current implementation of the platform for generating authentication tokens).

## Examples

### Action API (Python)
```python
import json
import requests
from requests_toolbelt.multipart import decoder

lsfCode = ("run(INTEGER no, DATE date, FILE detail) {\n"
           "    NEW o = FOrder {\n"
           "        no(o) <- no;\n"
           "        date(o) <- date;\n"
           "        LOCAL detailId = INTEGER (INTEGER);\n"
           "        LOCAL detailQuantity = INTEGER (INTEGER);\n"
           "        IMPORT JSON FROM detail TO detailId, detailQuantity;\n"
           "        FOR imported(INTEGER i) DO {\n"
           "            NEW od = FOrderDetail {\n"
           "                id(od) <- detailId(i);\n"
           "                quantity(od) <- detailQuantity(i);\n"
           "                price(od) <- 5;\n"
           "                order(od) <- o;\n"
           "            }\n"
           "        }\n"
           "        APPLY;\n"
           "        EXPORT JSON FROM price = price(FOrderDetail od), id = id(od) WHERE order(od) == o;\n"
           "        EXPORT FROM orderPrice(o), exportFile();\n"
           "    }\n"
           "}")

order_no = 354
order_date = '10.10.2017'
order_details = [dict(id=1, quantity=10),
                 dict(id=2, quantity=15),
                 dict(id=5, quantity=4),
                 dict(id=10, quantity=18),
                 dict(id=11, quantity=1),
                 dict(id=12, quantity=3)]

order_json = json.dumps(order_details)

url = 'http://localhost:7651/eval'
payload = {'script': lsfCode, 'no': str(order_no), 'date': order_date,
           'detail': ('order.json', order_json, 'text/json')}

response = requests.post(url, files=payload)
multipart_data = decoder.MultipartDecoder.from_response(response)

sum_part, json_part = multipart_data.parts
sum = int(sum_part.text)
data = json.loads(json_part.text)

##############################################################

print(sum)
for item in data:
    print('{0:3}: price {1}'.format(int(item['id']), int(item['price'])))

##############################################################
# 205
#   4: price 5
#  18: price 5
#   3: price 5
#   1: price 5
#  10: price 5
#  15: price 5
```
