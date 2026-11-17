---
slug: "/Brief_via_dev"
title: 'Brief: development'
---

## Source development

Development is work on an application's `.lsf` source in its repository, through the skills plugin or a direct connection to the documentation server. Code is checked by IDE diagnostics without starting the application, by a dry run or a server start, and, for behavior, by execution in a test application. A change to `.lsf` takes a restart of the application server; the `lsfusion-dev` skill says which other changes development mode picks up without one.

The `lsfusion-ai-skills` skills, where installed, hold the procedures for local setup, server start, calls on a running server and deployment. The skills plugin connects as `dev` (`?via=dev&with=skills`); `with=skills` does not change the answer.
