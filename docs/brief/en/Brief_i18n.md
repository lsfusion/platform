---
slug: "/Brief_i18n"
title: 'Brief: internationalization'
---

## Localizing strings and captions

Localization is reached for when one logic runs in several languages. A user-visible string — the caption of a class, a property, an action, a form, the text of a message — is localized by a string data identifier in curly braces inside a [string literal](../language/String_literal.md#localization). When the value is sent to the client, the platform looks each identifier up in the project files whose name ends with `ResourceBundle`, in the required locale, and substitutes the translation found; if there is no translation, the identifier itself is left without the braces.

```lsf
CLASS Book '{use.case.i18n.book}';
name '{use.case.i18n.book.name}' = DATA STRING[40] (Book);
```

The current locale — language, country, timezone — is taken from the `Authentication.language[CustomUser]` property and the like, and for actions started by the system, from the server locale; user data is not translated. The mechanism is described in [internationalization](../paradigm/Internationalization.md).

**Analogy**: ResourceBundle keys written straight into the caption text instead of a call to a translation function.

## Reverse translation

Reverse translation removes the need to place identifiers by hand: captions are written in the code as plain text in one language, and it is the platform that finds the entry whose value is that text — the entry keeps its own identifier as the key, and the text is its value. It is turned on by the `logics.lsfStrLiteralsLanguage` [launch parameter](../paradigm/Launch_parameters.md), which sets the language of the string literals in lsf code: at server start a `value -> identifier` dictionary is built from all ResourceBundle files of the project in that locale, and a plain literal matching an entry value is replaced at code parse time with that entry's identifier and then behaves as a localizable one. Leading and trailing spaces are excluded from the match and kept around the substitution, and a literal that is empty or all spaces is not replaced at all.

The replacement applies to any plain literal, not only to captions, so technical strings — JSON keys, addresses, formats, external identifiers — are written in the raw form of a [string literal](../language/String_literal.md), which takes part neither in localization nor in reverse translation:

```
'content'    // plain: localized, and replaced by reverse translation on a match
r'rawContent'  // raw: neither
```
