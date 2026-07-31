---
slug: "/String_literal"
title: 'String literal'
---

*A string literal* denotes a value of a string [built-in class](../paradigm/Built-in_classes.md) (`STRING[N]` or `TEXT`) — in the simple case, a [constant](../paradigm/Constant.md).

## Syntax

```
'content'
r'rawContent'
```

Here `content` is a sequence of ordinary characters, escape sequences, interpolation blocks `${...}`, and inline sequences (`$I{...}`, `$R{...}`, `$M{...}`), and `rawContent` is a sequence of characters taken as is. The `r` prefix can also be written in uppercase (`R`).

## Content and escaping

A plain literal `'content'` is a sequence of characters between single quotes. Special characters are specified with escape sequences:

| Sequence | Meaning |
|---|---|
| `\n` | line feed |
| `\r` | carriage return |
| `\t` | tab |
| `\'` | single quote |
| `\\` | backslash |
| `\$` | the `$` character |

The `$` character is special only before `{`, `I{`, `R{`, or `M{` (the start of interpolation or an inline sequence); otherwise it is an ordinary character. To get a plain `$` even before `{`, it must be escaped: `\$`.

Without escaping, `{` and `}` delimit a [localization](#localization) identifier, so `\{` and `\}` are used to insert the braces themselves. In strings where localization is not supported (technical arguments — formulas, charsets, key combinations, etc.), curly braces are ordinary characters, and escaping them is not allowed.

Any other escape sequence (`\` with another character), as well as a lone `\` at the end of the literal, is considered an error.

### Examples

```lsf
line (Order o) = 'Order #' + number(o) + '\tof ' + date(o);
tpl = 'Total: \$\{sum\}';                    // the text "Total: ${sum}" without interpolation or localization
```

## Multi-line

A literal can span several physical lines — real line breaks inside the quotes are kept in the value (together with the indentation of continuation lines). The `\r\n` sequence is normalized to `\n`.

### Examples

```lsf
note = 'first line
second line';                                // same as 'first line\nsecond line'

query = 'SELECT *
         FROM orders';   // the indentation becomes part of the value: 'SELECT *\n         FROM orders'
```

## Localization {#localization}

Inside a string literal, string-data identifiers in curly braces (for example, `'{form.title}'`) are replaced, when the value is sent to the client, with the translation corresponding to the [current locale](../paradigm/Internationalization.md#current). If there is no translation for the identifier in any ResourceBundle, the identifier itself (without the curly braces) is substituted in its place.

If the server is started with the `logics.lsfStrLiteralsLanguage` [parameter](../paradigm/Launch_parameters.md) (reverse translation of literals), which specifies the language of the string literals in the code, a plain (non-localizable) literal can automatically become localizable. If its value (excluding leading and trailing spaces) matches the value of a ResourceBundle entry in that language, at code parse time the literal is replaced with that entry's identifier and then behaves like the localizable literal `'{id}'`. Leading and trailing spaces are preserved, and an empty literal or one consisting only of spaces is not replaced.

Curly braces that should be just part of the string must be escaped: `\{` and `\}`.

### Examples

```lsf
formTitle = '{orders.title}';                // one identifier — the translation is substituted
header = '{period.from} — {period.to}';      // several identifiers in one literal
json = '\{"status": {order.status}\}';       // \{ \} are part of the string, {order.status} is an identifier
```

## Interpolation

Interpolation lets you insert the values of expressions directly into a string literal instead of assembling the string by hand with `+`. A `${expr}` block substitutes the value of an [expression](Expression.md), converting it to a string: the literal becomes a concatenation (`'a${x}b'` is equivalent to `'a' + STRING(x) + 'b'`). A literal may contain several such blocks, and the text between them follows the usual literal rules (escaping, localization).

Since the result is a concatenation, a literal with interpolation is an expression, not a constant, and is allowed only where an expression is allowed. The expression can use all parameters of the surrounding construction.

Inside `${...}` you can use nested string literals — their quotes are written escaped (`\'`), and this escaping is removed during parsing.

### Examples

```lsf
greeting (Customer c) = 'Hello, ${name(c)}!';
likePattern (STRING s) = '%${s}%';           // for example, for the LIKE operator
status (Order o) = 'Status: ${IF paid(o) THEN \'paid\' ELSE \'no\'}';   // nested literal: quotes are escaped
```

## Inline sequences

Inline sequences insert external content into the string. Like interpolation, they start with the `$` character:

- `$I{resourceName}` — includes the content of a **text** file `resourceName` in the string literal. The file is read at code parse time, and its content becomes part of the literal. Interpolation blocks `${...}` in it are processed as ordinary interpolation with the parameters of the current construction. Because of this, the literal sequence `${` cannot be used in the file (for example, in JavaScript or CSS). All other characters, such as quotes, backslashes, and curly braces, do not need to be escaped in the file — the platform does it automatically. Can be used to keep large parameterizable templates (HTML emails, pages, fragments, etc.) in separate files.
- `$R{resourceName}` — embeds the content of the file `resourceName` into the string not at code parse time, but at the moment the **value is sent to the client**. The file may be **arbitrary**, including binary (an image, a PDF), and its content is not processed as code. Can be used inside HTML.
- `$M{imageName}` — embeds an image by the name `imageName`. Unlike `$R{}` (a file by path), this name is interpreted the same way as the names of [property and action icons](../paradigm/Icons.md#manual).

### Examples

```lsf
letterBody (Order o) = HTML('$I{orders/letterBody.html}');
```

The file `orders/letterBody.html`:

```html
<h3>Order #${number(o)}</h3>
<p>Customer: ${nameCustomer(o)}</p>
```

If the order's number is `100` and the customer name is `Smith`, the value of `letterBody` will be:

```html
<h3>Order #100</h3>
<p>Customer: Smith</p>
```

`$R` embeds the file itself into the string (usually HTML):

```lsf
banner () = HTML('<img src="$R{images/banner.png}">');   // the file images/banner.png is embedded into HTML
```

## Raw literal

A literal with the `r` (or `R`) prefix records its content as is: escape sequences are not interpreted, the backslash and curly braces are ordinary characters, and localization and reverse translation are not performed (only `\r\n` is normalized to `\n`). The value class of such a literal is `STRING[N]`. Using it is convenient for strings with many backslashes or curly braces, where constant escaping would hurt readability: Windows paths, regular expressions, formats, JSON fragments.

The `r'...'` form does not allow a single quote inside. To use a quote inside, the delimiter-character form is used: the same character is placed between `r` and the opening quote and right after the closing quote (for example, `r!'...'!`). Such a literal ends at a quote immediately followed by this delimiter character. So the delimiter must be chosen so that the sequence of a single quote and this character does not occur inside the string, otherwise the literal would end there prematurely. The delimiter can be any character except Latin letters, digits, the underscore, a space, a line feed, a tab, a single quote, and the characters `+` `*` `,` `=` `<` `>` `(` `)` `[` `]` `{` `}` `#`.

### Examples

```lsf
filePath = r'C:\reports\sales.xlsx';         // backslashes are ordinary characters
quoted = r!'don't stop'!;                    // a quote inside — the delimiter form
```

## Value class

The class of a literal-constant is `STRING[N]`, where `N` is the number of characters. If the literal uses localization, its class is `TEXT` (see [built-in classes](../paradigm/Built-in_classes.md)). For example, `'abc'` has class `STRING[3]`, and `'{orders.title}'` has class `TEXT`. A literal with interpolation or an inline sequence (`${...}`, `$I{...}`, `$R{...}`, `$M{...}`) is an expression, so the class of its value is a string class (`STRING[N]` or `TEXT`) derived from its parts (the constant fragments and the inserted values).