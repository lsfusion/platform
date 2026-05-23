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
- GUIDE    — recommendation articles (filename prefix `Guide`); no dedicated
  folder, so the GUIDE section stays in this file under TYPE-SPECIFIC RULES.

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
