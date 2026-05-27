Type-specific documentation rules for docs/language/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the LANGUAGE-specific rules live here (auto-loaded for both
docs/language/en and docs/language/ru).


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

Cross-references:
A Language article
is primary documentation
and MUST NOT
reference (link to)
How-to, Rules,
or Brief articles;
cross-references
run only the other way
(How-to / Rules / Brief
link to Language,
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

Keyword form and grounding:
A Language article
describes the syntax
of the keyword
and naturally takes
the keyword form
as its subject —
not the human-language name
that abstraction prose uses.
Syntax documentation
MUST describe
what can be written
and what it means,
not speculative usage;
pure usage variants
belong to Rules / How-to.

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
  more generally,
  every distinct
  reader-visible
  parameter
  or independent option
  MUST be
  its own
  `Parameters` item,
  and several
  distinct parameters
  MUST NOT
  be merged
  under one
  comma-joined header
  (for instance,
  a single-value form
  and the key
  and the value
  of a map form
  are separate items);
  the keyword values
  of one choice,
  however,
  stay together
  in that choice's item;
  the assistant MUST NOT
  spell out
  trivial consequences
  of the syntax notation
  that already follow
  directly from the syntax,
  such as explaining
  how an empty list
  is written,
  or calling
  a parameter
  "optional"
  (or its translation)
  in prose
  when the syntax
  already marks it
  optional
  with `[...]`;
  however,
  it MUST still say
  whether the list
  may be empty
  or not,
  and what
  the absence
  of an optional element
  means
  when that
  is not obvious;
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
