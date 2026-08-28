Type-specific documentation rules for docs/rules/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the RULES-specific rules live here (auto-loaded for both
docs/rules/en and docs/rules/ru).

----------------------------------------------------------------

RULES — applies to docs/rules/<lang>/

----------------------------------------------------------------

The `rules/` folder holds the lsFusion task rules / coding recommendations.
This is the realization of the earlier "Guide" recommendation part: it states
what should and should not be done when writing `.lsf`, distilled from
Language / Paradigm / How-to.

The folder has two kinds of article, and they have different budgets:
- `Rules.md` is the CORE. It is served whole by `lsfusion_get_guidance`, so
  every byte of it is spent on every task. It carries only what applies
  regardless of area, and it MUST keep the requirement to fetch the rules of
  the area being worked in. It deliberately does NOT list the areas that have
  an article: the lookup is mandatory whether or not an area is on a list, and
  a list is a second catalogue to keep in step for nothing.
- `Rules_<area>.md` are the per-area articles. They reach the assistant only
  through `lsfusion_retrieve_docs(type='rules', ...)`, never automatically.

It MUST contain general recommendations:
- what should be done
- what is better not to do

That means recommendations, not errors.
Errors should be described in Language / Paradigm.

Rules cover both:
- recommendations for syntax usage
- recommendations for using abstractions

Rules article structure convention:
- a Rules article
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
- keep the CORE
  deliberately compact;
  it is primed into
  the assistant's context
  on every task,
  so noise there hurts
  more than missing edges
- a per-area article
  is retrieved, not primed:
  it may be longer,
  but it MUST start
  with a `##` heading —
  no preamble
  and no `Scope` section.
  Text before the first `##`
  becomes a chunk of its own
  in the retrieval index,
  and a scope paragraph
  repeated across articles
  becomes near-duplicate noise
- every `##`
  is a chunk boundary
  and a search signal;
  name it
  after its subject,
  not `General`

Cross-references:
Rules articles
reference Language
and Paradigm
(the primary documentation)
as needed;
the reverse is not allowed —
a Language / Paradigm article
never links back
to a Rules article.

Examples and grounding:
When writing Rules,
the assistant MUST
use examples
from existing lsFusion code.
Usage variants
documented here
MUST be grounded
in the grammar,
platform / plugin code,
existing examples,
or existing documentation —
not speculative.
