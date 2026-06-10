---
slug: "/System_Icon"
title: 'Icon'
---

`Icon` is a [system module](System_modules.md) that stores the catalogue of UI icons (Font Awesome and Bootstrap Icons) and finds the best icon for a text query through full-text search. It is pulled in via `REQUIRE Icon` (which in turn does `REQUIRE Utils, Reflection, SystemEvents`).

The full-text search and ranking properties build on the `toTsVector` / `toTsQuery` / `tsRank` primitives provided by [`Utils`](System_Utils.md) (see its "Full-text search" section).

### Icon catalogue

Every icon is an object of the `Icon` class. Its catalogue fields:

| Property                  | What it holds                                                                                  |
|---------------------------|------------------------------------------------------------------------------------------------|
| `name[Icon]`              | the icon name (the bare name used in the CSS class, such as `user`, `home`)                    |
| `explicit[Icon]`          | extra search words assembled from the `*Icons.properties` resources (highest-weight match text) |
| `label[Icon]`             | the human-readable label                                                                       |
| `terms[Icon]`             | space-separated search terms                                                                   |
| `synonyms[Icon]`          | space-separated synonyms                                                                       |
| `styles[Icon]`            | the available style classes for the icon (the Font Awesome free-style set, or `bi` for Bootstrap Icons) |
| `type[Icon]`              | the icon family, derived from `styles[Icon]`: `'bi'` when the style is `bi`, otherwise `'fa'`  |

`Icon.null` is the predefined "no icon" object (selected when no icon should be shown); its `name[Icon]` and `explicit[Icon]` are both set to `'null'` on startup.

An icon is looked up by name and family through `icon[name, type]`, an aggregation over the `(name[Icon], type[Icon])` pair, which is indexed to make the lookup direct.

`iconClass[Icon, STRING]` builds the CSS class string for the icon in a requested style:

| Family (`type[Icon]`) | Resulting class                                                                      |
|-----------------------|--------------------------------------------------------------------------------------|
| `'bi'`                | `'bi bi-' + name(i)`                                                                  |
| `'fa'`, `brands` style | `'fa-brands fa-' + name(i)`                                                          |
| `'fa'`, other styles   | `'fa-' + style + ' fa-' + name(i)` when the icon offers the requested `style`, otherwise `'fa fa-' + name(i)` |

### Full-text search and ranking

For each icon the module materializes one ts-vector per catalogue field — `searchExplicit[Icon]`, `searchLabel[Icon]`, `searchTerms[Icon]`, `searchSynonyms[Icon]` — and one combined ts-vector `search[Icon]` over the concatenation of `explicit` / `label` / `terms` / `synonyms`, used as the `MATCH` index for candidate selection. `searchStyles[Icon]` materializes the icon's default style word (`'regular'` for Bootstrap Icons, empty for the `brands` style, otherwise the icon's `styles`), used to penalize a style mismatch.

A query string is turned into a `TSQUERY` by `nameToIconQuery[STRING]`: it splits CamelCase, dashes, and spaces into separate words, lower-cases them, and joins them with the `OR` operator, so any of the words may match. `lengthIconQuery[TSQUERY]` gives the number of words in such a query (`(numNode(query) + 1) / 2`).

`tsRank[Icon, TSQUERY, STRING]` is the combined relevance score for one icon against a query and a requested style. It is a weighted sum of the per-field ranks, scaled by the query word count, with a style-mismatch penalty:

| Field        | Weight | Ranking model                                          |
|--------------|--------|---------------------------------------------------------|
| `explicit`   | 12     | `tsRank` (extra explicit words do not lower the rank)  |
| `label`      | 8      | `tsRankLN` with normalization base 4 (shorter labels rank higher) |
| `terms`      | 6      | `tsRank` (extra terms do not lower the rank)           |
| `synonyms`   | 4      | `tsRankLN` with normalization base 16 (a long synonym list is damped to avoid false positives) |

The weighted sum is multiplied by `lengthIconQuery[TSQUERY]`, so matching more query words raises rather than dilutes the score. Finally, when the icon does not offer the requested style (`searchStyles[Icon]` does not contain the style) the score is reduced by `0.25`.

The per-field helpers `explicitRank[Icon, TSQUERY]`, `labelRank[Icon, TSQUERY]`, `termsRank[Icon, TSQUERY]`, `synonymsRank[Icon, TSQUERY]` expose each field's contribution (the same model and weight, scaled by the query word count) for inspection.

### Best-icon search

The best-icon search reads one or more query lines and writes back, for each, the chosen icon's CSS class and score:

| Property                  | Direction | What it holds                                                        |
|---------------------------|-----------|----------------------------------------------------------------------|
| `bestIconNames[STRING]`   | input     | flag marking a query line to process; the line is `query,style`     |
| `bestIconClasses[STRING]` | output    | the `iconClass` of the best-matching icon for the line              |
| `bestIconRanks[STRING]`   | output    | the `tsRank` score of that icon                                     |

`getBestIcons[]` processes every flagged line. It takes the part before the comma as the query text and the part after it as the style, builds the `TSQUERY` for the query, and over the icons whose `search[Icon]` matches it picks the one with the highest `tsRank[Icon, TSQUERY, STRING]`; it then writes that icon's class and score into `bestIconClasses[STRING]` and `bestIconRanks[STRING]`.

The `icons` form drives this from the UI: `searchAndStyle[]` joins the entered search text and style as `search() + ',' + style()`, and on any change to either it flags that combined line and calls `getBestIcons[]`.

### Icon import

`importIcons[]` loads the catalogue and keeps the `Icon` objects in sync with it:

- It reads `/web/icons_with_synonyms.json` and, guarded by `importIconsHash[]` (the MD5 of the resource), re-imports only when the file has changed. On change it creates a new `Icon` for every `(name, type)` not yet present, refreshes `label` / `terms` / `synonyms` / `styles` from the file (lower-cased), deletes icons no longer present in it (keeping `Icon.null`), and stores the new hash.
- It then reads every `*Icons.properties` resource and fills `explicit[Icon]` for each icon from the matching property keys, split into separate words.

### Display properties

These properties build small pieces of HTML for composing an image (or a badge) with text:

| Property                                    | What it produces                                                                 |
|---------------------------------------------|-----------------------------------------------------------------------------------|
| `badge[STRING]`                             | the HTML for a badge showing the given text                                       |
| `imaged[HTML, STRING, …]`                   | HTML composing an image and text; the flags control vertical layout, leading position, word wrap, and collapsing, with shorter overloads supplying defaults |
| `badged[STRING, STRING, …]`                 | the same composition with a badge in place of the image; the integer overload renders a numeric badge |
| `imagedCaption[StaticObject]`               | composes the object's `image` and `caption` into one image-plus-text fragment      |

### Forms and navigator

The `icons` form is the icon search and preview screen: it shows the search box, the chosen style, the best-matching icon with its class and score, and the full grid of icons with their fields and per-icon rank. It is placed in the navigator under the `system` folder (the `icons` entry). `isIconModuleAvailable[]` is set to `TRUE` to signal that the module is present.

### Language

- [`MATCH` operator](../language/MATCH_operator.md) — the full-text match used to select candidate icons against the query.
- [`FORMULA` operator](../language/FORMULA_operator.md) — the syntax behind the ranking and string-splitting properties.

### See also

- [`System modules`](System_modules.md) — the general list of platform modules.
- [`Utils`](System_Utils.md) — its "Full-text search" section documents the `toTsVector` / `toTsQuery` / `tsRank` primitives this module is built on.
- [`Reflection`](System_Reflection.md) — the metadata module pulled in with `Icon`.
- [`SystemEvents`](System_SystemEvents.md) — the server-lifecycle module pulled in with `Icon`.
