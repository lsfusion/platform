---
slug: "/Utils_Numerator"
title: 'Numerator'
---

The `Numerator` module generates sequential, human-readable numbers and codes for objects — order numbers, invoice numbers, partner codes, and similar. Each kind of object keeps its own counter, the generated value carries an optional series prefix and a fixed number of leading zeros, and the generation survives several users creating objects at the same time without producing duplicate or skipped numbers.

### Numerator object

A *numerator* is a settings object of the `Numerator` class — one counter that several objects share. Its settings:

| Property | Meaning |
|---|---|
| `name[Numerator]` | Name shown when a numerator is picked |
| `series[Numerator]` | Series prefix prepended to every generated value (may be empty) |
| `minValue[Numerator]` | Starting counter value; `curValue[Numerator]` is set to it when the numerator is created |
| `maxValue[Numerator]` | Largest allowed value; once `curValue[Numerator]` reaches it, generation stops and a message is shown |
| `stringLength[Numerator]` | Width the counter is padded to with leading zeros |
| `curValue[Numerator]` | Current counter value — the number that will be issued next |

The next value is formed as `series[Numerator]` followed by `curStringValue[Numerator]`, where `curStringValue[Numerator]` is `curValue[Numerator]` written as text and padded on the left with `0` to `stringLength[Numerator]` characters. For a numerator with series `WKO`, current value `42`, and length `6`, the issued value is `WKO000042`.

### Producing the next value

Two actions issue the current value and advance the counter; they differ in how they hold the counter against concurrent saves.

`incrementValueSession[Numerator]` advances the counter inside the current session, so issuing the number and saving the object happen in one transaction. When two users save at the same time, both transactions update `curValue[Numerator]` of the same row, so the second one gets a `CONFLICT UPDATE`; the platform rolls it back and replays it automatically, with no user action. The user only sees a slightly slower save, and the numbering stays gapless.

`incrementValue[Numerator]` advances the counter in a separate short transaction of its own and exposes the issued value through `incrementedValue[]`. This keeps the lock on the counter row out of the object's main save transaction, which shrinks the window for a conflict; in exchange the number is consumed even when the object is not ultimately saved, so gaps in the numbering become possible.

Both actions show a message and issue nothing once `curValue[Numerator]` has reached `maxValue[Numerator]`.

### Numbering a class

A set of metacodes attaches numbering to an application class. The number-carrying properties go into the system `numbered` group.

| Metacode | What it adds to the class |
|---|---|
| `@defineNumbered(class, stype)` | Stored `number` (text) and `series` (of type `stype`) properties, and `seriesNumber` — their concatenation, materialized and indexed as the combined searchable key |
| `@defineNumerated(class)` | A `numerator` reference on the class and an event that fills `number` and `series` from it (via `incrementValueSession[Numerator]`) as soon as the numerator is set and the number was not entered manually or by import |
| `@defineNumeratedDefault(class, caption, series)` | Everything `@defineNumerated` adds, plus a default numerator for the class, chosen on the `defaultNumerators` form and assigned to every new object automatically; an initial numerator (`caption`, `series`, range `1..99999`, length `5`) is seeded through `loadDefaultNumerators` |
| `@defineNumeratedID(object, caption)` | A default numerator whose issued value is written into the object's own `id` instead of a separate `number`, for objects whose code is their identifier |

`@defineNumbered` also normalizes the entered number and series, governed by three options on the `options` form: `useLoweredNumber[]` lowercases the number, `useUpperedSeries[]` uppercases the series, and spaces are stripped from the number unless `keepNumberSpaces[]` is set.

When the order of saving must not depend on the counter — to reduce conflicts on a heavily used numerator — `@addEventGenerateNumberOnForm(form, object, class)` adds an event that runs before the form's save and, while the `generateNumberOnForm[]` option is on, issues the number through `incrementValue[Numerator]` (the separate-transaction variant above) rather than during the save itself.

### Default numerators

`loadDefaultNumerators` is an abstract list action that the seeding metacodes extend to create the initial numerators on a fresh database; it is wired into the platform's initial-data mechanism. The `numerators` form manages the catalogue of numerators, and `defaultNumerators` — added to the `Master data` navigator group — collects the default-numerator choice of every class set up through `@defineNumeratedDefault` or `@defineNumeratedID`.
