---
title: 'Access to an external system (EXTERNAL)'
---

The platform allows an lsFusion-based system to access external systems using various types of interactions / protocols, as the external counterpart to the [internal call](Internal_call_INTERNAL.md). The interface of such an access is the execution of code in the language / paradigm of the external system with specified parameters and, if necessary, the return of certain values as *results* written into the specified properties (without parameters). It is assumed that all parameter and result objects are objects of [built-in classes](Built-in_classes.md).

## Types of interactions / protocols

The platform currently supports the following types of interactions / external systems:

### HTTP - web server HTTP request {#http}

For this type of interaction, only the request string (URL) is specified, which simultaneously determines both the server address and the request to be executed. The HTTP method (`GET`, `POST`, `PUT`, `DELETE`, `PATCH`) is chosen separately; the default is `POST`. By default the request is executed on the application server, but can be redirected to the user's client instead — useful when the target is reachable from the client but not from the server.

The call timeout and SSL strictness are read from the `System.timeoutHttp[]` property (in milliseconds, with a built-in default) and the `System.insecureSSL[]` property (when truthy, disables TLS certificate verification).


:::info
Under client execution the desktop client performs the full call locally, but the regular browser client delegates to the browser's `XMLHttpRequest` and supports only what that API exposes: cookies are handled by the browser's own cookie jar, and timeouts and TLS verification by the browser itself.
:::

#### Parameters {#url}

Parameters can be passed both in the request string (to refer to the parameter, the special character `$` and the number of this parameter, starting with `1`, are used) and in its body (BODY). All parameters not used in the request string are passed to BODY, but only for HTTP methods that carry a body (`POST`, `PUT`, `PATCH`, `DELETE`); for `GET` any parameters left after URL substitution are silently dropped.

When processing file class parameters (`FILE`, `PDFFILE`, etc.) to BODY, the [content type](https://en.wikipedia.org/wiki/Media_type) of the parameter, depending on the file extension, is determined in accordance with the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties). If the file extension is not found in this table, the content type is set to `application/<file extension>`.

The file extension in this case is determined automatically, similarly to the [`WRITE` operator](WRITE_operator.md).

In each of the three cases above, if the parameter value is `NULL`, `null` is substituted for the file extension in the content type (for example, `application/null`), and an empty string is passed as the parameter itself.

Parameters of classes that differ from those of files are converted into strings and are passed as a `text/plain` content type. `NULL` values are passed as empty strings.

Custom request [headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) and [cookies](https://en.wikipedia.org/wiki/HTTP_cookie) can be supplied with the call.

The literal text of the connection string and of any body template is URL-encoded before the request is sent (suppressible); parameter values substituted via `$N` are URL-encoded independently.

#### Results

When processing a request response, results with a content type from the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties) are considered files, and can only be written to properties whose value class is `FILE`. During this process, the corresponding file extension is taken from the table mentioned above. If a particular content type is not found in the table, but it starts with `application`, the result is still considered a file, and the file extension is taken from the right part of the content type (for example, for the `application/abc` content type it will be `abc`). Results with the `application/null` content type are considered equal to `NULL`.

Results with content types different from the ones above are considered strings and on writing are automatically converted into the classes of the properties they are being written to. Empty strings are converted to `NULL`.

Response [headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) and [cookies](https://en.wikipedia.org/wiki/HTTP_cookie) can be captured into properties. The captured cookies combine the ones sent with the request and those received in `Set-Cookie` response headers; cookie attributes (`path`, `domain`, etc.) are dropped.

The HTTP [status code](https://en.wikipedia.org/wiki/List_of_HTTP_status_codes) of the response is written to the `System.statusHttp[]` property. A non-`2xx` status also throws a runtime exception with the status and response body, which can be intercepted with the [`TRY`](TRY_operator.md) operator to inspect `System.statusHttp[]` instead. Under client execution in the regular browser, a network / CORS / DNS failure surfaces as `status = 0`; in that case the exception carries a generic localized error message rather than a status + body.

#### Multiple results / parameters in BODY

If more than one parameter is passed to BODY, they are packed into a single BODY:

-   If `BODYURL` is specified — the given string is sent as BODY, with its parameters encoded as if they were [passed in the request string](#url), and `Content-Type: application/x-www-form-urlencoded`.
-   Otherwise — parameters are sent as the parts of a BODY with `Content-Type: multipart/mixed`. The `BODYPARAMNAMES` option switches the content type to `multipart/form-data` and names the first parts as form fields; the `BODYPARAMHEADERS` option attaches extra headers to individual BODY parts.

A `Content-Type` set manually through `HEADERS` overrides the above default: a `multipart/*` value forces multipart packing of the remaining parameters under that `Content-Type`; a non-`multipart/*` value replaces the default `Content-Type` of the sent BODY.

In turn, if the response content type is `multipart/*` or `application/x-www-form-urlencoded`, the response BODY is split into parts, and each part is considered a separate execution result. In this case, the order of these results is equal to the order of the corresponding parts in the response.


:::info
Note that the processing of parameters and request results is largely similar to their processing during [access from an external system](Access_from_an_external_system.md) over the HTTP protocol (here parameters are processed as results and, conversely, results are processed as parameters)
:::

### SQL - executing an SQL server command {#sql}

For this type of interaction, a connection string and the SQL command(s) to be executed are specified. Parameters can be passed both in the connection string and in the SQL command. To access the parameter, the special character `$` and the parameter number are used (starting from `1`). If the SQL command expression ends with `.sql`, it is treated as a path to a classpath resource whose contents are used as the actual command.

`EXTERNAL SQL 'LOCAL'` is not supported; to run SQL against the database used by the platform itself, use `INTERNAL DB`.

#### Parameters {#table}

File class parameters (`FILE`, `PDFFILE`, etc.) can be used only in an SQL command (not in the connection string). Furthermore, if any of the parameters, when executed, is a file in `TABLE` format (`TABLEFILE` or `FILE` with the extension `table`), that parameter is considered to be a table and in this case:

-   before executing an SQL command, the value of each such parameter is loaded onto the server into a temporary table
-   when substituting parameters, the name of the created temporary table is substituted instead of the parameter value itself

#### Results

The execution results are: for DML requests - numbers equal to the number of processed records; for SELECT requests - files in `TABLE` format (`FILE` with the extension `table`) containing the results of these requests. The order of these results is equal to the execution order of the corresponding queries in the SQL command.

### LSF - calling an action of another lsFusion server 

For this type of interaction, the following things need to be specified: the connection string for connecting to the lsFusion server (or its web server, if any), the action being executed, and the list of properties (without parameters) to whose values the results of the call will be written. The parameters passed must match the parameters of the action being performed by number and by class.

The way of defining an action in this type of interaction fully corresponds to the [way of defining](Access_from_an_external_system.md#actiontype) an action during [access from an external system](Access_from_an_external_system.md).

By default, this type of interaction is implemented via HTTP protocol using the corresponding interfaces for access [to](#http) and [from](Access_from_an_external_system.md#http) an external system.


:::info
You can also use operators for [reading](Read_file_READ.md) and [writing](Write_file_WRITE.md) files to access external systems (if file exchange is the interface for this interaction).
:::

### TCP / UDP - sending raw bytes over a socket {#tcp}

For these types of interaction, a connection string `host:port` is specified, together with a single file-class parameter whose raw bytes are sent to the socket. For `TCP`, the platform performs a single socket read (up to a 10 MB buffer) and writes the result to the `System.responseTcp[]` property; the optional `System.timeoutTcp[]` property sets the socket timeout in milliseconds. `UDP` sends the packet without waiting for a response.

By default the request is executed on the application server, but can also be performed from the user's client.


:::info
Under client execution, raw socket access is available locally in the desktop client and via a Flutter bridge in the Flutter-based web/mobile client; the regular browser client has no raw socket access and fails with `UnsupportedOperationException`.
:::

### DBF - writing rows to a `.dbf` file {#dbf}

For this type of interaction, the path to the `.dbf` file is specified as the connection string, and a single `TABLE`-format parameter (`TABLEFILE` or `FILE` with the extension `table`) supplies the rows to write. The call is declared with the `APPEND` keyword: if the file does not exist, a new file is created from the schema of the input table; if the file already exists, it is opened as-is and rows are written into its existing fields by name - the existing file's schema must already contain those fields, otherwise the call fails. The optional `CHARSET` option sets the file charset (`UTF-8` by default).

Input column names are truncated to the DBF 10-character limit before being used for both schema creation and field lookup; two columns that collide after truncation make the call fail, and a column whose original name exceeds 10 characters additionally loses its type (a 253-character string field is used as a fallback) and its value (the literal string `"null"` is written). `NULL` values in input cells are similarly written as the literal string `"null"` — harmless for string fields, but causing the write to fail for numeric fields. The input `TABLE` should therefore already use DBF-compatible field names and non-`NULL` values.

## Language

To declare an action that accesses an external system, use the [`EXTERNAL` operator](EXTERNAL_operator.md).

## Examples

```lsf
externalHTTP()  {
    // GET request with a single file result
    EXTERNAL HTTP GET 'https://www.cs.cmu.edu/~chuck/lennapg/len_std.jpg' TO exportFile;
    open(exportFile());

    // POST with a JSON body parameter; braces are escaped because of internationalization
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=getExamples' 
             PARAMS JSONFILE('\{"mode"=1\}')
             TO exportFile; 
    IMPORT FROM exportFile() FIELDS () TEXT caption, TEXT code DO
        MESSAGE 'Example : ' + caption + ', code : ' + code;
}
externalSQL ()  {
    // getting all barcodes of products with the name meat
    EXPORT TABLE FROM bc=barcode(Article a) WHERE name(a) LIKE '%Meat%';
    // reading prices for read barcodes
    EXTERNAL SQL 'jdbc:mysql://$1/test?user=root&password=' 
             EXEC 'select price AS pc, articles.barcode AS brc from $2 x JOIN articles ON x.bc=articles.barcode' 
             PARAMS 'localhost',exportFile() 
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
}
externalTCP()  {
    // send raw bytes over TCP and capture the peer's response
    EXTERNAL TCP 'example.com:9100' PARAMS RAWFILE('payload');
    MESSAGE STRING(responseTcp());
}
externalDBF()  {
    // export a table and append its rows to a .dbf file
    EXPORT TABLE FROM bc=barcode(Article a), nm=name(a);
    EXTERNAL DBF '/tmp/articles.dbf' APPEND PARAMS exportFile();
}
```
