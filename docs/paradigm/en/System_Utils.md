---
slug: "/System_Utils"
title: 'Utils'
---

`Utils` is a [system module](System_modules.md) that collects frequently needed helper properties and actions: file-system access, data conversion and encoding, string operations, arithmetic, JSON access, full-text search, server diagnostics, and so on. It is pulled in via `REQUIRE Utils` (`System` and `Time` are pulled in automatically).

### File system

The `(ISTRING path, ...)` actions run on the server by default; the `isClient = TRUE` flag (or a separate client-side overload) performs the same operation on the client.

| Action / property                                         | What it does                                                                |
|-----------------------------------------------------------|------------------------------------------------------------------------------|
| `listFiles[ISTRING, BOOLEAN, BOOLEAN]` and overloads      | lists files in a directory; writes the name, directory flag, modification time, and size into the local properties `fileName`, `fileIsDirectory`, `fileModifiedDateTime`, `fileSize` keyed by `INTEGER` |
| `listFilesClient[…]`                                      | same, on the client                                                          |
| `fileExists[ISTRING]` / `fileExistsClient[ISTRING]`       | existence check; result in `fileExists[]`                                    |
| `mkdir[ISTRING]` / `mkdirClient[ISTRING]`                 | create a directory                                                           |
| `delete[ISTRING]` / `deleteClient[ISTRING]`               | delete a file or directory                                                   |
| `copy[ISTRING, ISTRING]` / `copyClient[…]`                | copy                                                                         |
| `move[ISTRING, ISTRING]` / `moveClient[…]`                | move                                                                         |
| `getFileSize[FILE]`                                       | file size in bytes, written into `fileSize[]`                                |
| `appendToFile[STRING, TEXT, STRING]`                      | append a line to a file with the given encoding                              |

### File contents and encoding

| Property / action                                         | What it does                                                                |
|-----------------------------------------------------------|------------------------------------------------------------------------------|
| `stringToFile[TEXT, STRING, STRING]` → `resultFile[]`     | serialize a string into a `FILE` with the given encoding and extension       |
| `fileToString[FILE, STRING]` → `resultString[]`           | read a `FILE` as a string                                                    |
| `linkToString[LINK]` / `richTextToString[RICHTEXT]`       | cast a link / rich-text value to `STRING`                                    |
| `readResource[STRING, BOOLEAN]` → `resource[]`            | read a resource from the classpath                                           |
| `readResourcePaths[STRING, BOOLEAN]` → `resourcePaths`    | list resources matching a pattern                                            |
| `readProperties[RAWFILE]` → `properties[STRING]`          | parse a `.properties` file into a lookup property                            |
| `encode[…, STRING]` / `decode[STRING, STRING]`            | encoding / decoding with an arbitrary encoding                               |
| `encodeBase64[…]` / `encodeBase64Unchunked[…]` / `decodeBase64[STRING]` | base64 for `RAWFILE`, `STRING`, `FILE`, `NAMEDFILE`              |
| `urlEncode[TEXT, TEXT]` → `urlEncoded[]` / `urlDecode[…]` → `urlDecoded[]` | URL encoding with the given charset                              |
| `urlParse[]` / `urlFormat[]` (over `urlFormatted` and `urlParsed`) | parse a URL into parts and reassemble it                            |
| `escapeJSONValue[TEXT]` / `escapeXMLValue[TEXT]`          | escape a string for embedding into JSON / XML                                |

### Archives

| Action                            | What it does                                                                 |
|-----------------------------------|-------------------------------------------------------------------------------|
| `zipping[STRING] <- file`         | accumulating files for the archive (`STRING` path inside the archive → contents) |
| `makeZipFile[BOOLEAN]` → `zipped[]`| build the archive; the flag controls whether to zero out file timestamps    |
| `unzipping[] <- file`             | set the archive to be unpacked                                                |
| `makeUnzipFile[]` → `unzipped[STRING]` | unpack; result: `STRING` path inside the archive → file                  |

### Console and network

| Action                                                | What it does                                                            |
|-------------------------------------------------------|--------------------------------------------------------------------------|
| `cmd[TEXT, TEXT, BOOLEAN, BOOLEAN]` and overloads     | run an OS command; result in `cmdOut[]` and `cmdErr[]`                   |
| `cmdClient[TEXT, BOOLEAN]` / `cmdClient[TEXT]`        | same, on the client                                                      |
| `ping[TEXT, BOOLEAN]` / `pingClient[TEXT]`            | host availability check; error in `pingError[]`                          |

### Excel

| Property / action                                         | What it does                                                            |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `protectExcel[EXCELFILE, STRING]` → `protectedExcel[]`    | password-protect a file                                                  |
| `mergeExcel[EXCELFILE, EXCELFILE]` → `mergedExcel[]`      | merge two files                                                          |
| `columnsCount[EXCELFILE, INTEGER]` → `columnsCount[]`     | number of columns on a sheet                                             |
| `sheetNames[EXCELFILE]` → `sheetNames[INTEGER]`           | sheet names                                                              |

### PDF and Word

| Property / action                                         | What it does                                                            |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `pagesCountPdf[PDFFILE]` → `pagesCountPdf[]`              | number of pages                                                          |
| `pdfToString[PDFFILE, BOOLEAN]`                           | recognize text from PDF                                                  |
| `wordToPdf[WORDFILE]`                                     | convert Word to PDF                                                      |

### Strings

| Property                                                  | What it does                                                            |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `length[TEXT]`                                            | string length                                                            |
| `left[TEXT, INTEGER]` / `right[TEXT, INTEGER]`            | first / last N characters                                                |
| `substr[TEXT, INTEGER, INTEGER]` / `substrFrom[TEXT, INTEGER]` | substring from a position with a length / to the end                |
| `substring[STRING, STRING]`                               | substring matched by a regular expression                                |
| `strpos[TEXT, TEXT]`                                      | position of a substring                                                  |
| `replace[TEXT, TEXT, TEXT]`                               | replace all occurrences                                                  |
| `ltrim[…]` / `rtrim[…]` / `trim[TEXT]`                    | trim whitespace on the left / right / both sides                         |
| `lpad[TEXT, INTEGER, TEXT]` / `rpad[…]`                   | pad to a given length                                                    |
| `repeat[TEXT, INTEGER]`                                   | repeat N times                                                           |
| `startsWith[…]` / `istartsWith[…]` / `endsWith[…]`        | check the start / end of a string (case-sensitive and case-insensitive)  |
| `isSubstring[…]` / `isISubstring[…]`                      | substring containment (case-sensitive and case-insensitive)              |
| `isWordInCSV[…]`                                          | containment of a value in a CSV-style string                             |
| `getWord[TEXT, TEXT, INTEGER]` / `wordCount[…]`           | split a string by a separator                                            |
| `splitPart[STRING, STRING, INTEGER]`                      | N-th fragment by a separator                                             |
| `regexpReplace[STRING, STRING, STRING, STRING]`           | replace by a regular expression                                          |
| `regexPatternMatch[TEXT, STRING]`                         | match against a regular expression                                       |
| `onlyDigits[TEXT]`                                        | flag for strings consisting only of digits                               |
| `array[STRING, STRING, INTEGER]`                          | unnest a delimited string into a row-keyed set of values                 |

For identifiers and the clipboard: `generateUUID[]` → `generatedUUID[]`, `generatePassword[INTEGER, BOOLEAN, BOOLEAN]` → `generatedPassword[]`, `copyToClipboard[TEXT]`; for cookies: `getCookie[STRING]` → `cookie[]`, `setCookie[STRING, STRING, JSON]`.

### Numbers

| Property                                                  | What it does                                                            |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `abs[…]` / `delta[…, …]`                                  | absolute value / absolute difference                                     |
| `min[…, …]` / `max[…, …]`                                 | minimum / maximum of two values                                          |
| `floor[…]` / `floor[…, …]`                                | round down; the two-argument form rounds down to a multiple of the step  |
| `ceil[…]` / `ceil[…, …]`                                  | round up; the two-argument form rounds up to a multiple of the step      |
| `round[…, …]` and `roundM1` / `round0` … `round6`         | round to N digits (including negative)                                   |
| `trunc[…, …]`                                             | truncation                                                               |
| `mod[…, …]` / `divideInteger[…, …]` / `divideIntegerNeg` / `divideIntegerRnd` | remainder and integer-division variants                  |
| `sqr[…]` / `sqrt[…]` / `power[…, …]`                      | square, square root, power                                               |
| `ln[…]` / `exp[…]`                                        | natural logarithm / exponential                                          |
| `percent[…, …]` / `share[…, …]`                           | percentage of a sum / share of the whole as a percentage                 |
| `bitwiseAnd[…, …]` / `bitwiseOr[…, …]` / `bitwiseNot[…]`  | bitwise operations                                                       |
| `toInteger[…]` / `toNumeric[…]` / `toNumericNull[…]`      | strict and nullable casts to numeric types                               |
| `toChar[…, …]`                                            | format a value by a format string (PostgreSQL `to_char`)                 |

### Iteration

`iterate[INTEGER, INTEGER, INTEGER]` — recursive enumeration of an integer range from `from` to `to`. `count[INTEGER, INTEGER]` is a special case: `iterate(i, 1, count)`. See the analogous `iterate` for dates in [`Time`](System_Time.md).

### Color

`colorToHexString[COLOR]` — color representation as `'#RRGGBB'`.

### Full-text search

| Property                                                  | What it does                                                            |
|-----------------------------------------------------------|--------------------------------------------------------------------------|
| `toTsVector[STRING, STRING]` / `toTsVector[STRING]`       | build `TSVECTOR` with the given dictionary or `english`                  |
| `toTsQuery[STRING, STRING]` / `toTsQuery[STRING]`         | build `TSQUERY`                                                          |
| `setWeight[TSVECTOR, STRING]`                             | tag parts of a vector with a weight (`'A'`–`'D'`)                        |
| `tsRank[…]` / `tsRankCD[…]` / `tsRankLN[…]`               | relevance ranking with different models                                  |
| `numNode[TSQUERY]`                                        | number of nodes in a parsed query                                        |

### JSON access properties {#json-access}

This section collects the wrappers around PostgreSQL's `jsonb_*` functions that read values out of an existing JSON. They complement the `JSON` operator, which goes the other way and builds JSON from properties or from a form. All are declared with the `FORMULA` operator.

| Property                                          | What it returns                                                                                  |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------|
| `field[JSON, STRING]`                             | value at the given key, as `JSON`                                                                |
| `field[JSON, STRING, STRING]`                     | value at the given two-key path, as `JSON`                                                       |
| `field[JSON, STRING, STRING, STRING]`             | value at the given three-key path, as `JSON`                                                     |
| `fieldText[JSON, STRING]` / `[…, STRING, STRING]` / `[…, STRING, STRING, STRING]` | same as `field` but as `STRING`              |
| `array[JSON, INTEGER]`                            | array element at the given index, as `JSON`; the index parameter is also the iteration variable  |
| `arrayText[JSON, INTEGER]`                        | same as `array` but as `STRING`                                                                  |
| `arrayElement[JSON, INTEGER]`                     | array element at the given 1-based index as a raw string (jsonb → text)                          |
| `map[JSON, STRING]`                               | value for each key-value pair of a JSON object, as `JSON`; the key parameter is also the iteration variable |
| `mapText[JSON, STRING]`                           | same as `map` but as `STRING`                                                                    |
| `merge[JSON, JSON]`                               | recursive merge of two JSON values                                                               |
| `canonicalizeJSON[JSONFILE, BOOLEAN]` / `canonicalizeJSON[JSONFILE]` → `canonicalizedJSON[]` | canonical normalization of a JSON file; the short form is equivalent to `canonicalizeJSON(file, TRUE)` (encode unicode characters) |

`field[JSON, STRING ...]` and `fieldText[JSON, STRING ...]` are scalar wrappers around `jsonb_extract_path` and `jsonb_extract_path_text`. They return the value at the given one-, two-, or three-key path. They differ only in the class of the result: `field` returns `JSON` for further composition (passing the result into `array` or into another `field`), while `fieldText` returns `STRING` directly. For paths deeper than three keys, compose `field` calls.

`array[JSON, INTEGER]` and `arrayText[JSON, INTEGER]` are table-valued wrappers around `jsonb_array_elements` and `jsonb_array_elements_text`. The `INTEGER row` parameter is not referenced in the SQL text, so the platform fills it with the row number from `ROW_NUMBER() OVER ()` — a special shortcut for an unreferenced `INTEGER` parameter with exactly that name. In practice the parameter plays a double role: an index for direct access (`array(j, 1)` returns the first element) and the iteration variable in expressions like `array(j, INTEGER o)`, where `o` is declared in place and walks every array index.

`map[JSON, STRING]` and `mapText[JSON, STRING]` are table-valued wrappers around `jsonb_each` and `jsonb_each_text`, returning `(key, value)` pairs of a JSON object. The `STRING key` parameter is unreferenced in the SQL text and therefore becomes the key column of the result; the name `key` matches the column name in the `jsonb_each` result.

`arrayElement[JSON, INTEGER]` is a separate shortcut for direct indexed access. It goes through the PostgreSQL `->` operator, takes a 1-based index, and shifts it to 0-based on its own. It returns `STRING` — the textual representation of jsonb. Useful for logging and debugging, when a raw view of the element is needed rather than a parsed value.

### Local file-class properties

Empty `LOCAL` properties for every common file class: `file[]`, `wordFile[]`, `imageFile[]`, `pdfFile[]`, `dbfFile[]`, `rawFile[]`, `excelFile[]`, `textFile[]`, `csvFile[]`, `htmlFile[]`, `jsonFile[]`, `xmlFile[]`, `tableFile[]`. Convenient as intermediate buffers for import/export without declaring an ad-hoc `LOCAL`.

### Logging

`printToLog[TEXT, STRING, STRING]` and its overloads write a message to the server log. The parameters are the message text, the logger name (default `'system'`), and the level (default `'info'`).

### Dialog forms

`dialogString`, `dialogDate`, `dialogInteger`, `dialogNumeric` are ready-made single-object forms for prompting the user for a value of the corresponding class. They are used as `DIALOG dialog<...> OBJECTS ... INPUT DO { ... }`.

### Runtime environment

`serverAvailableProcessors[]`, `serverFreeMemoryMB[]`, `serverTotalMemoryMB[]`, `serverMaxMemoryMB[]` are filled by the `readServerMemory[]` action. They expose the server JVM parameters: number of available processors and three heap-size characteristics.

### Language

- [`JSON` operator](../language/JSON_operator.md) — building a JSON value from properties or from a form.
- [`FORMULA` operator](../language/FORMULA_operator.md) — the syntax behind most properties of the module.
- [`IMPORT` operator](../language/IMPORT_operator.md) — reading a JSON file into a form or through `FIELDS … DO`.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Custom formula (FORMULA)`](Custom_formula_FORMULA.md) — the mechanism behind most properties of the module.
- [`Time`](System_Time.md) — the separate module for working with time.
