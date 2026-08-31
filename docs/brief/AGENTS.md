Type-specific documentation rules for docs/brief/.
The COMMON rules in ../AGENTS.md apply to every documentation task;
only the BRIEF-specific rules live here (auto-loaded for both
docs/brief/en and docs/brief/ru).

----------------------------------------------------------------

BRIEF — applies to docs/brief/<lang>/

----------------------------------------------------------------

The `brief/` folder holds the capability map an assistant is given before it
knows anything about the platform. It answers "what exists here and what is
it for", and hands off to Paradigm for "how it works" and to Language for
the exact syntax.

The folder is NOT searched. Nothing here is chunked or indexed: an article is
named and delivered whole by `lsfusion_get_guidance`.

Two kinds of article, with different budgets:
- `Brief.md` is the TOP, served on every task by the zero-argument call, so
  every byte of it is spent on every task. It carries the map of the core
  elements and NOTHING a reader can be sent to fetch instead. That second half
  is the one that erodes: it is always tempting to explain an element a little
  more where it is already named, and the result is a top article that restates
  its own area articles at half the length — the same analogy, the same
  signature, less of the substance. If a sentence would also belong in the area
  article, it belongs there and not here.

  What the top MUST keep is the vocabulary the map routes on: an assistant
  fetches an area by NAME, and it cannot ask for a name it has never read, nor
  recognize that `WHEN` is what `logic` covers unless the top said so. One line
  per element, enough to route — not enough to substitute.
- `Brief_<name>.md` are the four area articles: `logic`, `view`, `physical`,
  `integration`, the same names the `rules` branch uses. They reach the
  assistant only when it asks for one by name, and must stay inside the
  tool-result envelope (under 40 KB against a measured 50 KB ceiling).

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
- a per-area article
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
