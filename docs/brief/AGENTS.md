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

The folder has two kinds of article, and they have different budgets:
- `Brief.md` is the TOP. It is served whole by `lsfusion_get_guidance`, so
  every byte of it is spent on every task. It carries the map of the core
  elements and nothing that a reader can be sent to fetch instead.

  The core map is what tells an assistant a concept exists at all, so an
  area that gets its own article MUST also be named and explained in one
  line there — the assistant retrieves an area by NAME, and a name it has
  never read is a name it will not ask for. There is deliberately no list
  of the article files: it was a second catalogue to keep in step, and the
  map already names every area worth fetching. No links either — the
  consumer reads this over MCP and has no way to follow one.
- `Brief_<area>.md` are the per-area maps. They reach the assistant only
  through `lsfusion_retrieve_docs(type='brief', ...)`, never automatically.

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
