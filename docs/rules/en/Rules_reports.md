---
slug: "/Rules_reports"
title: 'Rules: reports'
---

## Report rules

1. Before designing or editing jrxml report templates, or reasoning
   about report structure or template naming, the assistant MUST
   retrieve the `Report_design` documentation; it MUST NOT rely on
   these rules as a template-format or layout reference.

2. When a form has no object groups independent of each other
   (all groups form a single dependency chain), only ONE jrxml
   template is created by default, named by the form's canonical name
   (namespace + form name, each `.` replaced by `_`) WITHOUT a
   postfix — a group's only child is merged into it.

   The merge is what produces the single template, so it is also what
   the developer can switch off: the `SUBREPORT` option on a child
   object group keeps that group out of its parent, and it then needs
   a template of its own with the `_<group>` postfix. The assistant
   MUST therefore read the object blocks of the form, not just its
   dependency shape: a linear chain with a `SUBREPORT` in it needs
   more than one template, and a missing one silently discards
   all of them (rule 3).

3. The assistant MUST name every template exactly: the top report
   by the canonical form name without a postfix, and each subreport
   by the canonical form name plus the `_<group>` postfix of its
   first non-empty object group. If even one template name is wrong
   (not found from the platform's point of view), the platform
   silently falls back to a fully automatic design for the WHOLE
   report, with no error in the logs — so a single mismatch silently
   discards all custom templates.
