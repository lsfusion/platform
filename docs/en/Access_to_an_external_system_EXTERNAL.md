---
title: 'Access to an external system (EXTERNAL)'
---

The operator for *accessing an external system* executes a specified code in the language/paradigm of the external system specified. In addition, this operator allows passing objects of [built-in classes](Built-in_classes.md) as parameters of this type of call, and also writing the *results* of calls to the properties specified (without parameters).

Currently the platform supports the following types of interactions/external systems:

## HTTP - web server HTTP request {#http}

For this type of interaction, only the request string (URL) is specified, which simultaneously determines both the server address and the request to be executed.

### Parameters {#url}

Parameters can be passed both in the request string (to refer to the parameter, the special character `$` and the number of this parameter, starting with `1`, are used) and in its body (BODY). It is assumed that all parameters not used in the request string are passed to BODY.

When processing file class parameters (`FILE`, `PDFFILE`, etc.) to BODY, the [content type](https://en.wikipedia.org/wiki/Media_type) of the parameter, depending on the file extension, is determined in accordance with the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties). If the file extension is not found in this table, the content type is set to `application/<file extension>`.

The file extension in this case is determined automatically, similarly to the [`WRITE` operator](WRITE_operator.md).

In each of the three cases above, if the parameter value is `NULL`, `null` is substituted for the file extension in the content type (for example, `application/null`), and an empty string is passed as the parameter itself.

Parameters of classes that differ from those of files are converted into strings and are passed as a `text/plain` content type. `NULL` values are passed as empty strings.

If necessary, using the special `HEADERS` option you can define the [headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) of the request being executed. To do this, you need to specify a property with exactly one parameter of the string class in which the header will be stored, and with the value of the string class in which the value of this header will be stored.

### Results

When processing a request response, results with a content type from the following [table](https://github.com/lsfusion/platform/blob/master/api/src/main/resources/MIMETypes.properties) are considered files, and can only be written to properties whose value class is `FILE`. During this process, the corresponding file extension is taken from the table mentioned above. If a particular content type is not found in the table, but it starts with `application`, the result is still considered a file, and the file extension is taken from the right part of the content type (for example, for the `application/abc` content type it will be `abc`). Results with the `application/null` content type are considered equal to `NULL`.

Results with content types different from the ones mentioned above are considered strings and when writing are automatically converted into the classes with the value of the properties to which they are being written. Empty strings are converted to `NULL`.

If necessary, using the special `HEADERSTO` option you can write the [headers](https://en.wikipedia.org/wiki/List_of_HTTP_header_fields) of the request response received to the specified property. This property must have exactly one parameter of the string class in which the header will be stored, and the value of the string class in which the value of this header will be stored.

All results are returned in UTF-8 encoding.

### Multiple results/parameters in BODY

If more than one parameter is passed to BODY, then:

-   If the option `BODYURL` is specified, the BODY content type on transmission is set to `application/x-www-form-urlencoded`, and the specified string, in which the parameters are encoded as if they were [passed in the request string](#url), is passed as BODY.
-   Otherwise, during transmission the response content type is set to `multipart/mixed` and the parameters are passed as components of this BODY. 

In turn, if the request response type is `multipart/*` or `application/x-www-form-urlencoded`, it will be split into parts, and each part will be considered a separate execution result. In this case, the order of these results is equal to the order of the corresponding parts in the request response.


:::info
Note that the processing of parameters and request results is largely similar to their processing during [access from an external system](Access_from_an_external_system.md) over the HTTP protocol (here parameters are processed as results and, conversely, results are processed as parameters)
:::

## SQL - executing an SQL server command 

For this type of interaction, a connection string and the SQL command(s) to be executed are specified. Parameters can be passed both in the connection string and in the SQL command. To access the parameter, the special character `$` and the parameter number are used (starting from `1`).

### Parameters {#table}

File class parameters (`FILE`, `PDFFILE`, etc.) can be used only in an SQL command (not in the connection string). Furthermore, if any of the parameters, when executed, is a file in `TABLE` format (`TABLEFILE` or `FILE` with the extension `table`), that parameter is considered to be a table and in this case:

-   before executing an SQL command, the value of each such parameter is loaded onto the server into a temporary table
-   when substituting parameters, the name of the created temporary table is substituted instead of the parameter value itself

### Results

The execution results are: for DML requests - numbers equal to the number of processed records; for SELECT requests - files in `TABLE` format (`FILE` with the extension `table`) containing the results of these requests. The order of these results is equal to the execution order of the corresponding queries in the SQL command.

The predefined `LOCAL` value may be used as the connection string. In this case the connection will be made to the database server used by the platform.

## LSF - calling an action of another lsFusion server 

For this type of interaction, the following things need to be specified: the connection string for connecting to the lsFusion server (or its web server, if any), the action being executed, and the list of properties (without parameters) to whose values the results of the call will be written. The parameters passed must match the parameters of the action being performed by number and by class.

The way of defining an action in this type of interaction fully corresponds to the [way of defining](Access_from_an_external_system.md#actiontype) an action during [access from an external system](Access_from_an_external_system.md).

By default, this type of interaction is implemented via HTTP protocol using the corresponding interfaces for access [to](#http) and [from](Access_from_an_external_system.md#http) an external system.


:::info
You can also use operators for [reading](Read_file_READ.md) and [writing](Write_file_WRITE.md) files to access external systems (if file exchange is the interface for this interaction).
:::

## Language

To declare an action that accesses an external system, use the [`EXTERNAL` operator](EXTERNAL_operator.md).

## Examples

```lsf
testExportFile = DATA FILE ();

externalHTTP()  {
    EXTERNAL HTTP GET 'https://www.cs.cmu.edu/~chuck/lennapg/len_std.jpg' TO exportFile;
    open(exportFile());

    // braces are escaped as they are used in internationalization
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=getExamples' 
             PARAMS JSONFILE('\{"mode"=1,"locale"="en"\}')
             TO exportFile; 
    IMPORT FROM exportFile() FIELDS () TEXT caption, TEXT code DO
        MESSAGE 'Example : ' + caption + ', code : ' + code;

    // passes the second and third parameters to BODY url-encoded
    EXTERNAL HTTP 'http://tryonline.lsfusion.org/exec?action=doSomething&someprm=$1' 
             BODYURL 'otherprm=$2&andonemore=$3' 
             PARAMS 1,2,'3'; 
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
};
```
