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

The folder is NOT searched. Nothing here is chunked or indexed: an article is
named and delivered whole by `lsfusion_get_guidance`, which is what makes
completeness decidable — a top-N retrieval cannot report the chunk it withheld.

Two kinds of article, with different budgets:
- `Rules.md` is the CORE, served on every task by the zero-argument call, so
  every byte of it is spent on every task. It carries what applies regardless
  of area, plus the MAP of the branch: one row per article, with the trigger
  that makes reading it mandatory. The map is load-bearing, not a courtesy —
  it is the only thing that tells the assistant an article exists, and the
  session that motivated this structure went wrong precisely because nothing
  did. Changing the set of articles means changing the map in the same commit.
- `Rules_<name>.md` are the four area articles: `logic`, `view`, `physical`,
  `integration`. They reach the assistant only when it asks for one by name.
  Each must stay inside the tool-result envelope of the harnesses — under
  40 KB, against a measured 50 KB ceiling — because an article that arrives
  truncated defeats the whole point. Splitting one is a structural change: it
  needs a new row in the map and a trigger of its own.

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
- an area article
  is fetched, not primed:
  it may be longer,
  but it MUST open
  with its contents list
  and then `##` sections —
  no preamble
  and no `Scope` section,
  which only repeat
  across articles
- every `##`
  is one former area
  and an anchor target;
  name it
  after its subject,
  not `General`.
  Cross-references
  from other articles
  land on these anchors,
  so renaming one
  means fixing them

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
