---
title: 'WRITE operator'
---

The `WRITE` operator creates an [action](Actions.md) that [writes a file](Write_file_WRITE.md) from a property to an external resource. 

### Syntax

```
WRITE [CLIENT [DIALOG]] fileExpr TO urlExpr [APPEND]
```

### Description

The `WRITE` operator creates an action that writes a file from the property to an external resource located at the specified URL.

The following URL types are supported:

```
[file://]path_to_file
ftp://username:password[;charset]@host:port[/path_to_file][?param1=value1&param2=value2&...]
ftps://username:password[;charset]@host:port[/path_to_file][?param1=value1&param2=value2&...]
sftp://username:password[;charset]@host:port[/path_to_file]
```

For `ftp` and `ftps`, the supported query parameters are `passivemode`, `binarytransfermode`, `datatimeout`, `connecttimeout`.

If the part of the URL after the last period already looks like a file extension (either a standard MIME extension, or a short — fewer than 4 characters — suffix with more letters than digits), it is used as is. Otherwise the period `.` is treated as part of the file name, and the extension is determined automatically based on the class of the file being written:

| Extension                                  | Class       |
|--------------------------------------------|-------------|
| read from the passed object                | `FILE`      |
| json                                       | `JSONFILE`  |
| xml                                        | `XMLFILE`   |
| csv                                        | `CSVFILE`   |
| xls or xlsx, depending on the file content | `EXCELFILE` |
| dbf                                        | `DBFFILE`   |
| table                                      | `TABLEFILE` |
| html                                       | `HTMLFILE`  |
| doc or docx, depending on the file content | `WORDFILE`  |
| jpg                                        | `IMAGEFILE` |
| pdf                                        | `PDFFILE`   |
| mp4                                        | `VIDEOFILE` |

The `Downloads` folder in the user folder is considered to be the current folder on the client side.

### Parameters

- `CLIENT`

    Keyword. If specified, the action will be executed on the client side. By default, the action is executed on the server.

- `DIALOG`

    Keyword. If specified, before writing the file a dialog will be shown in which the user can change the specified URL. This can be used only when writing to the disk (the URL type is file). By default, the dialog is not shown. 

- `urlExpr`

    An [expression](Expression.md) whose value equals to the URL.

- `fileExpr`

    An [expression](Expression.md) whose value is the file that will be written to an external resource. JSON-typed values are also accepted: in this case the value is serialized to a `.json` file.

- `APPEND`

    Keyword. If specified, the file is re-read from `fileExpr` and appended to the file at `urlExpr`. Supported only when writing to the file system (URL type `file`); for `ftp`, `ftps`, and `sftp`, using `APPEND` raises a runtime error. It also cannot be combined with `CLIENT DIALOG`. Behavior by file extension:

    - **csv**, **txt** — data is appended to the end of the file;
    - **xls**, **xlsx** — all sheets from the `fileExpr` file are copied into the file at the specified `urlExpr`;
    - **docx** — the contents of the `fileExpr` document are appended to the document at the specified `urlExpr`;
    - **pdf** — the pages of the `fileExpr` document are appended to the document at the specified `urlExpr`;
    - not supported for other extensions.

    If the file at the specified `urlExpr` does not yet exist, `WRITE` with `APPEND` works for any file type: a new file with the contents of `fileExpr` is created.

    By default, the file is rewritten.

### Examples

```lsf
loadAndWrite ()  {
    INPUT f = FILE DO {
        WRITE f TO 'file:///home/user/loadedfile.csv' APPEND;
        WRITE CLIENT f TO '/home/user/loadedfile.txt';
        WRITE CLIENT DIALOG f TO 'loadedfile';
    }
}
```
