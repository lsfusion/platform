Type-specific documentation rules for docs/how-to/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the HOW-TO-specific rules live here (auto-loaded for both
docs/how-to/en and docs/how-to/ru).


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

Cross-references:
How-to articles
reference Language
and Paradigm
(the primary documentation)
as needed;
the reverse is not allowed —
a Language / Paradigm article
never links back
to a How-to article.

Examples and grounding:
When writing How-to,
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
