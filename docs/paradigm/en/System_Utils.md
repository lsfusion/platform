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
| `encode[…, STRING]` / `decode[STRING, STRING]`            | encode binary data into a `STRING` and decode it back into a `RAWFILE`; the second argument is the encoding format (`base64`, `hex`, `escape`); PG `encode($1,$2)` / `decode($1,$2)` |
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

Almost all of these properties are declared with `FORMULA` and, on PostgreSQL, translate into a same-named SQL function or operator. Where a property maps directly to a PostgreSQL expression, it is given at the end of the description after `; PG` (`$1`, `$2`, … are the parameters in order).

| Property                                                  | What it does                                                                                                                          |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `length[TEXT]`                                            | string length in characters; PG `length($1)`                                                                                          |
| `left[TEXT, INTEGER]` / `right[TEXT, INTEGER]`            | first / last `n` characters of a string; for a negative `n` PostgreSQL drops the last / first `\|n\|` characters; PG `left($1,$2)` / `right($1,$2)` |
| `substr[TEXT, INTEGER, INTEGER]` / `substrFrom[TEXT, INTEGER]` | substring of length `len` starting at position `from` (1-based) / substring from position `from` to the end; PG `substring($1,$2,$3)` / `substring($1,$2)` |
| `substring[STRING, STRING]`                               | the first substring matching a POSIX regular expression; `NULL` if there is no match; PG `substring($1, $2)`                           |
| `strpos[TEXT, TEXT]`                                      | position of the first occurrence of the substring (1-based); `0` if not found; PG `strpos($1,$2)`                                       |
| `replace[TEXT, TEXT, TEXT]`                               | replace all occurrences of `from` with `to`; PG `replace($1,$2,$3)`                                                                    |
| `ltrim[…]` / `rtrim[…]` / `trim[TEXT]`                    | trim whitespace on the left / right / both sides; PG `ltrim($1)` / `rtrim($1)` / `trim($1)`                                            |
| `ltrim[TEXT, TEXT]` / `rtrim[TEXT, TEXT]`                 | trim, on the left / right, any of the characters listed in the second argument; PG `ltrim($1,$2)` / `rtrim($1,$2)`                      |
| `lpad[TEXT, INTEGER, TEXT]` / `rpad[…]`                   | pad with `fill` on the left / right up to length `len`; if the source string is longer than `len`, it is truncated; PG `lpad($1,$2,$3)` / `rpad($1,$2,$3)` |
| `repeat[TEXT, INTEGER]`                                   | repeat the string `n` times; PG `repeat($1,$2)`                                                                                        |
| `startsWith[…]` / `istartsWith[…]` / `endsWith[…]`        | check the start / end of a string; the `i`-form is case-insensitive; PG `$1 LIKE $2\|\|'%'` / `$1 LIKE '%'\|\|$2` (the `i`-forms use `ILIKE`) |
| `isSubstring[…]` / `isISubstring[…]`                      | substring containment, case-sensitive / case-insensitive; PG `$1 LIKE '%'\|\|$2\|\|'%'` (`isISubstring` uses `ILIKE`)                    |
| `isWordInCSV[…]`                                          | splits the second argument by comma and checks the first argument for an exact match against one of the elements; PG `string_to_array($2,',')` + `= ANY` |
| `getWord[TEXT, TEXT, INTEGER]` / `wordCount[TEXT, TEXT]`  | `getWord` — the fragment numbered `p3` (1-based) after splitting by the separator, `NULL` if out of range; `wordCount` — the number of fragments; PG `(string_to_array($1,$2))[$3]` / `array_length(string_to_array($1,$2),1)` |
| `splitPart[STRING, STRING, INTEGER]`                      | the `num`-th fragment (1-based) after splitting by the separator; an empty string if out of range; PG `split_part($1,$2,$3)`            |
| `regexpReplace[STRING, STRING, STRING, STRING]`           | replace, in `source`, matches of the POSIX `pattern` with `replace`; the fourth argument is flags (e.g. `g` — replace all occurrences, not only the first); PG `regexp_replace($1,$2,$3,$4)` |
| `regexPatternMatch[TEXT, STRING]`                         | match of a string against a POSIX regular expression, case-sensitive; PG — the `~` operator                                            |
| `onlyDigits[TEXT]`                                        | flag that the string (with surrounding whitespace trimmed) consists only of digits; PG `trim($1) ~ '^[0-9]*$'`                          |
| `array[STRING, STRING, INTEGER]`                          | table-valued wrapper: unnests a delimited string into a row-keyed set of values; the `row` parameter is the iteration variable (as in `array[JSON, INTEGER]`, see below); PG `unnest(string_to_array($1,$2))` |

The predicate properties (`startsWith`, `endsWith`, `isSubstring`, `isWordInCSV`, `regexPatternMatch`, `onlyDigits`, and the like) are declared as `FORMULA NULL BOOLEAN` and return `TRUE` when the condition holds and `NULL` otherwise (not `FALSE`) — following the lsFusion convention for conditions.

For identifiers and the clipboard: `generateUUID[]` → `generatedUUID[]`, `generatePassword[INTEGER, BOOLEAN, BOOLEAN]` → `generatedPassword[]`, `copyToClipboard[TEXT]`; for cookies: `getCookie[STRING]` → `cookie[]`, `setCookie[STRING, STRING, JSON]`.

### Numbers

Some of these properties are wrappers around PostgreSQL numeric functions, others are pure arithmetic formulas that call no DBMS function. Where a property maps to a PostgreSQL expression, it is given at the end of the description after `; PG`.

| Property                                                  | What it does                                                                                                                          |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `abs[…]` / `delta[…, …]`                                  | absolute value / absolute difference of two values; PG `abs($1)` / `abs($1-$2)`                                                        |
| `min[…, …]` / `max[…, …]`                                 | minimum / maximum of two values; computed arithmetically, with no DBMS function call; PG `($1+$2-abs($1-$2))/2` / `($1+$2+abs($1-$2))/2` |
| `floor[…]` / `floor[…, …]`                                | round down; the two-argument form rounds down to a multiple of the step `$2`; PG `floor($1)` / `floor($1/$2)*$2`                        |
| `ceil[…]` / `ceil[…, …]`                                  | round up; the two-argument form rounds up to a multiple of the step `$2`; PG `ceiling($1)` / `ceiling($1/$2)*$2`                        |
| `round[…, …]`, `roundM1` / `round0` … `round6`            | round a value cast to `numeric` to `n` decimal places; `n` may be negative (`roundM1` = `round(…,-1)`), `round0`…`round6` fix `n` from 0 to 6; PG `round(CAST($1 AS numeric),$2)` |
| `trunc[…, …]`                                             | truncate to `n` decimal places without rounding; PG `trunc($1,$2)`                                                                     |
| `mod[…, …]` / `divideInteger[…, …]` / `divideIntegerNeg` / `divideIntegerRnd` | remainder of division; `divideInteger` — integer division, `divideIntegerNeg` — integer division rounding toward zero for negatives, `divideIntegerRnd` — division with the result rounded to an integer; PG `mod($1,$2)` (the rest — casts to `integer` and `round`) |
| `sqr[…]` / `sqrt[…]` / `power[…, …]`                      | square (with no DBMS function), square root, power (the result is `DOUBLE`); PG `$1*$1` / `sqrt($1)` / `power($1,$2)`                    |
| `ln[…]` / `exp[…]`                                        | natural logarithm (`DOUBLE`) / exponential; PG `ln($1)` / `exp($1)`                                                                     |
| `percent[…, …]` / `share[…, …]`                           | percentage of a sum / share of the whole as a percentage; computed arithmetically, with no DBMS function call; PG `$1*$2/100` / `$1*100/$2` |
| `bitwiseAnd[…, …]` / `bitwiseOr[…, …]` / `bitwiseNot[…]`  | bitwise AND / OR / NOT on integers; PG `$1 & $2` / `$1 \| $2` / `~ $1`                                                                  |
| `toInteger[…]` / `toNumeric[…]` / `toNumericNull[…]`      | parse a value into `INTEGER` / `NUMERIC[38,19]`; for an unparseable value `toInteger` and `toNumeric` return `0`, while `toNumericNull` returns `NULL`; PG `convert_to_integer($1)` / `convert_to_numeric($1)` / `convert_to_numeric_null($1)` — functions created by the platform in the DB |
| `toChar[…, …]`                                            | format a number or date into a string by a PostgreSQL template; PG `to_char($1,$2)`                                                    |

### Iteration

`iterate[INTEGER, INTEGER, INTEGER]` — recursive enumeration of an integer range from `from` to `to`. `count[INTEGER, INTEGER]` is a special case: `iterate(i, 1, count)`. See the analogous `iterate` for dates in [`Time`](System_Time.md).

### Color

`colorToHexString[COLOR]` — color representation as `'#RRGGBB'`: the color is cast to an integer and converted to a hex string by `to_hex`, then the first two hex digits (the high byte — the alpha component) are dropped and a `#` prefix is added; PG `'#'||substring(to_hex($1::integer),3)`.

### Full-text search

Wrappers around PostgreSQL full-text search functions. Available on PostgreSQL only.

| Property                                                  | What it does                                                                                                                          |
|-----------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `toTsVector[STRING, STRING]` / `toTsVector[STRING]`       | build a `TSVECTOR` from text using a dictionary: the text is split into normalized lexemes (the first argument is cast to `regconfig`); the single-argument form uses the `english` dictionary; a `NULL` text is replaced with an empty string; PG `to_tsvector($1::regconfig, coalesce($2,''))` |
| `toTsQuery[STRING, STRING]` / `toTsQuery[STRING]`         | build a `TSQUERY`: the query text is parsed into a tree of lexemes and operators using the dictionary; the single-argument form uses `english`; PG `to_tsquery($1::regconfig, coalesce($2,''))` |
| `setWeight[TSVECTOR, STRING]`                             | tag all lexemes of a vector with the weight label `'A'`–`'D'` (the second argument is cast to the `"char"` type); PG `setweight($1, $2::"char")` |
| `tsRank[TSVECTOR, TSQUERY]` / `tsRank[…, INTEGER]`        | relevance ranking of a vector against a query by the frequency of matched lexemes (`DOUBLE`); the third argument is the length-normalization bitmask; PG `ts_rank($1,$2)` / `ts_rank($1,$2,$3)` |
| `tsRankCD[TSVECTOR, TSQUERY]` / `tsRankCD[…, INTEGER]`    | the same with the cover-density model — by the density of matches, accounting for lexeme proximity; the third argument is the normalization mask; PG `ts_rank_cd($1,$2)` / `ts_rank_cd($1,$2,$3)` |
| `tsRankLN[TSVECTOR, TSQUERY, DOUBLE]`                     | `tsRank` with a logarithmic correction for vector length: `tsRank(v,q) / (1 + ln(length of v) / ln(base))`; a composite property, not a standalone DBMS function |
| `numNode[TSQUERY]`                                        | number of nodes (lexemes and operators) in a parsed query; PG `numnode($1)`                                                            |

### JSON access properties {#json-access}

This section collects the wrappers around PostgreSQL's `jsonb_*` functions that read values out of an existing JSON. They complement the `JSON` operator, which goes the other way and builds JSON from properties or from a form. Most are declared with the `FORMULA` operator.

| Property                                          | What it returns                                                                                                                       |
|---------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| `field[JSON, STRING]`                             | value at the given key, as `JSON`; PG `jsonb_extract_path($1,$2)`                                                                       |
| `field[JSON, STRING, STRING]`                     | value at the given two-key path, as `JSON`; PG `jsonb_extract_path($1,$2,$3)`                                                           |
| `field[JSON, STRING, STRING, STRING]`             | value at the given three-key path, as `JSON`; PG `jsonb_extract_path($1,$2,$3,$4)`                                                      |
| `fieldText[JSON, STRING]` / `[…, STRING, STRING]` / `[…, STRING, STRING, STRING]` | same as `field` but as `STRING`; PG `jsonb_extract_path_text(…)`                                     |
| `array[JSON, INTEGER]`                            | array element at the given index, as `JSON`; the index parameter is also the iteration variable; PG `jsonb_array_elements($1)` — expands a JSON array into a set of rows |
| `arrayText[JSON, INTEGER]`                        | same as `array` but as `STRING`; PG `jsonb_array_elements_text($1)`                                                                     |
| `arrayElement[JSON, INTEGER]`                     | array element at the given 1-based index as a raw string (jsonb → text); PG `($1)->($2-1)` (the `->` operator)                          |
| `map[JSON, STRING]`                               | value for each key-value pair of a JSON object, as `JSON`; the key parameter is also the iteration variable; PG `jsonb_each($1)` — expands a JSON object into a set of key-value pairs |
| `mapText[JSON, STRING]`                           | same as `map` but as `STRING`; PG `jsonb_each_text($1)`                                                                                 |
| `merge[JSON, JSON]`                               | recursive merge of two JSON values: objects are merged by key recursively, and on a conflict of non-object values the second argument wins; PG `jsonb_recursive_merge($1,$2)` — platform function |
| `canonicalizeJSON[JSONFILE, BOOLEAN]` / `canonicalizeJSON[JSONFILE]` → `canonicalizedJSON[]` | canonical normalization of a JSON file; the short form is equivalent to `canonicalizeJSON(file, TRUE)` (encode unicode characters); an `INTERNAL` action, not a DBMS function |

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
