SYSTEM PROMPT - lsFusion DOCUMENTATION TASK RULES

SCOPE: describing, editing, restructuring, and clarifying
lsFusion documentation only.

These rules apply only to documentation tasks.

----------------------------------------------------------------

RULE SCOPE

The rules below are in two parts.

COMMON RULES apply to every documentation task, of any type.

TYPE-SPECIFIC RULES apply only when working on that documentation type,
and apply to BOTH language versions of it (docs/en/<type> and
docs/ru/<type>) — these are agent rules, not translated content:
- LANGUAGE — docs/<lang>/language/  (syntax-construction articles)
- PARADIGM — docs/<lang>/paradigm/   (abstractions and concepts)
- GUIDE    — recommendation articles (filename prefix `Guide`; no dedicated folder)
- HOW-TO   — docs/<lang>/how-to/     (task recipes)

Keep a rule that applies to only one type in that type's section below.

----------------------------------------------------------------

COMMON RULES

----------------------------------------------------------------

DOCUMENTATION STRUCTURE

Documentation is divided into parts.

These parts define:
1) the order in which documentation should be written
2) the order in which language capabilities should be understood

Paths in this file are written relative to the `platform/` repo root
(sibling to `../docusaurus/`, `../mcp/`, etc. in the `lsfusion-aggregate/`
workspace).

Common structure rules:
- the documentation structure / hierarchy
  for navigation
  is defined in `sidebars.js`
  in this `platform/docs` directory
- the assistant MUST treat
  `platform/docs/sidebars.js`
  as the source of truth
  for section placement
  and parent-child relationships
  in the docs,
  while treating
  the sibling `../docusaurus/` project
  as a derived copy
  of the documentation
  from `platform/docs`
  (its own `sidebars.js`
  is only a loader
  that reads
  `platform/docs/sidebars.js`)
- the assistant MUST NOT edit
  files inside `../docusaurus/`;
  all documentation content
  and navigation changes
  MUST be made in `platform/docs`
  (navigation in `platform/docs/sidebars.js`)
- the assistant MUST make
  all documentation content changes
  in both language versions
- the versions in different languages
  MUST contain
  the same information
  and structure;
  they are translations
  of the same documentation,
  not separate documents
- if a new block
  or section is created,
  the parent
  or higher-level section
  SHOULD contain
  a link
  to that new section
  and a brief description
  of what that section covers
- in that case,
  the assistant SHOULD also
  update `platform/docs/sidebars.js`
  so that the new block
  or section is included
  in the documentation hierarchy
  and navigation
- the documentation
  SHOULD preserve
  clear links between sections
  so that its hierarchy
  and navigation
  remain explicit
- if a section
  becomes too large
  and can be split
  into logically complete blocks,
  the assistant SHOULD do that
  or suggest doing that
- Language and Paradigm
  articles
  are primary documentation
  and MUST NOT
  reference Guide
  or How-to articles;
  cross-references
  go from Guide / How-to
  to Language / Paradigm,
  never the other way around

The required order is:
1) Language
2) Paradigm
3) Guide
4) How-to

The assistant MUST first look at Language,
then try to understand the abstractions,
then try to compose Guide and How-to.


----------------------------------------------------------------

GENERAL GUIDE

There is also a general guide: `../mcp/brief.md`.

It is assumed to exist.
It SHOULD contain important / key information
from all documentation parts,
and this information SHOULD always be available
in the context for the AI assistant.

----------------------------------------------------------------

RULES FOR UNDERSTANDING LANGUAGE CAPABILITIES

1. RESEARCH PRIORITY
   The assistant MUST inspect functionality
   and implementation details in both:
   - the platform project
   - the plugin project

   The platform implementation has higher priority.

2. RESEARCH FLOW
   The assistant MUST first determine
   which syntax constructions are relevant.

   After that it MUST:
   - inspect the primary platform / plugin sources
   - inspect examples in existing lsFusion code
   - inspect supplementary materials

3. PRIMARY SOURCE REVIEW
   In the platform project, the preferred direction is:
   `LsfLogics.g` ->
   `ScriptingLogicsModule` ->
   `LogicsModule` ->
   the classes they use and the semantics of those classes

   In this repository the corresponding paths are:
   - `platform/server/src/main/antlr3/lsfusion/server/language/LsfLogics.g`
   - `platform/server/src/main/java/lsfusion/server/language/ScriptingLogicsModule.java`
   - `platform/server/src/main/java/lsfusion/server/logics/LogicsModule.java`

   In the plugin project, the preferred direction is:
   `LSF.bnf` ->
   mixin / implements classes ->
   methods in `LSFPsiImplUtil` ->
   the classes they use and the semantics of those classes

   In this repository the corresponding paths are:
   - `plugin-idea/src/com/lsfusion/lang/LSF.bnf`
   - `plugin-idea/src/com/lsfusion/lang/psi/LSFPsiImplUtil.java`

4. SUPPLEMENTARY SOURCES
   The assistant MUST search for examples
   in existing lsFusion code
   in order to understand:
   - how it is used
   - how it can be used

   The assistant SHOULD also look for
   relevant community and history materials
   (tutorials, articles, discussions,
   GitHub issues, commits,
   and GitHub issues referenced
   from those commits)
   using the available tools.

   In this repository
   it SHOULD also inspect:
   - `rag-fill/src/main/resources/docs`

5. ATTENTION TO IMPLEMENTATION DETAILS
   The assistant MUST inspect the source code
   as carefully as possible
   so as not to miss non-obvious behavior.

6. GUIDE / HOW-TO WRITING
   When writing Guide / How-to,
   the assistant MUST also use examples
   from existing code.

----------------------------------------------------------------

----------------------------------------------------------------

RULES FOR DOCUMENTING LSFUSION

This documentation will be used by a human
and by an AI assistant
to write code, explain code, and similar tasks.

General writing goals:

The documentation MUST be written so that
the reader can understand exactly:
- how the written code will work
- what can / should be done
- what cannot / should not be done

If there is a potential question such as:
- how does this work
- what will happen in this case
- can this be done this way

then it SHOULD be described / explained in the documentation.

At the same time,
the description should be concise,
but also complete.

When the documentation states
that a value,
class,
result,
or behavior
is derived,
computed,
determined,
inferred,
or chosen,
it MUST give
the concrete rule for how:
the actual inputs
and the operation
or choice applied to them,
stated specifically enough
that an outside reader
can determine the outcome
unambiguously.
Asserting only
that something
"is derived from",
"depends on",
or "is based on"
some inputs,
without the rule
that produces the result,
is insufficient.

Wording, naming, and terminology:

The assistant SHOULD try to describe
all significant details of the language
and usage variants.
The assistant MUST reread
the resulting text
and check
that it reads naturally
and coherently,
not as stitched fragments,
mechanical paraphrases,
or isolated remarks.
The assistant MUST phrase things
as simply,
naturally,
and specifically
as possible;
if several correct phrasings
are available,
it MUST prefer
the most natural,
smooth,
and accurate one,
not one
that is merely
shorter
or denser;
if the exact
reader-visible element,
property,
class,
or relation
can be named directly,
the assistant MUST name it
instead of replacing it
with a broader
or more abstract word
and SHOULD prefer
direct,
compact noun phrases
over heavier
relative-clause wording
such as
`which ...`
or `that ...`
when the meaning
stays equally clear
and MUST avoid
awkward noun chains
and repeated
generic words,
especially when
the repetition
appears only
because of
the surrounding wording
or a linked term
and avoid
unnecessarily fancy,
bureaucratic,
or pseudo-formal wording,
unless a more technical phrase
is an established term
of the platform
or documentation.
The assistant MUST NOT
invent a new
or unusual word
when an ordinary
everyday word
carries the same meaning
(for example,
prefer "new"
over "fresh",
"paired with"
over "coupled with",
"target"
only where it is
already the established
documentation term);
if a plain word
fits,
use the plain word.

The assistant MUST NOT describe a construction
by metaphor, analogy,
or a label imported
from an external type system,
language-theory framework,
or design-pattern catalogue;
the construction MUST be described
in terms of what it does
and what it produces,
using the platform's own
abstraction names literally.
Imported labels are allowed
only if they are
established terms
in the platform
or in the existing documentation.

Sibling section headings
at the same depth
SHOULD follow one consistent form
(usually the shortest noun phrase
that names the topic).
When a subset of siblings
shares a common umbrella,
the umbrella SHOULD be factored
into a deeper sub-heading
rather than left alongside
unrelated siblings,
provided the resulting sub-section
has enough material
to stand on its own;
otherwise the sub-structure
SHOULD be inlined.
Canonical structural headings
mandated for a part
(`Syntax`, `Description`,
`Parameters`, `Examples`,
`Language` and similar)
are exempt from this check.

When several parallel variants,
cases,
options,
or scenarios
of one construction
are documented together,
they MUST be organized
as one consistent structure:
one list
with parallel items,
or one subsection
with parallel subparts.
The assistant MUST NOT split
the same set
between inline prose,
separate lists,
and unrelated paragraphs.
Each parallel item
SHOULD present
the same relevant
and primary facts
in the same order
(for example,
what the item is,
the rule it follows,
and the result it produces),
when those facts apply
to that item.

Properties and actions
MUST be referenced
using the property ID form
from `en/IDs.md#propertyid`
and `ru/IDs.md#propertyid`,
with the signature
in square brackets
(`foo[]`, `foo[TEXT]`,
`foo[TEXT, INTEGER]`);
parenthesis-call notation
(`foo()`, `foo(<name>)`)
MUST NOT be used.

In Paradigm articles
and in other prose
describing the abstraction
of a language construction,
the human-language name
of that construction
SHOULD be used,
not its keyword form.
Language articles
are exempt,
since they describe
the syntax of the keyword
and naturally take
the keyword form as the subject.
Inside `Syntax` blocks
and code examples
in any article
the keyword form
is also appropriate.

Preserving existing material:

When completing or extending
existing documentation,
the assistant MUST preserve
as much of the current text
as possible
and MUST prefer extending it
in place
over rewriting,
reordering,
or replacing it,
unless the current text
is incorrect, unclear,
duplicated,
no longer relevant,
or conflicts
with the required structure.
By default,
the assistant SHOULD add
missing information
next to the existing
relevant sentence,
paragraph, list item,
or example,
instead of replacing
a larger block.
When changing
existing text,
the assistant MUST keep
the scope of the edit
as small as possible:
replace a phrase
before a sentence,
a sentence
before a paragraph,
and a paragraph
before a section.
Deletion or compression
is a last resort.
The assistant MUST NOT
remove existing text
only to simplify wording,
unify style,
or make the article shorter
if that text
is still correct,
relevant,
and structurally valid.
When rewriting
or tightening text,
the assistant MUST NOT
drop informative details
that are still correct
and relevant,
including links
to related articles,
semantic qualifiers,
defaults,
or other clarifications
that add understanding.
Before removing
or compressing
such details,
the assistant MUST compare
the new wording
with the original version
of the article
and make sure
that those details
are still preserved.
Every removal
or replacement
of existing material
MUST have
a concrete reason:
incorrectness,
irrelevance,
duplication,
or structural conflict.
The assistant SHOULD try to preserve
the existing style, size, and level of detail.

Cross-references and links:

The assistant MUST NOT
add tautological
cross-reference remarks
that merely say
that the linked section
or article
contains the corresponding
general rules,
examples,
syntactic variants,
or other material
of the kind the link
is already to,
if that already follows
from the link itself
and adds no new
local information;
a plain link
without such a remark
is preferred.

What not to restate or invent:

When documenting
any language construction
or other element,
the assistant MUST describe
only what is primary
and specific
to the current section;
general rules,
resolving,
lookup,
overloading,
signature rules,
and special cases
that are already described
in other sections
MUST be reused
or referenced,
not restated
or duplicated;
things that are
already obvious
from the syntax,
directly follow
from already stated rules,
or can be determined
from canonical
general documentation
MUST NOT be restated
locally;
the assistant MUST NOT
explain a local rule
mainly through contrast
with another rule,
operator,
or section
if that contrast
adds no primary
information.
The assistant MUST NOT
add tangential comments,
background remarks,
or implementation advice
that are not directly needed
to understand
the current article
or the current rule.
In particular,
internal optimisations
performed by the platform
(short-cutting
identity compositions,
caching,
shared sub-expression reuse,
constant folding,
and similar runtime
or query-builder optimisations)
MUST NOT be documented
in Paradigm articles
unless the optimisation
is itself
an abstraction
the developer must reason about;
typical optimisation tells
such as
"no new property is created" /
"the result is equivalent to X" /
"computed in a single pass"
add no abstraction-level information
and MUST be omitted.
The assistant MUST NOT
invent,
extrapolate,
or document
usage patterns
or semantic scenarios
as documented behavior
unless they are directly supported
by the grammar,
platform or plugin code,
existing lsFusion examples,
or existing documentation.
This is especially important
for Language articles:
syntax documentation
MUST describe
what can be written
and what it means,
not speculative usage.
Pure usage variants
SHOULD appear
only in Guide / How-to
and only when grounded
in such sources.
The assistant MUST NOT
add abstract,
empty,
or low-information
bridge sentences
that only restate
the documentation structure
without adding
syntax,
semantics,
behavior,
or usage details.
A lead-in
of the form
"X works uniformly
for any Y"
or "X is the same
for every Y"
followed
by per-Y rules
contradicts itself
(the per-Y rules
show that the behavior
is not uniform)
and adds
no real information;
the assistant
MUST drop
the lead phrase
and state
the per-Y rules
directly.
The assistant MUST
state
the direct primary rule,
not its converse,
complement,
obvious consequence,
or special-case contrast.
The assistant MUST NOT
add clarifications
for particular cases
or mirrored,
contrastive,
or otherwise derived
statements
that only restate
such general
or derived information,
including obvious
special cases
such as empty variants
of an already described form
or special cases
that only follow
from canonical general rules
described elsewhere.

Minor technical details
MAY be omitted,
especially if they are hard
to explain clearly
and do not add
useful understanding
for the reader.

Declarative wording for restrictions:

When describing
rules, restrictions,
or requirements,
the assistant SHOULD prefer
declarative wording:
- what can be done
- what cannot be done
- what must be done
- what must not be done

Platform errors,
warnings,
or other reactions
SHOULD be described
as secondary consequences
or clarifications,
not as the primary way
to formulate the rule.

Deprecated functionality:

The assistant MUST NOT
document deprecated functionality:
syntax constructions,
options,
URL schemes,
parameters,
properties,
or other elements
that are marked deprecated
in the platform or plugin sources,
known to be broken,
or otherwise no longer supported
MUST be omitted
from the documentation,
even if they are still
recognized by the parser
or runtime.

Examples:

When adding code examples,
the assistant MUST validate them in the IDE
if such access or tools are available,
for example by creating the corresponding code fragment.
If IDE validation is not available,
the assistant MUST use a syntax-checking tool,
if such a tool exists.

----------------------------------------------------------------

RULES FOR ERRORS, PROHIBITIONS, AND RECOMMENDATIONS

Restriction priority:

When describing or implementing restrictions,
the assistant MUST reason
in the following order,
from the strictest level
to the least strict level:

1. plugin errors / warnings
2. platform (application server) errors / warnings at startup
3. prohibition / recommendation in the general guide
4. prohibition / recommendation in the guide
   for the corresponding element
5. warning in the Language or Paradigm documentation
   for the corresponding element

Application rules:

The assistant SHOULD try to add support
at all applicable levels.

The assistant SHOULD try to add the restriction
at the strictest level possible.

If that is not possible,
the assistant SHOULD explicitly propose doing so
in the response to the requester.

Assessing available levels:

To check or assess the available error level
in the platform and plugin,
the assistant MUST inspect the source code
according to the source-review rules above.

----------------------------------------------------------------

MAINTAINING THESE RULES

When adding or extending a rule
in this file,
the assistant MUST re-read
the surrounding sections first
and place the rule
in the section
that matches its scope:
a rule that applies
to every documentation type
goes in the COMMON RULES part;
a rule that applies
to only one type
goes in that type's section
in the TYPE-SPECIFIC RULES part
(see RULE SCOPE at the top).
If a similar rule
already exists,
the assistant MUST extend
or tighten it in place;
overlapping or contradicting rules
MUST be reconciled
so this file reads
as a single source of truth.
This file is kept
intentionally compact
to fit in agent context;
new rules SHOULD be phrased
as briefly as possible
without losing precision.

This file describes
general documentation
conventions only.
Rules specific to
a project,
task plan,
or workflow
MUST stay
in the corresponding
plan or workflow file.

----------------------------------------------------------------

TYPE-SPECIFIC RULES

----------------------------------------------------------------


----------------------------------------------------------------

LANGUAGE — applies to docs/<lang>/language/

----------------------------------------------------------------

### 1. Language

Language documentation is organized by syntax constructions.

It MUST contain:
- the language syntax itself
- a description of the syntax
- the mechanics of expression
  for that construction —
  how an element is
  **defined**,
  **named**,
  **looked up / resolved**,
  **bound to parameters**,
  **declared at one site
  vs reused
  by reference** —
  including the syntactic forms
  each option takes
  (e.g. a value-supplying argument
  declared inline
  vs given by reference
  to an existing element)
- a basic explanation
  assuming that the reader already more or less understands
  the paradigm

Language article structure convention:
- a Language article SHOULD begin
  with a short definition
  of what the construction does,
  creates,
  or describes,
  with a link
  to the main abstraction
  when relevant
- when the corresponding sections
  are needed,
  they SHOULD usually go
  in this order:
  `Syntax`,
  `Description`,
  `Parameters`,
  `Examples`
- `Syntax`
  SHOULD contain
  only the syntax itself
  and helper syntax rules
  needed to read it
- `Parameters`
  SHOULD contain
  only reader-visible
  node-level elements:
  non-keyword placeholders,
  keywords,
  and options
  corresponding
  to reader-visible choices;
  details that are localized
  to one parameter
  or option
  SHOULD be described
  in that parameter item,
  including,
  when relevant,
  rules about
  how that element
  may use
  or introduce
  parameters
- a `Parameters` item
  may present
  a parameter's values
  only as keywords;
  it MUST NOT inline
  a syntactic construction
  (grammar fragment,
  non-keyword placeholders,
  optional or composite forms) —
  such a form
  belongs in `Syntax`
  or an intermediate rule,
  referenced from `Parameters`
  by name only
- `Syntax`,
  `Parameters`,
  and intermediate rules
  SHOULD be mutually consistent:
  everything referenced
  in `Syntax`
  SHOULD be covered
  either
  by an intermediate rule
  or in `Parameters`,
  and everything described
  in `Parameters`
  or intermediate rules
  SHOULD correspond
  to `Syntax`;
  local names
  later used
  in `Description`
  or `Parameters`
  SHOULD be introduced
  in `Syntax`
  or intermediate rules
  first,
  unless they are
  already established
  canonically
- `Description`
  SHOULD contain
  everything else:
  semantics,
  behavior,
  defaults,
  restrictions,
  interactions,
  and usage notes,
  except details
  that belong
  to one specific
  parameter
  or option;
  such details
  SHOULD be described
  only
  in that parameter item;
  the section
  SHOULD read
  as a coherent explanation
  of the construction,
  not as disconnected notes

Language file naming convention:
- file names usually contain the syntax token in uppercase

RULES FOR DESCRIBING LANGUAGE SYNTAX ARTICLES

When writing,
completing,
or extending
an article for a language syntax construction,
the assistant MUST:

Consistency with existing documentation:
- preserve
  the current syntax-description style
  used in the documentation
  (for example,
  optional fragments
  are written
  in square brackets `[...]`);
  before extending
  or finishing
  syntax documentation,
  it MUST read
  analogous articles
  and inspect
  how the corresponding material
  is documented there
- when extending
  or finishing
  syntax documentation,
  it MUST search
  for analogous syntax constructions
  in other articles;
  equivalent fragments
  and behavior
  MUST be documented consistently:
  either described
  in the same way,
  or linked
  to an existing section,
  or extracted
  into a separate section
  and referenced
  from the relevant articles

Reader-facing explanation:
- when describing
  a syntax construction
  or one of its parts,
  it MUST explain
  what exactly
  can appear there
  from the reader's point of view;
  if needed,
  it MUST refer
  to other syntax rules
  or related articles
  so that the reader
  can understand
  what is allowed
  in that position
- if a syntax construction
  uses parameters
  in a non-obvious way,
  the article
  MUST make clear
  which parameters
  are available
  and where they
  may be declared
  or used;
  only parameter behavior
  that is specific
  to the current
  construction
  or changes
  the general rules
  SHOULD be described
  there;
  the assistant
  MUST NOT restate
  general parameter
  rules
  just because
  they also apply
  here,
  such as
  parameters being local
  to the construction,
  being introduced
  implicitly,
  repeated names
  denoting
  the same parameter,
  or classes
  not being specified
  again;
  if that behavior
  is tied
  to a specific
  reader-visible element,
  it SHOULD be described
  in that element's
  parameter item;
  if that behavior
  is complex,
  the assistant
  MUST simplify it
  as much as possible
  in the description
  and SHOULD prefer
  a short
  declarative
  reader-facing rule
  over step-by-step
  context-building
  mechanics
- `Examples`
  SHOULD preferably
  cover
  all syntax variants
  and all parameters
  or options;
  when possible,
  the assistant SHOULD use
  compact examples
  that cover
  several such variants
  at once
- keep `Parameters`
  focused and concise:
  use them only
  for syntax elements
  that the reader
  actually writes or chooses;
  defaults,
  restrictions,
  interactions,
  and behavior notes
  that apply
  to one parameter
  or option
  MUST be described
  in that parameter item;
  if the default value
  of a parameter
  or option
  is not obvious,
  that default
  MUST be made clear
  there;
  if the meaning
  of the absence
  of a keyword
  or option
  is not obvious,
  that absence
  MUST be made clear
  there,
  and MUST NOT
  be repeated
  in the general
  `Description`;
  container helper rules
  that only group
  other syntax parts
  MUST NOT appear there
  as parameter names;
  if a reader-visible
  part of the syntax
  is a choice
  among keywords,
  it MUST be described
  there
  as a parameter
  or option
  with those keyword values;
  any local name
  later used
  for that choice
  in `Parameters`
  or `Description`
  MUST be introduced
  in `Syntax`
  first;
  references
  to parameters
  or options
  in `Parameters`
  and `Description`
  SHOULD use
  simple reader-facing names
  from the visible syntax
  or established
  canonical terminology,
  not invented names
  and not raw
  syntax fragments
  or alternative lists
  such as
  ``A | B``
  and MUST NOT
  be documented
  as a separate rule
  in `Syntax`;
  if several keyword choices
  are independent,
  they MUST be described
  as separate parameters
  or options,
  not as one
  combined keyword value;
  the assistant MUST NOT
  spell out
  trivial consequences
  of the syntax notation
  that already follow
  directly from the syntax,
  such as explaining
  how an empty list
  is written;
  however,
  it MUST still say
  whether the list
  may be empty
  or not;
  for IDs,
  expressions,
  typed parameters,
  literals,
  selectors,
  options,
  and other reusable element kinds,
  it SHOULD link
  to the canonical article
  instead of redefining them locally;
  standard parameter
  mechanics
  SHOULD stay
  in those canonical
  articles;
  for typed parameters,
  the canonical place
  is currently
  `ru/IDs.md#paramid`
  and
  `en/IDs.md#paramid`
- if a term
  has already been linked
  earlier in the same article,
  the assistant SHOULD NOT
  add another link
  for later occurrences
  of that same term

Syntax block rules:
- make sure
  that each syntax block
  reflects the full real grammar,
  including all relevant variants;
  all code-valid
  grammatical branches,
  optional syntactic continuations,
  and other real syntax variants
  MUST appear
  in syntax blocks,
  not be left only
  to prose;
  there MUST NOT
  be syntax
  that is valid
  in the code
  but invalid
  in the documentation
- check consistency
  between `Syntax`,
  `Parameters`,
  and intermediate rules:
  every placeholder,
  keyword choice,
  and intermediate rule
  referenced
  in `Syntax`
  MUST be covered
  by `Syntax`,
  `Parameters`,
  or intermediate rules,
  and nothing described
  in `Parameters`
  or as an intermediate rule
  SHOULD be absent
  from `Syntax`
- make sure
  that each syntax block
  contains one
  syntax construction;
  named subrules
  SHOULD have names
  that make it clear
  what they contain
  (for example,
  expressions
  SHOULD use `expr`
  in the name),
  while remaining
  reasonably concise;
  named subrules
  and syntax fragments
  MUST be explained in prose
  and, if separate,
  given in their own
  syntax blocks,
  usually introduced
  in the style
  `Where ... is defined as:`
- check
  the documented syntax
  for presentational redundancy
  and readability;
  if several variants
  differ only
  in one fragment,
  common parts
  SHOULD be factored out
  into the main rule
  or a shared subrule,
  rather than repeated
  in each variant;
  after factoring out
  common parts,
  the assistant MUST check
  that alternatives
  still read as alternatives,
  not as a sequence
  of simultaneously allowed
  fragments;
  factorization MUST NOT
  accidentally turn `OR`
  into implicit `AND`;
  syntax blocks
  MUST stay compact,
  structural,
  and factored;
  the assistant MUST NOT
  replace structure
  with a long enumeration
  of keyword combinations
  or expand
  an intermediate rule
  into near-identical variants
  just to encode
  option compatibility
  or semantic constraints
  when common parts
  can be factored out;
  when a construction
  varies along several
  independent dimensions,
  the factored presentation
  with separate parameters
  or options
  SHOULD be preferred
  over enumerating
  all combined forms
  or using
  a looser syntax
  plus prose
  when the dependency
  or restriction
  can be expressed
  compactly
  in the syntax itself
  without harming
  readability
  or forcing
  near-identical variants;
  if a more compact
  factored syntax
  is clearer,
  it MAY be used
  even if it is
  a controlled superset
  of the code-valid forms,
  but it MUST still cover
  all code-valid forms,
  and the extra
  semantic restrictions,
  allowed combinations,
  and option compatibility
  MUST then be stated
  in `Parameters`
  or `Description`
- show alternative variants
  with explicit alternation
  without using `|`,
  with common parts
  factored out;
  prose MAY clarify
  defaults,
  semantic restrictions,
  invalid combinations,
  or other non-grammatical points,
  but MUST NOT replace
  missing syntax variants
- removing `|`
  does NOT mean
  expanding a small choice
  into N alternative lines
  of the syntax block.
  If alternative lines
  would share
  non-trivial common parts
  (a multi-line tail,
  a non-trivial common prefix,
  or a non-trivial common suffix
  even on a single line —
  for instance,
  the same placeholder list,
  the same options,
  or the same closing tokens),
  the result reads
  as a sequence
  attached only
  to the last alternative
  (when the tail spans
  multiple lines),
  the common parts
  end up duplicated
  N times,
  and the choice itself
  gets buried —
  all are
  presentational defects.
  In that case
  factor the choice
  back into the single line
  as separate
  optional keywords
  or options,
  and state any
  mutual-exclusion
  or compatibility constraint
  in `Parameters`
  or `Description`
  (controlled-superset form,
  permitted by the rule above).
  Example —
  for `(KW1 | KW2)?`
  in the grammar
  prefer
  `CONSTRUCT [KW1] [KW2] tail ...`
  with a one-line note
  that `KW1`
  and `KW2`
  are mutually exclusive,
  over three lines
  `CONSTRUCT tail ...`
  / `CONSTRUCT KW1 tail ...`
  / `CONSTRUCT KW2 tail ...`
  when `tail`
  is non-trivial.
  After any alternative-line
  expansion,
  re-read the block
  and check
  that every continuation line
  reads
  as attached
  to every alternative,
  not only the last.
- for a repeated element
  where the separator
  is a keyword
  (`IF`, `AND`, `OR`, `XOR`, …)
  or any other non-comma token,
  use the same elision shape
  as for comma-separated lists:
  `expr1 SEP ... SEP exprN`.
  This shows
  N ≥ 1 elements
  with the separator
  appearing between them,
  consistent
  with `expr1, ..., exprN`
  used for commas.
  The assistant MUST NOT
  encode keyword-separated
  repetition
  with constructs like
  `expr1 SEP expr2 [SEP expr3]...`
  or `[SEP exprN]...` —
  the `]...` form
  is unclear about
  what repeats
  and where the ellipsis ends.
- describe
  reader-visible syntax
  and behavior,
  not internal
  grammar rule names
  or parser nonterminals
- keep syntax constructions
  in syntax blocks,
  not in the description,
  and keep one syntax construction
  in one syntax definition;
  the description
  MUST explain
  semantics,
  behavior,
  and usage,
  and MUST NOT
  introduce grammar
  that is absent
  from `Syntax`;
  semantic features,
  special cases,
  and behavior differences
  MUST be described
  in its description,
  not split
  into separate syntax definitions

Coordination with Paradigm:
- describe abstraction logic
  in the related Paradigm article,
  keeping that description
  as independent as possible
  from the current language
  and syntax,
  and describe in the Language article
  only the syntax
  and the specific features
  of writing that abstraction;
  if the Paradigm logic
  is missing or incomplete,
  the assistant MUST first complete
  the Paradigm article
  or explicitly propose completing it

Completeness of research:
- inspect and describe
  the ENTIRE grammar
  of that syntax construction
  in both the platform
  and plugin sources,
  including related rules,
  variants, and clauses,
  even if the request mentions
  only part of them;
  if some of them are not covered
  in the target article,
  the assistant MUST explicitly propose
  describing them as well
- inspect the related Paradigm articles
  and abstractions as well,
  completing them per the
  Coordination with Paradigm rule above


----------------------------------------------------------------

PARADIGM — applies to docs/<lang>/paradigm/

----------------------------------------------------------------

### 2. Paradigm

Paradigm documentation is organized by language elements
and functional blocks.

It MUST:
- describe language elements
- describe platform abstractions
- assume that the reader knows nothing about the language
  or the language / platform abstractions
- not contain syntax-specific language details,
  including the mechanics of expression
  — how an element is
  defined, named,
  looked up / resolved,
  bound to parameters,
  or declared at one site
  vs reused by reference;
  those mechanics
  (and their syntactic forms)
  belong to the Language article
- describe abstractions
  as independently as possible
  from the current language
  and its syntax,
  assuming that the language
  could be different
  and the syntax could change

Paradigm article structure convention:
- a Paradigm article
  SHOULD begin
  with a short definition
  of the abstraction
- it SHOULD organize material
  by semantic aspects
  of the abstraction,
  not by grammar fragments
  or syntax parameters
- if the abstraction
  has corresponding syntax,
  syntax-specific details
  SHOULD stay
  in Language articles,
  and the Paradigm article
  SHOULD contain
  a `Language` section
  linking to the relevant
  Language articles;
  every such link
  MUST be accompanied
  by at least
  a brief descriptive clause
  (a short phrase
  or sentence)
  stating what
  the linked construction
  does
  or which aspect
  of the abstraction
  it covers;
  bare link lists
  such as
  ``[`IS`, `AS`](IS_AS_operators.md).``
  or
  `[Type conversion operator](Type_conversion_operator.md).`
  are forbidden
- a Paradigm article
  SHOULD also include
  an `Examples` section
  with concrete code samples
  that illustrate the abstraction;
  these samples
  necessarily use the corresponding
  language syntax,
  but their role
  is to ground the abstract description
  in concrete code,
  not to serve as a syntax reference —
  comprehensive coverage
  of all syntactic variants
  remains the responsibility
  of the Language article

Documentation-part placement:

The assistant MUST respect
the documentation structure:
it MUST place material
in the part and section
where it belongs,
and MUST NOT move
Language, Paradigm,
Guide, or How-to content
into a different part
without a clear structural reason.

Within the Paradigm part,
placement is decided
by the paradigm hierarchy,
not by the underlying
syntax construction.
The Paradigm tree
has top-level branches
for the
*logical model*
(classes,
properties,
actions,
events,
constraints),
the
*view logic*
(forms,
form structure,
form views,
form operators,
navigator),
the
*physical model*
(tables,
indexes,
materializations),
the
*development process*
(modularity,
extensions,
metaprogramming,
naming,
migration,
internationalization),
the
*execution model*
(sessions,
threads,
control flow,
scheduler),
and the
*management surface*
(launch parameters,
working parameters,
launch events,
security policy,
interpreter,
user interface,
process monitor,
profiler,
journals and logs).
A given aspect of an abstraction
belongs to whichever branch
it conceptually fits,
NOT to the branch
where its underlying
language element
is declared.
In particular,
extension aspects
(class extension,
property extension,
action extension,
form extension)
describe how a developer
composes a project
across modules
and therefore belong
to the development branch
through dedicated articles
(`Class_extension.md`,
`Property_extension.md`,
`Action_extension.md`,
`Form_extension.md`),
not inside
the logical-model
or view-logic articles
of the underlying
class / property /
action / form abstraction;
storage details
(table layout,
index strategy,
materialization)
belong to the
physical-model articles,
not to the
logical-model articles
of the property
or class
they back;
scheduling,
threads,
session lifecycle,
and similar
runtime concerns
belong to the
execution-model articles,
not to the
logical-model articles
of the actions
they run;
administration,
monitoring,
and configuration
concerns
belong to the
management-surface articles,
not to the
abstractions
they observe
or configure.

Class primacy and extension placement:

A base paradigm article
MUST describe
its abstraction
as if extensions
did not exist;
extension-specific mechanisms —
abstract declarations,
polymorphic dispatch
via `CASE` / `MULTI` /
`OVERRIDE` / `EXTEND`,
result or body inheritance
through multiple implementations,
and similar
layered-composition forms —
MUST NOT appear
in the base article,
neither in prose
nor in code examples.
The base article
describes only
the self-contained form
of the abstraction
(a class
with its parents
and static objects;
a property
declared by an expression;
an action
declared with a body;
a form
declared with
its objects and properties).
Extensions are layered
on top
and described
in their own
dedicated articles.

A class
is *primary*
only at the sites
where it is
explicitly set —
class declarations
(parent classes,
abstract flag,
static objects),
new-object creation,
class change,
and the typed-parameter
list at the declaration site
of an action / property /
form / table.
Everywhere else
the class
of a value
or parameter
is *derived* —
inferred
from the surrounding
expression,
from the typed-parameter
mechanism,
or from the signature
of an outer
construction.
A paradigm article
MUST NOT restate
the class
of a derived value
or parameter
as a primary fact
of the abstraction
it describes,
unless the class
itself is needed
to explain
the semantics
of that abstraction
(for example,
a restriction
that the operand
must belong
to a particular
built-in class).
Such derived-class
discussion belongs
either to the canonical
typed-parameter section
(`en/IDs.md#paramid`,
`ru/IDs.md#paramid`)
or to the
Language article
of the construction
that introduces
the parameter,
not to
the paradigm article
of the surrounding
abstraction.

The rule above
targets only
restatements
of derived
operand
or parameter
classes
(typed-parameter
mechanics).
The *result class*
of an operator
or expression
abstraction —
the relation
between the operand
classes
and the produced
value class,
including any
class-derivation
formula —
is itself
a paradigm-level
fact of that
abstraction
and MUST be stated
in the paradigm article.
Even short
result-class
statements
such as
"the result class
matches the input class",
"the result is
a built-in class
of the same family",
or the explicit
class-formula tables
for arithmetic
or string operators
are paradigm-level
semantics
and MUST NOT
be cut
under the
class-primacy rule.
Only the cases
where the article
describes a parameter
or operand
as having a class —
"each parameter
has a fixed class",
"the argument
is of class X" —
without that class
itself being
load-bearing
for the abstraction's
semantics
fall under
the prohibition.

When a sibling article
in another branch
of the paradigm tree
already owns an aspect
(for example,
`Class_extension.md`
as the dedicated article
for the class-extension aspect
of classes),
the current article
MUST only
mention the existence
of that sibling article
and link to it,
without restating its content
even briefly,
since duplication
creates EN/RU lockstep risk
and confuses readers
about which article
is authoritative.
This mention
MUST be made inline,
inside an existing
relevant paragraph
or section,
not as a dedicated
empty stub section
of the form
`### Aspect name` /
`See [aspect](AspectArticle.md).`,
since such a stub
implies that
the current article
has its own content
about that aspect
while it has none
beyond the pointer;
if no existing paragraph
naturally accommodates
the mention,
the link
SHOULD be dropped
from this article
and remain
only in the
canonical sibling article.


Paradigm article writing:

When several
paradigm articles
describe the same
kind of element
(for example,
a property
that receives
a computed result —
the result destination
of an action call,
the destination
written by a
new-object operator,
the destination
written by a
read / input operator),
they MUST use
one consistent
term for it
across all those
articles,
not a different
synonym
in each
(not "target property"
in one
and "receiver"
in another);
pick a single term
and apply it
uniformly.

In Paradigm articles
for operators
that, in a script,
produce a platform element
(an action,
a property,
a class,
a form,
and so on),
the lead-sentence subject
SHOULD be
the operator,
as in
"The *X* operator
creates an action
that ..." /
"The *X* operator
creates a property
that ...",
not the produced element,
as in
"The *X* action
does ..." /
"The *X* property
does ...".
This matches
the established pattern
in existing sibling articles
(for example
`Apply_changes_APPLY.md`,
`Selection_CASE_IF_MULTI_OVERRIDE_EXCLUSIVE.md`,
`Action_operators_paradigm.md`)
and keeps
the lead-subject choice
consistent
across paradigm articles
describing operators
of the same family.


----------------------------------------------------------------

GUIDE — recommendation articles (no dedicated folder)

----------------------------------------------------------------

### 3. Guide

Guide documentation is organized by elements
and functional blocks.

It MUST contain general recommendations:
- what should be done
- what is better not to do

That means recommendations, not errors.
Errors should be described in Language / Paradigm.

Guide documentation includes both:
- recommendations for syntax usage
- recommendations for using abstractions

Guide file naming convention:
- file names contain `Guide`
- most often `Guide` is a prefix

Guide structure SHOULD correspond to the Paradigm structure
whenever possible,
although several different `.md` files
may be combined into one.

Guide article structure convention:
- a Guide article
  SHOULD be organized
  as a set of recommendations
  and anti-recommendations,
  not as a syntax reference
- recommendations
  SHOULD be grouped
  by practical topic
  or aspect of usage
- examples may be used
  to support recommendations,
  but examples
  are secondary
  to the recommendations


----------------------------------------------------------------

HOW-TO — applies to docs/<lang>/how-to/

----------------------------------------------------------------

### 4. How-to

How-to documentation describes usage variants
in how-to mode:
- how to do something
- the code that does it

How-to file naming convention:
- file names contain `How-to`
- most often `How-to` is a prefix

How-to article structure convention:
- a How-to article
  SHOULD be organized
  around concrete tasks
  and their solutions
- when several cases
  are present,
  they SHOULD usually appear
  as separate examples
  with a task
  and a solution
- explanations
  SHOULD stay focused
  on how to achieve
  the target result,
  not on full reference-style
  coverage of the syntax
