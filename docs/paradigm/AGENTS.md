Type-specific documentation rules for docs/paradigm/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the PARADIGM-specific rules live here (auto-loaded for both
docs/paradigm/en and docs/paradigm/ru).


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

Cross-references:
A Paradigm article
is primary documentation
and MUST NOT
reference (link to)
How-to, Rules,
or Brief articles;
cross-references
run only the other way
(How-to / Rules / Brief
link to Paradigm,
never the reverse).
Navigation and index pages
(the documentation
root / overview page
and section-overview pages,
such as `Learn.md`,
whose purpose is
to point readers
to every part)
are exempt
and MAY link
to any part.

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
  that links
  ONLY to the Language
  article(s) of the
  construction(s)
  the abstraction
  is written as
  — the realizing
  operator, statement, or block;
  a concept article
  qualifies only when
  it is itself
  that construction
  (a constant
  is written as a literal,
  a name as an identifier),
  but a generic
  concept or companion
  article MUST NOT
  be linked on top of
  the realizing construction
  (such as the
  expression article
  beside a file operator);
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
- links to Language articles
  MUST appear
  ONLY in the article's
  `Language` (`Язык`) section;
  the definition,
  body, `Examples`,
  and any other section
  of a Paradigm article
  MUST NOT link
  to a Language article.
  Beyond links,
  a Paradigm article
  MUST NOT use,
  anywhere in its prose,
  a Language-specific term —
  one whose meaning
  is owned by
  a Language article
  (for example
  *expression*) —
  whether or not
  it would be linked;
  name the paradigm
  abstraction directly
  instead
  (a filter is a
  *condition* / *property*;
  a computed value
  is a *property*
  or its *value*,
  not an *expression*)
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
- an intermediate
  (umbrella) Paradigm article —
  one whose child
  Paradigm articles
  already cover
  the concrete constructions,
  their syntax,
  and their examples —
  SHOULD NOT carry
  its own `Language`
  or `Examples` section;
  those sections
  belong to the leaf
  articles that own
  the concrete construction,
  and duplicating them
  at the umbrella level
  only restates
  what a child
  already owns

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

In a Paradigm article
(and in other prose
describing the abstraction
of a construction),
use the construction's
human-language name,
not its keyword form;
the keyword form
is appropriate only
inside `Syntax` blocks
and code examples.

Internal optimisations
performed by the platform
(short-cutting
identity compositions,
caching,
shared sub-expression reuse,
constant folding,
and similar runtime
or query-builder optimisations)
MUST NOT be documented
in a Paradigm article
unless the optimisation
is itself an abstraction
the developer must reason about;
typical optimisation tells
such as
"no new property is created" /
"the result is equivalent to X" /
"computed in a single pass"
add no abstraction-level information
and MUST be omitted.

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
