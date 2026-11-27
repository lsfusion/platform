Type-specific documentation rules for docs/rules/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the RULES-specific rules live here (auto-loaded for both
docs/rules/en and docs/rules/ru).

----------------------------------------------------------------

RULES — applies to docs/rules/<lang>/

----------------------------------------------------------------

The `rules/` folder holds the lsFusion task rules / coding recommendations.
This is the realization of the earlier "Guide" recommendation part: it states
what should and should not be done when writing `.lsf` and working through
the available access paths, distilled from Language / Paradigm / How-to.

The folder is NOT searched. Nothing here is chunked or indexed: an article is
named and delivered whole by `lsfusion_get_guidance`, which is what makes
completeness decidable — a top-N retrieval cannot report the chunk it withheld.

Three kinds of article, with different budgets:
- `Rules.md` is the CORE, served on every task by the zero-argument call, so
  every byte of it is spent on every task. It carries what applies regardless
  of area, plus the MAP of the branch: an area table and a separate access-kind
  table, one row per article with the trigger
  that makes reading it mandatory. The map is load-bearing, not a courtesy —
  it is the only thing that tells the assistant an article exists, and the
  session that motivated this structure went wrong precisely because nothing
  did. Changing the set of articles means changing the map in the same commit.
- `Rules_<area>.md` are the six area articles: `properties`, `changes`, `view`, `custom`,
  `physical`, `integration`. They reach the assistant only when it asks for one by name.
  Each must stay inside the tool-result envelope of the harnesses — under
  40 KB, against a measured 50 KB ceiling — because an article that arrives
  truncated defeats the whole point. Splitting one is a structural change: it
  needs a new row in the map and a trigger of its own.
- `Rules_via_<name>.md` are articles for four access kinds in two hierarchies.
  The base kinds are `dev` (source development through skills or a direct
  connection) and `app` (work in a running application). `idedev` extends `dev`
  with IntelliJ tools; `webapp` extends `app` with the page's tools. The server
  sends the Brief top, the brief chain ancestor first, the Rules top and the
  rules chain ancestor first in the zero-argument answer. Every article name
  in a chain MUST exist in both branches. The skills plugin connects as `dev`;
  the `with=skills` parameter it also sends is not read by the server yet.
  The legacy server kind receives `app` and `webapp`.
  Both branches are also read by name, ancestor included, before using
  another kind's tools, following the cross-read rule
  and separate table in `Rules.md`. Only whole fenced articles count as read;
  already held articles are not read again. The map MUST list every rules
  article by its short name in the first column.
  Inherited rules MUST retain their scope: skill procedures apply where the
  skills are available, `/mcp` behavior to calls over `/mcp`, and page rules
  when using the page's tools. Skill requirements apply whichever connection
  delivered the guidance. Keep each article at about 6 000 characters or
  less (the server serves the English text, and each article adds to the
  first answer); measure the combined first answer after changing it.

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
- in an area article,
  every `##`
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
- an access-kind article
  MUST start with a practical `##` topic (it has no contents list), and
  every rule in it MUST state its strength (MUST / MUST NOT / SHOULD /
  SHOULD NOT), as the rules of the area articles do. Short topical lead
  lines and links, factual reasons and examples may accompany the rules.
  The matching brief describes available tools, context and how mechanisms
  work; keep the fact a rule rests on beside it as its reason rather than
  forcing the reader to fetch it separately.
  Do not repeat full tool descriptions or area rules.

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
