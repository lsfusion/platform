Type-specific documentation rules for docs/brief/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the BRIEF-specific rules live here (auto-loaded for both
docs/brief/en and docs/brief/ru).

----------------------------------------------------------------

BRIEF — applies to docs/brief/<lang>/

----------------------------------------------------------------

The `brief/` folder holds the capability map an assistant is given before it
knows anything about the platform. It answers "what exists here and what is
it for". Area articles hand off to Paradigm for "how it works" and to
Language for exact syntax. Access-kind articles explain what is reachable,
which mechanisms serve a task and how changes take effect through that access.

The folder is NOT searched. Nothing here is chunked or indexed: an article is
named and delivered whole by `lsfusion_get_guidance`.

Three kinds of article, with different budgets:
- `Brief.md` is the TOP, served on every task by the zero-argument call, so
  every byte of it is spent on every task. It carries the map of the core
  elements and the maps of the area and access-kind articles, and NOTHING a
  reader can be sent to fetch instead. That second half
  is the one that erodes: it is always tempting to explain an element a little
  more where it is already named, and the result is a top article that restates
  its own area articles at half the length — the same analogy, the same
  signature, less of the substance. If a sentence would also belong in the area
  article, it belongs there and not here.

  What the top MUST keep is the vocabulary the map routes on: an assistant
  fetches an area by NAME, and it cannot ask for a name it has never read, nor
  recognize that `WHEN` is what `logic` covers unless the top said so. One line
  per element, enough to route — not enough to substitute.
  Every article MUST appear by its short name in the first column of a map.
  Keep the access-kind section to its table and two lines of explanation.
- `Brief_<name>.md` are the six area articles: `logic`, `view`, `custom`,
  `physical`, `integration`, `operation`. Operation maps work in a running system for
  users and administrators; declarations of forms, designs and the navigator
  stay in `view`. Operation is an ordinary area, read by need. Area articles
  reach the assistant only when it asks for one by name, and must stay inside
  the tool-result envelope (under 40 KB against a measured 50 KB ceiling).
- `Brief_via_<name>.md` carry knowledge for the access kinds, matching the
  rules branch's articles and inheritance. The base kinds are `dev` and `app`;
  `idedev` extends `dev`, and `webapp` extends `app`. The skills plugin
  connects as `dev`; the `with=skills` parameter it also sends is not read by
  the server yet. The first answer contains the Brief top, the brief chain
  ancestor first, the Rules top and the rules chain ancestor first.
  Every article name in a chain MUST exist in both branches. The legacy
  server kind receives `app` and `webapp`.
  These briefs describe available tools, context, effects and how mechanisms
  work. Obligations and recommendations belong in the matching rules, which
  may include topical introductions, factual reasons and examples alongside
  them, following the same convention as the area articles.
  Do not repeat the tools' own descriptions or retell Paradigm: explain
  task-to-mechanism choices and link to the primary documentation.
  Keep each access-kind article at about 6 000 characters or less (the
  server serves the English text, and each article adds to the first
  answer); measure the combined first answer after changing it. An extending kind's
  brief MUST open by naming and linking its ancestor.

Writing a brief article:
- an analogy
  to a familiar construction
  from another system
  is allowed here
  and nowhere else
  (see ../AGENTS.md);
  it comes AFTER
  the literal description,
  never instead of it
- do NOT retell Paradigm.
  Brief says
  what the thing is
  and when it is reached for;
  Paradigm explains
  how it behaves.
  Link, do not paraphrase
- name the operators
  and keywords literally:
  they are what
  a reader searches by
- an area or access-kind article
  MUST start
  with a `##` heading —
  no preamble
  and no `Scope` section.
  Text before the first `##`
  becomes a chunk of its own
  in the retrieval index
- every `##`
  is a chunk boundary
  and a search signal;
  name it
  after its subject,
  not `General`.
  Aim for 100-400 tokens
  per section,
  measured in BOTH locales —
  Russian runs
  about 1.4x
  the English token count,
  so a section
  that fits in English
  can still be too long.
  A catalogue section
  may go over;
  the hard limit
  is the chunker's,
  far above this
- do NOT use `###`
  in these articles:
  it splits the section
  and changes
  what a search returns
- do NOT put
  an introductory sentence
  under an umbrella `##`
  that only holds subsections;
  it becomes
  a weak chunk of its own
