# Licensing

This repository contains material under three different licenses.

## Source code — LGPL-3.0

All source code of the lsFusion platform is licensed under the GNU Lesser
General Public License v3.0. See [`LICENSE`](LICENSE) for the full text.

SPDX identifier: `LGPL-3.0-only`

## Documentation — CC BY 4.0

Documentation content — the Markdown articles and images under `docs/*/en/`
and `docs/*/ru/` — is licensed under the Creative Commons Attribution 4.0
International license, except for the code examples embedded in it, which are
covered by the next section, and unless a particular file states otherwise.

Full text: [`LICENSES/CC-BY-4.0.txt`](LICENSES/CC-BY-4.0.txt)

SPDX identifier: `CC-BY-4.0`

You may copy, redistribute, translate, and adapt this documentation for any
purpose, including commercially, as long as you give appropriate credit.
The suggested form of credit is:

> Based on the [lsFusion platform documentation](https://docs.lsfusion.org)
> by lsFusion Foundation, licensed under
> [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/).

Everything else under `docs/` is not documentation content and stays under
LGPL-3.0 with the rest of the repository: the navigation source `sidebars.js`
and the agent instruction files (`AGENTS.md`, `CLAUDE.md`, `GEMINI.md`).

## Code examples in documentation — CC0 1.0

Code examples embedded in the documentation are dedicated to the public
domain under CC0 1.0 Universal, unless a particular example states otherwise.

Full text: [`LICENSES/CC0-1.0.txt`](LICENSES/CC0-1.0.txt)

SPDX identifier: `CC0-1.0`

You may use these examples in your own projects, including commercial and
closed-source ones, without attribution or any other condition. This carve-out
exists because the examples are written to be copied into applications, where
the attribution requirement of CC BY 4.0 would be impractical to honor.

## Machine-readable summary

[`REUSE.toml`](REUSE.toml) maps the paths above to their SPDX identifiers in
the [REUSE](https://reuse.software/) format, so that a scanner does not have
to read this document to see that the documentation is not under LGPL-3.0.

It covers the split between the source code and the documentation only. REUSE
can annotate a snippet inside a file, but only through tags carried in
comments, and the documentation is rendered as MDX, which does not tolerate
HTML comments — so the CC0 dedication for the code examples is stated in this
document alone.

The repository uses the REUSE file format without targeting full REUSE
compliance: `LICENSES/` holds the license texts this document refers to,
which is not the exact set the specification prescribes, so `reuse lint` is
not expected to pass.

## Contributions

Unless you state otherwise, a contribution is offered under the same license
as the material it changes: CC BY 4.0 for documentation content, CC0 1.0 for
code examples inside it, and LGPL-3.0 for everything else. If you cannot
contribute a change on those terms, say so in the pull request.

## Third-party material

Material in `docs/` that originates elsewhere — screenshots of other products,
borrowed text or diagrams — carries its own license notice next to it and is
not covered by the grants above.

## Copyright

Copyright (c) 2013-2026 lsFusion Foundation.
