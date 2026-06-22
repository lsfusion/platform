SYSTEM PROMPT - lsFusion DOCUMENTATION TASK RULES

SCOPE: describing, editing, restructuring, and clarifying
lsFusion documentation only.

These rules apply only to documentation tasks.

----------------------------------------------------------------

RULE SCOPE

COMMON RULES (this file) apply to every documentation task, of any type.

TYPE-SPECIFIC RULES live in each type folder's own AGENTS.md and apply only
when working on that type (auto-loaded for BOTH language versions, since the
layout is type-first docs/<type>/{en,ru}/ — these are agent rules, not
translated content):
- LANGUAGE — docs/language/AGENTS.md   (syntax-construction articles)
- PARADIGM — docs/paradigm/AGENTS.md   (abstractions and concepts)
- HOW-TO   — docs/how-to/AGENTS.md      (task recipes)
- RULES    — docs/rules/AGENTS.md       (AI task rules / coding recommendations;
  this is the `rules/` folder — what the earlier "Guide" recommendation part
  is realized as)
- BRIEF    — `brief/` is a derived AI capability map distilled from the above;
  no per-type AGENTS.md (a common rule covers it)

Keep a rule that applies to only one type in that type's docs/<type>/AGENTS.md.

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
- the Russian version
  is the source
  of truth
  for terminology;
  the English version
  is its translation.
  When the two locales
  seem to disagree
  on a term,
  the assistant MUST
  defer to the Russian side
  and adjust the English,
  not the reverse.
  When drafting prose
  on the Russian side,
  the assistant MUST NOT
  transliterate
  an English term
  into Russian —
  neither a single word
  (a bare phonetic borrowing
  of an English word)
  nor a compound noun
  into a hyphenated calque —
  when an ordinary Russian word,
  a natural descriptive form,
  or an established
  platform / documentation term
  carries the same meaning;
  it MUST use
  that natural Russian form,
  especially
  in lead occurrences
  and section openings;
  the compact
  or borrowed form
  MAY remain
  only in a few
  less prominent
  spots
  where the natural form
  would be too heavy
  and no established
  Russian equivalent exists,
  or where the borrowing
  is itself
  the established
  platform term
- if a new block
  or section is created,
  the parent
  or higher-level section
  SHOULD contain
  a link
  to that new section
  and a brief description
  of what that section covers
- `platform/docs/sidebars.js`
  is itself part
  of the documentation,
  not a separate
  downstream artifact
  or a later step;
  whenever an article
  is added,
  removed,
  renamed,
  or moved,
  the assistant MUST
  update `platform/docs/sidebars.js`
  in the same change
  so that the new
  or relocated article
  takes its place
  in the documentation hierarchy
  and navigation,
  and MUST NOT
  leave it
  disconnected
  from the navigation
  or defer the wiring
  to a separate step
- the documentation
  SHOULD preserve
  clear links between sections
  so that its hierarchy
  and navigation
  remain explicit
- because the layout
  is type-first
  (`docs/<type>/{en,ru}/`),
  a link
  to an article
  of a different type
  is written
  as a relative path
  that crosses
  only the type level
  (`../<other-type>/<name>.md`),
  and a link
  within the same type
  is written
  as the bare
  article name
  (`<name>.md`);
  such cross-type links
  resolve
  by the target article's
  document id,
  so they are
  correct
  and MUST NOT
  be treated
  as broken
  merely because
  the literal path
  does not exist
  as a sibling
  on disk
- if a section
  becomes too large
  and can be split
  into logically complete blocks,
  the assistant SHOULD do that
  or suggest doing that

The required order is:
1) Language
2) Paradigm
3) How-to

Rules and Brief are derived AI-guidance branches,
distilled from the above
(Rules = the task rules / recommendations in `rules/`;
Brief = the concise capability map in `brief/`).
The earlier "Guide" recommendation part
is realized as the `rules/` folder.

The assistant MUST first look at Language,
then try to understand the abstractions,
then try to compose How-to
and the Rules / Brief guidance.


----------------------------------------------------------------

GENERAL GUIDE

There is also a general guide — the Brief at `brief/{en,ru}/Brief.md` —
distilled from all documentation parts. It is served to the AI assistant by the
MCP `lsfusion_get_guidance` tool (alongside the Rules at `rules/{en,ru}/Rules.md`),
so its key information is always available in context.

Brief, which has no per-type AGENTS.md, references Language / Paradigm
(the primary documentation) and never the reverse — the same cross-reference
direction that `how-to/AGENTS.md` and `rules/AGENTS.md` state for those types.

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

When a higher-level,
overview,
or summary description
restates behavior
that a more detailed
section or article
documents as conditional —
holding only
under a particular mode,
default,
or other qualifier,
or only approximately —
it MUST preserve
that bounding qualifier
or stay general enough
not to assert
the stronger,
unconditional claim;
presenting qualified
or approximate behavior
as an unconditional
guarantee
misstates
the detailed source.

Tabular presentation:

When information
is naturally tabular —
several items
each described
along the same dimensions,
or a correspondence
from one set of values
to another —
it SHOULD be presented
as a table
rather than as
repetitive prose
or a flat list;
a run of parallel
sentences
or list items
that repeats
the same structure
for each entry
SHOULD be converted
into a table,
since a table
makes the shared structure
and the per-item values
easier to scan
and compare.
Prose or a list
SHOULD be kept
only when the items
do not share
a common set
of described dimensions,
or when there is
a single item.

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

The assistant MUST
keep the set of terms
as small as possible:
once a concept
is named
by a given term,
the assistant MUST reuse
that same term
and MUST NOT introduce
a second,
near-synonymous term
for the same
or an overlapping concept
(for example,
not "property"
in one place
and "computation"
for the same notion
in another),
since parallel terms
for one idea
leave the reader
unsure whether
two different things
are meant;
prefer the term
already established
for that concept
in the same
or related articles.

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
The single-term rule above
applies to headings
as much as to running text,
and across heading depth:
a sub-heading,
the parent heading
it sits under,
and the prose
the heading introduces
MUST name a shared concept
with the same term,
not a near-synonym
(for example,
not "выполнение"
in a parent heading
and "исполнение"
in its sub-headings).
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
is preferred;
in particular,
a remark
that only states
the construction
obeys a general,
cross-cutting mechanism
documented elsewhere —
such as
operator priority,
parameter binding,
or `NULL` propagation —
which applies
uniformly
to every such
construction
and carries no
construction-specific
detail
MUST be omitted.

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

Rules in this file
MUST be stated
abstractly:
they MUST NOT include
concrete examples —
specific keywords,
literals,
identifiers,
syntax fragments,
code snippets,
or worked cases
illustrating a rule.
A rule
states
what to do
in general terms;
concrete examples
belong
in the documentation
the rule governs,
never
in the rule itself.

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

Type-specific rules live in each type folder's own AGENTS.md (see RULE
SCOPE at the top): docs/language/, docs/paradigm/, docs/how-to/, and
docs/rules/AGENTS.md.

----------------------------------------------------------------
