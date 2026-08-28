---
slug: "/Rules_i18n"
title: 'Rules: internationalization'
---

## Internationalization and reverse translation rules

1. The assistant MUST use `*ResourceBundle.properties` files
   for UI localization.

   The value inside `{...}` MUST be treated
   as the lookup key that lsFusion resolves
   according to the current locale.

2. The assistant MUST first determine
   whether reverse translation is used
   in the current project area.

   If it is used,
   the assistant MUST continue using it
   in that area
   and MUST follow the existing project policy.

   The assistant MUST keep id selection
   consistent with the established pattern
   already used there.

   The assistant MUST NOT introduce
   a new explicit id policy
   unless the user requests it.

3. Reverse translation means
   translating in the opposite direction
   of normal UI localization:
   not `key -> localized text`,
   but `localized text -> key`,
   and then, if needed, to another locale.

   If ids are not specified explicitly in code,
   this canonical value is the source-language text itself.
   It is what the platform LOOKS UP, not what it stores as
   the key: the entry stays `id = source text`, and the
   dictionary built for the lookup is the reversed one,
   `value -> id`. An assistant writing the bundle the other
   way round produces entries reverse translation never
   matches.

4. Reverse translation is turned on by the launch parameter
   that sets the language of lsf string literals
   (`logics.lsfStrLiteralsLanguage`).
   When it is active, ANY plain `'...'` literal
   in a localizable position —
   including a constant literal in any expression —
   that matches a ResourceBundle entry value
   is silently replaced at code parse time with its key `{id}`
   and is substituted in the current locale at runtime:
   `'position'` can become `'pozycja'`.
   Leading and trailing spaces take no part in the match and
   are kept around the substitution; a literal that is empty
   or made of spaces alone is never replaced.

   Therefore the assistant MUST write technical literals —
   JSON keys, URLs, formats, canonical names,
   external identifiers —
   as raw literals `r'...'`,
   which take part neither in localization
   nor in reverse translation.
   Plain `'...'` literals are meant
   for user-visible text.
