#!/usr/bin/env python3
"""Validate that every image reference, asset import, and relative doc link in
the type-first docs (docs/<type>/{en,ru}) resolves in the FLATTENED Docusaurus
build layout produced by the deployDocs job.

Why this and not just `yarn build`: docusaurus.config uses onBrokenLinks /
onBrokenMarkdownImages = 'warn', so a broken image or link does not fail the
build — it 404s at runtime (this is exactly how the Backup_restore images
broke while deployDocs #208 still reported SUCCESS). This check fails the CI
instead, before deploy.

Build-layout model (mirrors deployDocumentation.copyDocPages, "next"):
  source  docs/<type>/<lang>/<name>.md   ->  build  current/<type>/<name>.md
  source  docs/images/<lang>/<file>      ->  build  current/images/<file>
A reference is resolved relative to the doc's build dir `current/<type>/`.
Catches:
  - raw markdown images that escape the build root, e.g. ../../images/<lang>/x.png  (Backup_restore)
  - image refs / asset imports pointing at a missing file, e.g. ./images/x.svg      (#195)
  - relative doc links to a non-existent page
"""
from __future__ import annotations

import os
import posixpath
import re
import sys

ROOT = os.path.normpath(os.path.join(os.path.dirname(__file__), "..", ".."))
DOCS = os.path.join(ROOT, "docs")
TYPES = {"language", "paradigm", "how-to", "brief", "rules"}
LANGS = ("en", "ru")
IMG_EXT = (".png", ".jpg", ".jpeg", ".gif", ".svg", ".webp", ".ico")

RE_MD_IMG = re.compile(r"!\[[^\]]*\]\(\s*([^)\s]+)")
RE_HTML_IMG = re.compile(r"<img[^>]*\bsrc=[\"']?([^\"'\s>]+)")
RE_MDX_IMPORT = re.compile(r"""\bfrom\s+['"](\.[^'"]+)['"]""")  # relative imports only
RE_MD_LINK = re.compile(r"(?<!\!)\[[^\]]*\]\(\s*([^)\s]+\.md[^)\s]*)")


def _strip(ref: str) -> str:
    return ref.split("#", 1)[0].split("?", 1)[0]


def _external(ref: str) -> bool:
    return ref.startswith(("http://", "https://", "mailto:", "tel:", "/", "data:", "#"))


def _resolve(doc_type: str, lang: str, ref: str) -> tuple[bool, str]:
    r = _strip(ref)
    if not r:
        return True, ""
    norm = posixpath.normpath(posixpath.join("current", doc_type, r))
    if norm.startswith("current/images/"):
        rel = norm[len("current/images/"):]
        p = os.path.join(DOCS, "images", lang, rel)
        return os.path.isfile(p), f"image not found: docs/images/{lang}/{rel}"
    if norm.startswith("current/") and norm.endswith(".md"):
        parts = norm.split("/")
        if len(parts) >= 3 and parts[1] in TYPES:
            p = os.path.join(DOCS, parts[1], lang, "/".join(parts[2:]))
            return os.path.isfile(p), f"doc link not found: docs/{parts[1]}/{lang}/{'/'.join(parts[2:])}"
    return False, f"ref escapes the build root current/: {ref!r} -> {norm}"


def main() -> int:
    broken: list[str] = []
    for lang in LANGS:
        for t in sorted(TYPES):
            d = os.path.join(DOCS, t, lang)
            if not os.path.isdir(d):
                continue
            for fn in sorted(os.listdir(d)):
                if not fn.endswith(".md"):
                    continue
                text = open(os.path.join(d, fn), encoding="utf-8").read()
                refs: list[str] = []
                for rx in (RE_MD_IMG, RE_HTML_IMG, RE_MDX_IMPORT, RE_MD_LINK):
                    refs += rx.findall(text)
                for ref in refs:
                    if _external(ref):
                        continue
                    base = _strip(ref)
                    if not (base.endswith(".md") or base.endswith(IMG_EXT) or "images/" in base):
                        continue
                    ok, why = _resolve(t, lang, ref)
                    if not ok:
                        broken.append(f"{t}/{lang}/{fn}: {ref}  ({why})")
    if broken:
        print(f"BROKEN DOC REFERENCES ({len(broken)}):")
        for b in broken:
            print("  -", b)
        return 1
    print("docs reference check: all image/asset/doc-link references resolve.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
