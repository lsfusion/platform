# AGENTS.md — lsFusion platform web-client

Module-specific rules for `web-client/`. The cross-cutting process, commit, issue,
verification, rebuild, and documentation rules in `../AGENTS.md` still apply.

## The JS controllers and the page's WebMCP tool

The page's own WebMCP tool, `lsfusion_client` (`WebMCP.java`), gives the browser's AI agent the methods of a form's and of the navigator's JS controller by a catalog kept by hand: `getFormMethods` for a form's controller (`GReactFormData.addFormMethods`, `GFormController.addFormVerbs`), `getNavigatorMethods` for the navigator's (`GNavigatorActionDispatcher.initController`). An entry names the method's arguments (`takes`) and has the sentence the tool's description says of the method (`does`). The calls of the logic that both controllers have (`GController.initController`) are the tools `lsfusion_eval` and `lsfusion_exec`; the members of a React view's own controller — a group's, a property's, a container's — are not the tool's.

A change of those methods, or of the code under them that decides what one takes, does or answers, goes with the catalog, in the same commit:

- **A method added, removed or renamed.** A new method goes into the catalog, or — where the agent cannot call it — into the list of those the tool leaves out in `UiWebMCPIT.theClientToolsMethodsAreTheControllers`, with the reason in the comment over `getFormMethods`. The test fails until the method is in one of the two.
- **What a method takes.** That test compares names only. An argument added or removed, a new field of an options object, another way to name a row, a column or a property is checked by nothing: read the entry — its `takes`, its text — against the method.
- **What a method does and answers.** The entry's text says it, with what the method refuses and what it lists — the `compare` of a filter's condition, the modes of a selection, of a lookup. A value added to such a list, a refusal added or lifted, another shape of the answer leaves the text false with every test green.
- **What a call names has to be on the page.** The agent has no sources: it names a group, a property, a row, an element of the navigator by what the DOM shows — `lsfusion-container`, `lsfusion-property`, `lsfusion-group` and `lsfusion-row`, `lsfusion-form` and its neighbours. An argument that names something new needs an attribute that shows it on what the platform draws, and the tool's description says where it is; an attribute renamed is renamed there.
- **A row** reaches a method as its key, which `WebMCP.readRow` reads. A method that takes the row in no place of its own — among its options, beside a value — needs a `call` in its entry.

The tool's description says only what a method does, takes and answers. What the tool is for is in `docs/paradigm/{en,ru}/MCP_server.md`.
