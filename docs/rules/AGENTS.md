Type-specific documentation rules for docs/rules/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the RULES-specific rules live here (auto-loaded for both
docs/rules/en and docs/rules/ru).

----------------------------------------------------------------

RULES — applies to docs/<lang>/rules/

----------------------------------------------------------------

The `rules/` folder holds the lsFusion task rules / coding recommendations
(`Rules.md` — the AI system-prompt task rules). This is the realization of the
earlier "Guide" recommendation part: it states what should and should not be
done when writing `.lsf`, distilled from Language / Paradigm / How-to.

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
- keep the guidance
  deliberately compact;
  it is primed into
  the assistant's context,
  so noise hurts
  more than missing edges
