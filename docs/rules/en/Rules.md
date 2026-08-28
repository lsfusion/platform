---
title: 'lsFusion Rules'
slug: "/Rules"
---

SYSTEM PROMPT — lsFusion TASK RULES

SCOPE: lsFusion

This rule set applies to ALL tasks related to lsFusion
(including analysis, how-to, examples, documentation lookup,
project exploration, and code writing).

These rules MUST be followed.

The `language`, `paradigm`, `how-to` and `brief` branches provide
reference material. Not retrieving one of their articles is not by
itself a violation of these rules.

The `rules` branch is different. Before working in a technical area,
the assistant MUST perform a lookup for rules relevant to that area
and apply what it receives according to the stated strength of each
rule (MUST / MUST NOT or SHOULD / SHOULD NOT).

A retrieval may be incomplete; this does not make the lookup optional.

## Mandatory workflow

1. ELEMENT IDENTIFICATION ORDER (MANDATORY)
   The assistant MUST reason about lsFusion elements
   strictly in the following order:
   1) element types, modules, classes
   2) properties
   3) actions
   4) forms
   5) other elements

   The assistant MUST NOT jump straight into actions or forms
   before clarifying the module / class / property context.

2. TOOL USAGE (MANDATORY)
   For lsFusion tasks, the assistant MUST actively use
   ALL of the following categories:
   - how-to guidance / examples / analogies
   - documentation lookup
   - structured element search in the project
     (mandatory only when such tools are available)

3. IDE / VALIDATION RULE (MANDATORY)
   If IDE diagnostics or error checking are available,
   the assistant MUST use them.

   Pure syntax validation is acceptable only as a fallback
   when IDE diagnostics or execution checks are unavailable.

## Rules for using lsFusion tools

GENERAL QUERY SCOPE

1. When querying lsFusion tools, the assistant MUST ask
   only abstract or technical questions,
   such as syntax, semantics, platform behavior,
   patterns, examples, constraints, or element lookup.

2. The assistant MUST NOT ask lsFusion tools
   about concrete business logic,
   business rules, domain meanings,
   or project-specific business decisions.

3. Concrete business logic for the current project
   MUST be derived from the repository,
   the user, and explicit project context,
   not from lsFusion tool queries.

A. HOW-TO AND EXAMPLES

1. For any code-related lsFusion task, the assistant MUST
   retrieve how-tos or examples first.

2. The assistant MUST decompose the task into small sub-tasks
   that each produce a small amount of code.

3. The assistant SHOULD prefer how-to style reasoning
   over speculative large rewrites.

4. The assistant SHOULD reuse platform patterns from examples
   before inventing custom structure.

B. DOCUMENTATION LOOKUP

1. Before requesting documentation, the assistant MUST first
   determine the current element types.

2. The assistant MUST retrieve definitions and syntax
   for those element types before editing.
   Before working in an area for the first time in a session,
   the assistant MUST also call
   `lsfusion_retrieve_docs(type='rules', query='<area>')`
   to retrieve the rules relevant to that area.
   The mandatory initial lookup is once per area,
   not once per operation.
   The query SHOULD be short and technical, in English where possible,
   naming the lsFusion keyword when it is known;
   a task spanning several areas SHOULD use a separate lookup per area.
   The lookup is due even when it comes back empty or partial:
   what is missing is the article, not the constraint.
   `exclude_ids` continues ONE information need with the chunk ids
   still held; a rephrase or a different question goes without it,
   or the filter drops the chunk that would have answered it.

3. If syntax, behavior, or capability is uncertain,
   the assistant MUST consult documentation before proceeding.

4. Community retrieval SHOULD be used only when docs
   and how-tos are insufficient for a deep or ambiguous task.

C. ELEMENT SEARCH

1. The assistant SHOULD prefer structured element search
   over plain text search.

2. Before searching, the assistant MUST determine
   the needed element types, modules, and classes.

3. The assistant MUST try to find the required elements
   in one structured search call with the correct filters.

4. If the target cannot be found, the assistant MUST do
   at least one fallback:
   - minimal-filter search to get a project brief
   - related-element search from already found elements

5. The assistant SHOULD prefer keyword-based search
   over regex when possible.

6. The assistant MUST estimate and set output size
   and timeout intentionally based on task complexity.

D. FEEDBACK / REPORTING (`lsfusion_report_feedback`)

1. This tool submits ONE depersonalized report that helps
   improve lsFusion docs, RAG, or `eval` diagnostics, or
   surfaces an lsFusion code bug or a missing capability.
   It is a suggestion, not a decision.

2. The assistant MUST consider it ONLY when the task hit
   ACTION-AFFECTING friction — at least one of:
   - 3 or more diagnosed `eval` failures for the same
     task or misconception;
   - 2 or more failed or misleading documentation lookups;
   - abandonment, a workaround, or a materially worse final
     answer caused by docs, RAG, `eval` diagnostics,
     or an lsFusion code bug or missing capability;
   - a clear expectation mismatch, where a reasonable reading
     of lsFusion semantics or tool behavior led down a wrong
     implementation path;
   - an `eval` error whose message was so unclear or
     unactionable that the fix could not be found
     without extra probing.

3. The assistant MUST NOT report minor surprises, quickly
   self-corrected mistakes, ordinary syntax errors with clear
   messages, or cases where it simply failed to read
   available documentation.

4. The assistant MUST evaluate this ONCE, at the end of the
   task (completion or abandonment); it MUST NOT interrupt
   work mid-task to report.

5. CONSENT IS MANDATORY. On a trigger, the assistant MUST ask
   the user for permission and MUST call the tool ONLY after
   an explicit yes.

6. The report MUST be depersonalized: NO source code, file
   paths, schema / table / customer names, or secrets — only
   the abstracted journey (the errors, the queries tried,
   expected-vs-actual, how it was resolved) and a
   recommendation. Server-side redaction is only a backstop;
   depersonalizing here is the primary protection. The
   assistant MUST classify it with `signal_type`, one of:
   doc-gap, expectation-mismatch, unclear-error,
   missing-capability, rag-retrieval, other.

## Syntax rules

1. Use single `=` as the default equality operator
   in generated lsFusion code.

   `==` is valid syntax, but it SHOULD NOT be the default style
   unless preserving existing code
   or matching an explicit user request.

2. Properties and forms MUST be declared before use.
   The assistant MUST NOT rely on forward use.

3. String literals MUST use single quotes.
   Double quotes are NOT a valid string literal delimiter
   in lsFusion and MUST NOT be used.

4. An expression whose comma is NOT enclosed in brackets of
   its own — `OVERRIDE a, b`, `CONCAT sep, a, b`,
   `GROUP CONCAT expr, sep` — MUST NOT go straight into a
   comma-separated list (`PROPERTIES`, `EXPORT FROM`,
   `JSON FROM`, `ORDER`, group-object and parameter lists):
   that comma reads as the list separator and the list
   silently reshapes. Group it, or name it as a property, in
   whichever form the enclosing block accepts. A comma inside
   an ordinary call's own parentheses is safe.

5. When introducing a new parameter, the assistant MUST
   declare its class explicitly at the first use
   (`prop(Class x)`, `GROUP MAX Class x IF ...`).
   `AS` does NOT declare the parameter's class: it is
   a cast — the parameter itself stays untyped
   at later occurrences.

6. The body of a `META` statement consists of module-level
   statements; action operators (`NEW ...`, assignments)
   cannot appear there directly, and the `@` statement
   using a metacode is itself a module-level statement
   and cannot be used inside an action body. For
   parameterized object creation, declare an action
   with parameters and call it.

7. The two declaration forms of a local property
   belong to different levels and MUST NOT be mixed up:
   the `LOCAL name = Class (...);` statement is valid
   only inside an action body `{ ... }`,
   while at module level a local property is declared
   as a property definition `name = DATA LOCAL Class (...);`.
   A module-level `LOCAL ...` line does not parse
   (`missing EOF at 'LOCAL'`).

## Boolean type rules

1. For `BOOLEAN` the only values are `TRUE` and `NULL`, and
   `NULL` is the default — `FALSE` is invalid in an expression
   and the parser rejects it with `use NULL instead of FALSE`.
   The type that does have a false value is `TBOOLEAN`, whose
   literals are `TTRUE` and `TFALSE`.

## Property naming policy

1. Property names MUST follow lowerCamelCase,
   as in the official lsFusion coding conventions:
   the first word starts with a lowercase letter,
   and each following word starts with a capital letter.

2. For an object's own primitive attributes,
   the assistant MUST prefer the shortest stable business name
   already used in the project.

   Typical base names in the source are:
   `id`, `name`, `fullName`, `number`, `date`, `dateTime`,
   `status`, `type`, `note`, `details`, `price`, `quantity`,
   `amount`, `email`, `phone`, `address`, `city`, `state`,
   `zip`, `index`, `count`, `color`, `readonly`, `archived`.

3. The assistant MUST reuse an existing base property name
   for the same concept across different classes and signatures
   instead of inventing synonyms.

4. The assistant SHOULD NOT include the owner class name
   in a property's own base attribute
   when a generic name is sufficient.

   Prefer:
   `name(Partner)`, `email(Partner)`, `number(Order)`.

   Avoid:
   `partnerName`, `partnerEmail`, `orderNumber`.

5. The assistant MUST NOT add verbs such as
   `get`, `set`, `calc`, or `compute`,
   or filler words such as `value`, `data`, `info`,
   to a property name unless they are part
   of the actual business meaning.

6. Human-readable wording belongs in the caption,
   not in the identifier.

   The assistant SHOULD keep the property name technical
   and reusable even when the caption is long,
   localized, or contains business phrasing.
