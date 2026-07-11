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
----------------------------------------------------------------
MANDATORY WORKFLOW

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
----------------------------------------------------------------
RULES FOR USING LSFUSION TOOLS

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

3. If syntax, behavior, or capability is uncertain,
   the assistant MUST consult documentation before proceeding.

4. Community retrieval SHOULD be used only when docs
   and how-tos are insufficient for a deep or ambiguous task.

C. ELEMENT SEARCH

1. The assistant MUST prefer structured element search
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
----------------------------------------------------------------
SYNTAX RULES

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

4. Expressions with commas (`OVERRIDE` with multiple
   operands, multi-argument `FORMULA` calls such as
   `toChar(...)`, `GROUP CONCAT` with an explicit separator,
   and similar) placed inside a comma-separated field list
   (`PROPERTIES` blocks, `EXPORT FROM`, `JSON FROM`, `ORDER`
   lists, group-object lists, parameter lists) MUST be
   wrapped in an extra pair of parentheses or extracted
   into a separately named property. Otherwise the inner
   comma is parsed as the list separator and the list
   silently reshapes into something other than the intent.
----------------------------------------------------------------
BOOLEAN TYPE RULES

1. The assistant MUST use `FALSE` only for `TBOOLEAN`.

2. For `BOOLEAN`, the only values are `TRUE` and `NULL`.

3. For `BOOLEAN`, `NULL` MUST be treated
   as the default value.
----------------------------------------------------------------
PROPERTY RULES

1. The assistant MUST NOT declare a property
   if it is used only once.

   Exception:
   a property may still be declared if it is added to a form.

2. Every property parameter MUST be used in its expression.
   Unused parameters are forbidden.

3. The assistant MUST assume standard `NULL` propagation
   for property expressions:
   if any parameter is `NULL`, the result is `NULL`.

   Exceptions that do NOT nullify on a single `NULL` operand:
   `MIN` / `MAX`, the `NULL`-tolerant arithmetic `(+)` / `(-)`,
   and `GROUP` aggregates (`GROUP SUM`, `GROUP MAX`, etc.) —
   a `NULL` operand or value is skipped instead of propagating.

   These exceptions still yield `NULL` when:
   - every operand or aggregated value is `NULL`;
   - `(+)` / `(-)` or `GROUP SUM` produces `0`
     (a zero result is returned as `NULL`).

4. The assistant MUST NOT use `GROUP AGGR`
   inside arbitrary expressions.

   `GROUP AGGR` is allowed only in property definitions.

   When reasoning about it, the assistant MUST treat
   `GROUP AGGR` as `GROUP MAX`
   with an additional constraint.

5. The assistant SHOULD avoid unnecessary conditions
   when the language semantics already produce the required result.

6. The assistant MUST NOT create a property whose expression
   is equal to one of its parameters.

7. The assistant MUST NOT create multiple properties
   with identical expressions.

8. If a property is calculated from another property
   but has different parameters, the assistant SHOULD try
   to keep the same property name.

9. To check whether a property is `NULL`,
   the assistant SHOULD use `IF NOT property(...)`.

   To check that it is not `NULL`,
   the assistant SHOULD use `IF property(...)`.

10. The assistant SHOULD specify `CHARWIDTH`
   in the property definition rather than in form design.

   For a simple property composition that only forwards
   another property, the assistant SHOULD NOT repeat
   `CHARWIDTH` on the derived property unless it must differ.

11. For static objects, the assistant MUST NOT use
    `staticCaption` or `staticName` properties.

    The assistant MUST use `caption` and `name` instead.

    This applies to reads: `caption` and `name` are calculated
    (materialized) properties and do not allow writes.
    At assignment sites (for example, when programmatically
    filling a static object's caption) the DATA properties
    `staticCaption` / `staticName` remain.

12. Property names SHOULD be concise
    and avoid unnecessary words.

13. The assistant SHOULD NOT use words in a property name
    that duplicate parameter class names
    unless required for clarity.

14. The assistant SHOULD NOT specify an explicit namespace
    for a property unless necessary.

15. When creating a DATA property — or a simple composition
    over a DATA property (for example, pulling the name of a
    related object) — for a single object's own attribute,
    the assistant MUST deliberately decide
    whether to place it in the system `id` or `base` group
    via `IN`.

    Attributes that form the object's business identity
    and appear in its representation SHOULD go in the `id`
    group; other primary attributes go in the `base` group
    (`id` is nested under `base`).

    A property SHOULD NOT be placed in `id` or `base`
    when it is not the object's own primary attribute.

16. When dividing values of integer classes, the assistant MUST
    cast one of the operands to `NUMERIC`, not the result.

    The ratio of two integers is integer division,
    so an outer cast like `NUMERIC[16,4](a * b / c)`
    silently drops the fractional part;
    the correct form is `NUMERIC[16,4](a) * b / c`.

17. A parameter's class annotation (`prop(SubClass x)`) is a
    signature, not a runtime filter: it resolves same-named
    properties and sets the signature, but the computed set is
    determined by the properties used in the expression.
    Reading a parent-class property with a subclass-annotated
    parameter still ranges over ALL objects of the parent class
    (e.g. in a `GROUP SUM` — silently wrong totals).

    To restrict the set to a class, the assistant MUST add
    an explicit `x IS SubClass` condition (or use a property
    declared on that subclass).
----------------------------------------------------------------
ACTION RULES

1. The assistant MUST avoid `FOR` when the same result
   can be expressed with a set-based construct.

   `FOR` iterates row by row and SHOULD be the last resort
   when no declarative alternative exists.

   Prefer set-based alternatives, for example:
   - aggregation or set materialization
     -> `GROUP SUM`, `GROUP CONCAT`, `GROUP MAX`,
        `GROUP LAST`, `GROUP AGGR`
   - assigning a property over a set
     -> direct property assignment with parameters
        instead of a `FOR ... DO` loop
   - exporting tabular or hierarchical data
     -> `EXPORT FROM`, `EXPORT JSON FROM`,
        `EXPORT XML FROM`, `EXPORT CSV FROM`
   - building structured payloads
     -> `JSON FROM`, `XML FROM`
   - bulk integration writes
     -> `NEW`, `DELETE`, or set-based property change
        instead of a per-row `FOR`

   `FOR` is acceptable when the body has genuine
   per-row control flow such as conditional `APPLY`,
   `MESSAGE`, `throwException`, or external calls
   that cannot be expressed as a set operation.

2. Parameters introduced in `NEW alias = Class`
   and `FOR expr(p) [NEW alias = Class] DO { ... }`
   do NOT follow the usual lexical scoping rules
   of mainstream programming languages.

   Such parameters are visible ONLY inside the body
   of the `NEW` block or the `FOR` loop that introduces them.

   The assistant MUST NOT reference these parameters
   outside their introducing block.

   When dependent computation must reuse these parameters,
   the assistant SHOULD nest further `NEW` or `FOR` blocks
   inside the introducing block, where the parameters
   are still in scope, rather than lifting values out
   into auxiliary storage.

3. The assistant SHOULD avoid introducing `LOCAL`
   properties without a concrete need.

   Each `LOCAL` is backed by a temporary table
   in PostgreSQL, so it carries a real runtime cost
   well above a stack variable in a conventional language.

4. A `LOCAL` is normally justified when BOTH conditions hold:
   - its value is non-trivial to compute
     (aggregation, joins, multi-step logic, external calls,
     or other work worth materializing), AND
   - the same value is consumed more than once,
     so materializing it avoids recomputation.

5. When possible, the assistant SHOULD prefer alternatives
   to a fresh `LOCAL`:
   - inline the expression at each use site if it is cheap
   - nest `NEW` / `FOR` blocks so intermediate values stay
     in parameter scope
   - use a regular (non-`LOCAL`) calculated property
     when the value is reusable across actions

6. These are recommendations, not hard prohibitions.
   If the assistant cannot find a working syntax for
   a `LOCAL`-free construction, or some other approach
   keeps failing and a clean action cannot be built,
   falling back to a `LOCAL` is acceptable as a last resort.

   Established `LOCAL` patterns mandated by other rules
   (e.g. import staging, nested-session carry-over)
   remain valid; the assistant SHOULD still keep such
   `LOCAL`s minimal in count and scope.

7. The parameters of the top-level statements of an action
   body share one parameter context: identical names denote
   the same parameter, and a parameter's class is declared
   only at its first use.

   In generated scripts (`eval`, data seeding) the assistant
   SHOULD give the parameters of top-level statements unique
   names, so as not to depend on the statement order.

8. Many system utility actions return their result through
   a same-named parameterless `LOCAL` property (for example,
   in `Utils`: the action `fileExists[ISTRING[500]]` writes
   into the property `fileExists[]`). Such an element is an ACTION,
   not a boolean property: the assistant MUST call the action
   first and then read the parameterless property
   (`fileExists(path); IF fileExists() THEN ...`), and MUST
   NOT use the parameterized form inside an expression
   (`IF fileExists(path)` is wrong).
----------------------------------------------------------------
EVENT RULES (`WHEN`)

1. A `WHEN` event fires whenever its condition becomes true
   during a session and writes its target property
   unconditionally. If the same target property is also
   changed explicitly elsewhere in the session
   (user input, action assignment, import),
   the event overwrites that explicit change.

2. When the event's purpose is only to derive or default
   a value from other inputs, the assistant SHOULD guard
   the condition with `AND NOT CHANGED(<target>)`
   for each target property the event writes.

   This prevents the event from clobbering an explicit
   change to the target made elsewhere in the same session.

3. The guard SHOULD be omitted only when the event must
   forcibly override any explicit change — for example,
   maintained totals, audit stamps, or invariants
   the user is not allowed to bypass.

4. Rules 1-3 describe the event-action form
   `WHEN <condition> DO <target> <- <expr>`. The calculated
   event form `<target> <- <expr> WHEN <condition>` behaves
   differently: its change is calculated when the target
   property is accessed, and an explicit change of that
   property in the session takes priority over the event's
   change.

   So to default a value while yielding to an explicit
   change, the calculated event form alone is enough —
   no guard is needed. Testing `CHANGED(<target>)` in its
   condition is not possible in any case: the target would
   then depend on its own change, forming a cycle
   `<target>` -> `CHANGED(<target>)` -> `<target>`.

   In the absence of an explicit change the event writes
   the value of the expression even when it is `NULL`.

5. Inline in an action or event body, `PREV(<expr>)` takes
   the WHOLE wrapped expression to the session-start state,
   including its argument sub-expressions: an argument
   computed in the current session (a `LOCAL`, a property
   of an object created in the session) reads as `NULL`
   inside `PREV`, silently nulling the result.

   To read previous data with current arguments, the
   assistant MUST wrap `PREV` in a separate property —
   `prevF(x) = PREV(f(x));` — and call it instead of
   writing `PREV(f(<session-computed arg>))` inline.
----------------------------------------------------------------
CONSTRAINT RULES

1. When a value choice in one property must be restricted
   based on values of other properties — sibling fields
   on the same form, current context, related objects —
   the assistant SHOULD first consider
   `CONSTRAINT ... CHECKED BY <property>`.

   `CHECKED BY` makes the selection dialog for the listed
   property automatically filter out values that would
   violate the constraint, so the restriction is enforced
   declaratively at the point of selection, not after
   the fact.

2. Manual form filters or hand-rolled validation actions
   SHOULD be the fallback only when `CHECKED BY` cannot
   express the restriction (e.g. the filter depends on
   transient UI state not modeled as a property, or the
   rule is advisory rather than enforced).

3. The assistant SHOULD NOT put heavy aggregates over large
   tables (especially nested non-materialized ones) into a
   `CONSTRAINT` condition: the incremental check at apply
   can expand into an impractically large query, even with
   computation hints on the properties.

   For such expensive checks, use a `WHEN` event instead:
   a cheap change-detector condition, reads of the heavy
   values into `LOCAL`s in the handler, then `MESSAGE` +
   `CANCEL` on violation.
----------------------------------------------------------------
PROPERTY NAMING POLICY

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
----------------------------------------------------------------
INTERNATIONALIZATION AND REVERSE TRANSLATION RULES

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
   this canonical value is the source-language text itself,
   or its normalized stable form,
   so it effectively plays the role of the key.
----------------------------------------------------------------
FORM RULES

1. To place several objects in one table at once,
   the assistant SHOULD combine them into one object group
   using brackets.

2. In a `FORM ... ORDERS` clause, the assistant MUST use
   only form properties that were already added to the form
   via a `PROPERTIES` block.

   In `ORDERS`, the assistant MUST specify either:
   - the form property name with its parameters, if no explicit alias was given
   - the explicit form property alias, if such an alias was specified

   Raw expressions, objects, or properties not added to the form
   MUST NOT be placed into `ORDERS`.

3. The assistant MUST NOT use `INPUT` inside actions
   added directly to a form through `PROPERTIES`,
   unless that action is used in an `ON CHANGE` handler.

   `INPUT` is allowed only in form property change handlers.

4. The assistant MUST NOT display internal object identifiers
   on a form.

   Meaningful properties MUST be shown instead.

5. The assistant MUST NOT add object-valued properties to forms.

   Primitive or derived primitive / text properties
   MUST be exposed instead.

6. A `PANEL` object of a user-defined class
   is NOT user-selectable by default.

   If such an object is meant to be chosen by the user
   (for example, a filter parameter shown on the form),
   the assistant MUST mark a displayed property of that object
   with `SELECTOR` in the `PROPERTIES` block.

   Without `SELECTOR`, the panel cell does not open
   a selection dialog and the object cannot be changed.
   The assistant MUST NOT assume a panel cell is editable
   by analogy with grid editing.

7. These form rules do NOT cover `DESIGN` layout — the default
   container tree, the flexbox `fill` / alignment model, or the
   container idioms. They give only placement meta-advice.
   Before writing or modifying any `DESIGN`, the assistant MUST
   retrieve the `Form_design` documentation; it MUST NOT rely on
   these rules as if they described the layout model.

8. The assistant SHOULD specify a `DESIGN`
   for all interactive forms containing more than four properties.

9. Exception:
   for a trivial form with only one or two objects in `GRID` mode
   and no other properties displayed in `PANEL` mode,
   omitting `DESIGN` is acceptable.

10. In `DESIGN`, the assistant SHOULD prefer moving `BOX(...)`
    containers for tables first.

    `GRID(...)` SHOULD be used only when absolutely necessary.

11. If possible, the assistant SHOULD avoid form designs
    with more than two tables stacked vertically
    and more than two tables placed horizontally.

12. In a form `PROPERTIES` block, the parameter style on the
    property or action being added to the form MUST match
    the block header:
    - With a common-parameter header
      `PROPERTIES(p1, ..., pN)`, each entry MUST be specified
      by its ID only — the common parameters are bound
      implicitly. Writing `propName(p1, ..., pN)` after the
      ID is a parse error.
    - With no common-parameter header (just `PROPERTIES`),
      each entry MUST carry explicit parentheses,
      e.g. `propName(t)` with parameters, or `propName()` /
      `actionName()` for parameterless properties and
      actions. Parentheses are MANDATORY even when there
      are no parameters — empty parentheses MUST still be
      written. Writing the bare name without parentheses
      is a parse error.

    The assistant MUST NOT mix the two styles in one block,
    and MUST NOT repeat the common parameters after the
    property name when a common-parameter header is in use.

    This rule applies only to the entry being added to the
    form. Argument lists inside option clauses such as
    `ON CHANGE actionName(...)`, `READONLYIF expr`,
    `BACKGROUND expr`, etc. are regular action calls /
    expressions and ALWAYS use explicit parameters,
    regardless of the block header.

13. Custom actions added to a grid form (status changes,
    document generation, bulk operations) MUST be given an
    explicit `TOOLBAR` view, e.g. `PROPERTIES(o) confirmDoc TOOLBAR`.
    Actions default to the `PANEL` view, so without `TOOLBAR` the
    custom button is drawn as a separate group below the table
    instead of in the grid toolbar next to the predefined
    `NEW` / `EDIT` / `DELETE` (which the platform places in the
    system toolbar itself). The property / action views are
    `GRID`, `TOOLBAR`, `PANEL`, and `POPUP`.
----------------------------------------------------------------
REPORT RULES

1. Before designing or editing jrxml report templates, or reasoning
   about report structure or template naming, the assistant MUST
   retrieve the `Report_design` documentation; it MUST NOT rely on
   these rules as a template-format or layout reference.

2. When a form has no object groups independent of each other
   (all groups form a single dependency chain), only ONE jrxml
   template is created, named by the form's canonical name
   (namespace + form name, each `.` replaced by `_`) WITHOUT a
   postfix. The `_<group>` postfix and separate subreport templates
   appear only for object groups that are independent of each other.

3. The assistant MUST name every template exactly: the top report
   by the canonical form name without a postfix, and each subreport
   by the canonical form name plus the `_<group>` postfix of its
   first non-empty object group. If even one template name is wrong
   (not found from the platform's point of view), the platform
   silently falls back to a fully automatic design for the WHOLE
   report, with no error in the logs — so a single mismatch silently
   discards all custom templates.
----------------------------------------------------------------
NAVIGATOR RULES

1. A folder whose children should appear only when the folder
   is selected MUST place those children in a different window
   than the folder itself (typically `WINDOW toolbar`). In a
   horizontal toolbar such as `System.root`, a folder that keeps
   its children in its own window cannot switch anything — they
   are shown flattened next to it and selecting the folder does
   nothing. A vertical toolbar instead renders same-window
   children as a nested group under the folder, so there the
   separate window is not required.
----------------------------------------------------------------
MODULE DESIGN RULES

1. The assistant MUST split lsFusion code into modules
   by domain logic or feature area,
   not by arbitrary technical grouping.

2. The assistant SHOULD prefer relatively short modules.

   A single broad module SHOULD NOT keep growing
   when the logic naturally separates
   into smaller cohesive modules.

3. The assistant MUST apply low coupling and high cohesion:
   closely related classes, properties, actions, and forms
   SHOULD stay together,
   and cross-module dependencies SHOULD remain narrow and explicit.

4. Module `NAMESPACE` SHOULD be chosen by shared business domain,
   not by the full module name.

5. When a module belongs to an existing domain family,
   the assistant SHOULD reuse that family namespace
   for all its elements.

   A new namespace SHOULD be created only
   for a genuinely new domain,
   not for each technical submodule.

6. If the module name already equals the intended domain namespace,
   omitting `NAMESPACE` is acceptable
   because lsFusion will use the module name as the default.

   Otherwise, the assistant SHOULD specify `NAMESPACE` explicitly.

7. The assistant SHOULD use `REQUIRE`, `EXTEND`,
   abstract properties / actions,
   and form extensions to connect modules
   instead of duplicating logic
   or creating a god module.

8. Before adding code to an existing module,
   the assistant MUST check whether the logic belongs
   to that module's domain.

   If not, the assistant SHOULD create
   or extend a more appropriate module.

9. When introducing a new module,
   the assistant MUST choose dependencies deliberately
   and avoid circular or unnecessary dependencies.

10. To use a property, action, class, or form
    from another module, that module MUST be reachable
    from the current module's `REQUIRE` chain — either
    directly, or transitively through other required modules.

    If the owning module is not in the transitive `REQUIRE`
    closure, the platform raises a "Property not found"
    (or analogous "not found") error at startup.

    The assistant MUST add the owning module
    (or any module that already requires it)
    to the current module's `REQUIRE` list before using
    its elements.
----------------------------------------------------------------
MIGRATION RULES (`migration.script`)

1. Renaming a property or action, or moving it to another
   namespace, changes its canonical name. Whenever the assistant
   renames or re-namespaces an existing element, it MUST record
   the change in `migration.script` in the same edit; otherwise
   the platform treats the old and new names as unrelated
   elements — the old one is dropped and the new one starts empty.

2. For a primary (`DATA`) property this is silently destructive
   and the assistant MUST take special care. The rename / namespace
   change MUST be recorded as a `STORED PROPERTY` change
   (`old canonical name -> new canonical name`), which renames the
   underlying database column and preserves its data. A plain
   `PROPERTY` change carries over only the security-policy and
   reflection settings, NOT the stored data.

3. Without the `STORED PROPERTY` entry, on the next server start
   the old column is renamed to `<oldID>_deleted` and a fresh
   empty column is created for the new name, so all existing
   values of the property are lost. The assistant MUST NOT rename
   or move a `DATA` property to another namespace without adding
   this entry.

4. Renaming a custom class, or moving it to another namespace,
   MUST be recorded as a `CLASS` change to preserve its objects
   and their data. Such a class rename can also change the
   canonical names of its `DATA` properties; these are not tracked
   automatically and MUST be added as their own `STORED PROPERTY`
   changes.
----------------------------------------------------------------
CHANGE SESSION RULES (`NEWSESSION`, `NESTEDSESSION`, `APPLY`)

1. Before introducing `NEWSESSION`, the assistant MUST decide
   which session behavior is required:
   - isolated independent unit -> `NEWSESSION`
   - isolated unit that must also see selected local properties
     from the upper session -> `NEWSESSION NESTED (...)`
   - isolated unit that must see all local properties
     from the upper session -> `NEWSESSION NESTED LOCAL`
   - child dialog or editor that must work with unsaved upper-session
     objects and return its changes to that upper session
     -> `NESTEDSESSION`

2. For actions added to forms,
   there are two main patterns:

   - readonly form pattern:
     the form is effectively browse-only, so actions added to it
     SHOULD run in a new session by default
   - editable form pattern:
     the form has editable properties, so any action added to it
     that uses `NEWSESSION` MUST either:
     `APPLY;`
     `IF canceled() THEN RETURN;`
     before `NEWSESSION`, or be fully independent
     from unsaved changes in that form

3. Plain `NEWSESSION` is the default
   for isolated work that must not accidentally apply
   the caller's pending form changes.

   Typical patterns in the source:
   - readonly list forms with
     `PROPERTIES(...) NEWSESSION NEW, EDIT, DELETE`
   - status transitions or dependent document creation
     after a preceding `APPLY`
   - external or integration actions that isolate HTTP calls
     and persist their own results
   - small immediate UI updates with
     `NEWSESSION { APPLY { ... } }`

4. If inner logic depends on upper-session local state
   such as selections, marks, or import buffers,
   the assistant MUST carry that state explicitly
   through `NESTED (...)` or `NESTED LOCAL`.

5. `APPLY` resets every `LOCAL` property in the current
   session by default. After `APPLY` returns, a plain
   `LOCAL` is empty again, even on success.

   A `LOCAL` value survives `APPLY` when EITHER:
   - the `LOCAL` is declared as `NESTED` at declaration
     time (`LOCAL NESTED name = Type;` or
     `name = DATA LOCAL NESTED Type (...);`), OR
   - the `APPLY` explicitly preserves it via
     `APPLY NESTED (name1, ..., nameN)`
     or `APPLY NESTED LOCAL` for all locals.

   The assistant MUST NOT rely on a plain `LOCAL` value
   computed before `APPLY` to still be readable after it.
   If a staged value must outlive `APPLY` — for example,
   an import buffer read during post-apply follow-up —
   the assistant MUST either declare it with `NESTED`,
   or list it in `APPLY NESTED (...)` (or use
   `APPLY NESTED LOCAL`) at the call site.

6. When using `NEWSESSION NESTED (...)` or
   `NEWSESSION NESTED LOCAL`, the assistant SHOULD preserve
   the same nested local properties on `APPLY`
   if the result must be copied back to the upper session,
   for example with `APPLY NESTED (...)`
   or `APPLY NESTED LOCAL`.

7. The assistant MUST NOT replace `NESTEDSESSION`
   with plain `NEWSESSION` for child forms or dialogs
   attached to a parent object that may still be unsaved
   in the current form session.

8. Before opening a fresh `NEWSESSION` from an action
   started on an edit form, the assistant SHOULD decide
   whether current form changes must be saved first.

   The common pattern is:
   `APPLY;`
   `IF canceled() THEN RETURN;`
   `NEWSESSION { ... }`

   This pattern is used before status changes,
   document generation, and other isolated follow-up actions.

9. After `APPLY`, the assistant MUST check `canceled()`
   only when later logic depends on whether the save
   succeeded — to early-return, skip a follow-up side
   effect, or roll back staged work.

   `APPLY` in an interactive context shows the constraint
   message to the user on its own. The assistant MUST NOT
   add `IF canceled() THEN MESSAGE applyMessage()` after
   `APPLY` in interactive actions solely to report the
   failure — it duplicates the message the platform
   already shows. Explicit surfacing via `applyMessage()`
   or `throwException(applyMessage())` is required only
   for non-interactive callers (API endpoints, background
   integrations) where no dialog is shown.

   If `APPLY` fails because of a constraint, the changes
   remain unsaved in the current session, and any following
   `APPLY` in the same session will also fail until the
   offending data is fixed or the changes are discarded
   (for example with `CANCEL`).

10. The assistant SHOULD keep `NEWSESSION` blocks small
    and purpose-specific: isolate one unit of work,
    apply it if needed, and exit.

    The assistant MUST NOT introduce `NEWSESSION`
    merely to hide session-visibility bugs.
    If upper-session changes must remain visible,
    nested session semantics are required.
----------------------------------------------------------------
IMPORT RULES (`IMPORT`)

1. Before working with `IMPORT`, the assistant MUST identify
   elements in this order:
   - module and namespace that own the import flow
   - target classes that will be created or updated
   - staging properties used during import
   - import actions
   - import forms, if the payload is hierarchical

2. The assistant MUST choose the import style intentionally:
   - flat files (`CSV`, `XLS`, `DBF`, `TABLE`)
     -> prefer `IMPORT ... TO` or `FIELDS`
   - nested `JSON` / `XML`, parent-child structures,
     namespaces, or `EXTID` mapping
     -> prefer form import
   - row-at-a-time integration responses
     -> prefer `FIELDS ... DO`

3. For flat imports that need validation, deduplication,
   multi-pass processing, or post-processing,
   the assistant SHOULD stage data into `LOCAL` properties
   first, usually by `INTEGER` row,
   then process it in a separate
   `FOR imported(INTEGER i)` pass.

4. The assistant SHOULD use `FIELDS ... DO`
   when imported values are consumed only once
   and introducing reusable local properties
   would add noise.

5. The assistant SHOULD specify column mappings explicitly
   when the external template is fixed or sparse.

   Sequential mapping without explicit column IDs
   is acceptable only when column order itself
   is the agreed interface.

6. For form import, the assistant MUST declare
   a dedicated import form before use.

   The form MUST use one object per object group
   with numeric or concrete user classes.

   The form SHOULD mirror the external structure with:
   - `FILTERS` for parent-child links
   - `EXTID`, `FORMEXTID`, groups, and `ATTR`
     only where the external schema requires them

   The assistant MUST remember that importing into a form
   cancels pending changes to imported form properties
   in the current session.

7. The assistant MUST choose format options explicitly
   when the external contract depends on them:
   - `HEADER` / `NOHEADER`
   - `SHEET`
   - `CHARSET`

   The assistant SHOULD prefer `HEADER`
   for stable `CSV` / `XLS` templates,
   because `NOHEADER` can silently map missing
   or mistyped columns to `NULL`.

8. The assistant MUST validate referenced business keys
   before creating or updating persistent objects.

   Typical keys in this project are `id`, `number`,
   partner or item codes, and external references.

   Each reference MUST be checked
   in a separate `FOR`
   using `GROUP SUM 1 BY`
   over the imported key values.

   If possible, the assistant SHOULD NOT write
   resolved references to a separate `LOCAL`
   before the main import logic.

   Missing master data or malformed payloads
   MUST stop the import or surface a clear error.

9. The assistant SHOULD separate raw import
   from domain resolution:
   - first parse the file or payload
     into locals or an import form
   - then check references such as
     item, partner, status, type, or other lookups
   - only then create or update domain objects

10. For user-started batch imports and external integrations,
    the assistant SHOULD isolate persistence in `NEWSESSION`.

    After domain writes, the assistant SHOULD `APPLY;`.
    If later logic depends on success, the assistant MUST
    check `canceled()` to gate that follow-up logic;
    explicit surfacing via `applyMessage()` or
    `throwException(...)` is needed only for non-interactive
    callers (see CHANGE SESSION RULES, rule 9).

    If the import must see upper-session local buffers,
    the assistant MUST use nested session semantics
    instead of plain `NEWSESSION`.

11. The assistant MUST NOT partially persist
    a failed import silently. For failures the assistant
    detects on its own (missing references, malformed
    payload, pre-`APPLY` validation), it SHOULD use
    `MESSAGE`, `RETURN`, `throwException`, or an explicit
    failure flag, consistent with the caller:
    - interactive import -> `MESSAGE`
    - API or background integration
      -> exception or explicit failure state

    Failures raised by `APPLY` itself are already surfaced
    by the platform (see CHANGE SESSION RULES, rule 9);
    the assistant MUST NOT duplicate them.

12. When importing booleans,
    the assistant MUST remember workspace boolean rules:
    - `BOOLEAN` uses `TRUE` and `NULL`
    - `FALSE` is valid only for `TBOOLEAN`

13. For create-or-update synchronization imports,
    the assistant MUST separate object creation
    from property updates.

    The assistant MUST use one separate `FOR`
    to create missing objects only.

    If imported key values may be non-unique,
    the creation pass SHOULD iterate by grouped keys
    using `GROUP SUM ... BY`
    rather than by raw imported rows.

    The assistant MUST then use a second separate `FOR`
    to update properties of matched objects.

    The assistant MUST NOT mix object creation
    and property updates in the same loop
    for synchronization imports.

    If full synchronization is required,
    the assistant SHOULD add an explicit delete step.

14. If `LOCAL` staging properties
    are used only in one import action,
    the assistant MUST declare them
    inside that action.

    The assistant SHOULD NOT lift such `LOCAL` properties
    to module scope without need.

    Exception:
    a `LOCAL` property may be declared outside the action
    only when it must be used by an import form
    or reused by several related actions.
