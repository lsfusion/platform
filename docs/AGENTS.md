SYSTEM PROMPT - lsFusion DOCUMENTATION TASK RULES

SCOPE: describing, editing, restructuring, and clarifying
lsFusion documentation only.

These rules apply only to documentation tasks.

----------------------------------------------------------------

DOCUMENTATION STRUCTURE

Documentation is divided into parts.

These parts define:
1) the order in which documentation should be written
2) the order in which language capabilities should be understood

The documentation structure / hierarchy
for navigation is defined in `docusaurus/sidebars.js`.
The assistant MUST treat that file
as the source of truth for section placement
and parent-child relationships in the docs,
while treating the `docusaurus` subproject
as a derived copy of the documentation
from `platform/docs`.
The assistant MUST NOT edit files
inside `docusaurus`
except `docusaurus/sidebars.js`;
all documentation content changes
MUST be made in `platform/docs`.

The required order is:
1) Language
2) Paradigm
3) Guide
4) How-to

The assistant MUST first look at Language,
then try to understand the abstractions,
then try to compose Guide and How-to.

### 1. Language

Language documentation is organized by syntax constructions.

It MUST contain:
- the language syntax itself
- a description of the syntax
- a basic explanation
  assuming that the reader already more or less understands
  the paradigm

Language file naming convention:
- file names usually contain the syntax token in uppercase

Language syntax notation convention:
- the assistant MUST preserve
  the current syntax-description style
  used in the documentation
  (for example, optional fragments
  are written in square brackets `[...]`)
- when extending or finishing
  syntax documentation,
  the assistant MUST search
  for analogous syntax constructions
  in other articles;
  equivalent fragments and behavior
  MUST be documented consistently:
  either described in the same way,
  or linked to an existing section,
  or extracted into a separate section
  and referenced from the relevant articles
- the assistant MUST make sure
  that the syntax block
  reflects the full real grammar,
  including all relevant variants
- each syntax block
  MUST contain one
  syntax construction;
  named subrules
  and syntax fragments
  MUST be explained in prose
  and, if separate,
  given in their own
  syntax blocks;
  alternative variants
  MUST be shown
  with explicit alternation
  or described in prose,
  with common parts
  factored out
- the assistant MUST NOT move
  syntax constructions
  into the description;
  the description MUST explain
  semantics, behavior, and usage
- the assistant MUST NOT split
  one syntax construction
  into separate syntax definitions
  for semantic differences;
  semantic features,
  special cases,
  and behavior differences
  MUST be described
  in its description

### 2. Paradigm

Paradigm documentation is organized by language elements
and functional blocks.

It MUST:
- describe language elements
- describe platform abstractions
- assume that the reader knows nothing about the language
  or the language / platform abstractions
- not contain syntax-specific language details
- describe abstractions
  as independently as possible
  from the current language
  and its syntax,
  assuming that the language
  could be different
  and the syntax could change

### 3. Guide

Guide documentation is organized by elements
and functional blocks.

It MUST contain general recommendations:
- what should be done
- what is better not to do

That means recommendations, not errors.
Errors should be described in Language / Paradigm.

Guide documentation includes both:
- recommendations for syntax usage
- recommendations for using abstractions

Guide file naming convention:
- file names contain `Guide`
- most often `Guide` is a prefix

Guide structure SHOULD correspond to the Paradigm structure
whenever possible,
although several different `.md` files
may be combined into one.

### 4. How-to

How-to documentation describes usage variants
in how-to mode:
- how to do something
- the code that does it

How-to file naming convention:
- file names contain `How-to`
- most often `How-to` is a prefix

----------------------------------------------------------------

GENERAL GUIDE

There is also a general guide: `mcp/brief.md`.

It is assumed to exist.
It should contain important / key information
from all documentation parts,
and this information should always be available
in the context for the AI assistant.

----------------------------------------------------------------

RULES FOR UNDERSTANDING LANGUAGE CAPABILITIES

1. SOURCE PRIORITY
   The assistant MUST try to inspect functionality
   and implementation details in both:
   - the platform project
   - the plugin project

   The platform implementation has higher priority.

2. REQUIRED UNDERSTANDING ORDER
   The assistant MUST first try to understand
   which syntax constructions are needed.

   After that it MUST:
   - inspect the platform / plugin source code
   - inspect examples in existing lsFusion code
   - try to find relevant community and history information
     (tutorials, articles, discussions,
     GitHub issues, commits, and GitHub issues
     referenced from those commits)
     using the available tools or in
     `rag-fill/src/main/resources/docs`

3. PLATFORM SOURCE REVIEW
   In the platform project, the preferred direction is:
   `LsfLogics.g` ->
   `ScriptingLogicsModule` ->
   `LogicsModule` ->
   the classes they use and the semantics of those classes

   In this repository the corresponding paths are:
   - `platform/server/src/main/antlr3/lsfusion/server/language/LsfLogics.g`
   - `platform/server/src/main/java/lsfusion/server/language/ScriptingLogicsModule.java`
   - `platform/server/src/main/java/lsfusion/server/logics/LogicsModule.java`

4. PLUGIN SOURCE REVIEW
   In the plugin project, the preferred direction is:
   `LSF.bnf` ->
   mixin / implements classes ->
   methods in `LSFPsiImplUtil` ->
   the classes they use and the semantics of those classes

   In this repository the corresponding paths are:
   - `plugin-idea/src/com/lsfusion/lang/LSF.bnf`
   - `plugin-idea/src/com/lsfusion/lang/psi/LSFPsiImplUtil.java`

5. EXAMPLES IN EXISTING LSF CODE
   The assistant MUST search for examples
   in existing lsFusion code in order to understand:
   - how it is used
   - how it can be used

6. ATTENTION TO IMPLEMENTATION DETAILS
   The assistant MUST inspect the source code
   as carefully as possible
   so as not to miss non-obvious behavior.

7. GUIDE / HOW-TO WRITING
   When writing Guide / How-to,
   the assistant MUST also use examples
   from existing code.

8. COMMUNITY SOURCES
   The assistant SHOULD also look for
   relevant community and history materials
   (tutorials, articles, discussions,
   GitHub issues, commits, and GitHub issues
   referenced from those commits)
   using the available tools.

   In this repository it SHOULD also inspect:
   - `rag-fill/src/main/resources/docs`

----------------------------------------------------------------

RULES FOR COMPLETING LANGUAGE SYNTAX ARTICLES

When the request is to complete or extend
an article for a language syntax construction,
the assistant MUST:
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
  and if something important
  is missing there,
  complete those articles too
  or explicitly propose completing them

----------------------------------------------------------------

RULES FOR DOCUMENTING LSFUSION

This documentation will be used by a human
and by an AI assistant
to write code, explain code, and similar tasks.

The documentation MUST be written so that
the reader can understand exactly:
- how the written code will work
- what can / should be done
- what cannot / should not be done

If there is a potential question such as:
- how does this work
- what will happen in this case
- can this be done this way

then it SHOULD be described / explained in the documentation.

At the same time,
the description should be concise,
but also complete.

The assistant SHOULD try to describe
all significant details of the language
and usage variants.

When completing or extending
existing documentation,
the assistant SHOULD preserve
as much of the current text
as possible
and prefer extending it
over rewriting it,
unless the current text
is incorrect, unclear,
or conflicts
with the required structure.

The assistant SHOULD try to preserve
the existing style, size, and level of detail.

When adding code examples,
the assistant MUST validate them in the IDE
if such access or tools are available,
for example by creating the corresponding code fragment.
If IDE validation is not available,
the assistant MUST use a syntax-checking tool,
if such a tool exists.

If a new block or section is created,
the assistant SHOULD make sure that
the parent or higher-level section
contains:
- a link to that new section
- a brief description of what that section covers

In that case, the assistant SHOULD also
update `docusaurus/sidebars.js`
so that the new block or section
is included in the documentation hierarchy
and navigation.

The documentation SHOULD preserve
clear links between sections
so that its hierarchy and navigation
remain explicit.

If a section becomes too large
and can be split into logically complete blocks,
the assistant SHOULD do that
or suggest doing that.

----------------------------------------------------------------

RULES FOR ERRORS, PROHIBITIONS, AND RECOMMENDATIONS

When describing or implementing restrictions,
the assistant MUST reason in the following order,
from the strictest level to the least strict level:

1. plugin errors / warnings
2. platform (application server) errors / warnings at startup
3. prohibition / recommendation in the general guide
4. prohibition / recommendation in the guide
   for the corresponding element
5. warning in the Language or Paradigm documentation
   for the corresponding element

The assistant SHOULD try to add support
at all applicable levels.

The assistant SHOULD try to add the restriction
at the strictest level possible.

If that is not possible,
the assistant SHOULD explicitly propose doing so
in the response to the requester.

To check or assess the available error level
in the platform and plugin,
the assistant MUST inspect the source code
according to the source-review rules above.
