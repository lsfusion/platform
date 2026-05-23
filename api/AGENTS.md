# AGENTS.md — lsFusion platform api

Module-specific rules for `api/`. The cross-cutting process, commit, issue,
verification, rebuild, and risk rules in `../AGENTS.md` still apply.

## Bumping the API version

`BaseUtils.getApiVersion()` in `api/src/main/java/lsfusion/base/BaseUtils.java` gates the RMI handshake between BLB and every RMI client (desktop JVM, web-client servlet JVM, external Java integrations). `BaseUtils.checkClientVersion()` compares `ServerSettings.apiVersion` against client-supplied `NavigatorInfo.apiVersion` via `.equals()` — mismatch hard-rejects the session.

The gate protects the **RMI wire only**. GWT RPC between the browser and the web-client servlet is a separate protocol governed by GWT codegen and the servlet's redeploy; pure GWT-layer changes (`web-client/.../gwt/`) that don't touch `api/` don't need a bump.

### Bump (+1, in the same commit) when

**Remote interface signature changes.** Any method add/remove/rename, parameter/return type change, or `throws` clause change in one of: `RemoteLogicsInterface`, `RemoteLogicsLoaderInterface`, `RemoteConnectionInterface`, `RemoteFormInterface`, `RemoteClientInterface`, `RemoteNavigatorInterface`, `RemoteSessionInterface`, `RemoteRequestInterface`, `RemoteInterface` (all under `api/.../interop/`).

**Serializable wire-format change.** Any change to the serialized form of a class reachable — directly or transitively — from a Remote method parameter or return type. Usually under `api/.../interop/`, but a Serializable field can reach elsewhere in `api/` (e.g. `lsfusion.base.*`); reachability matters, not path. Triggers: add/remove non-`transient` field, change field type, add/remove/rename an `enum` constant, introduce or change `serialVersionUID`, change `extends`/`implements`, or modify `readObject` / `writeObject` / `readResolve` / `writeReplace`.

**New polymorphic subclass sent over RMI.** Java serialization writes the runtime class name; an old peer without the class hits `ClassNotFoundException`. Hierarchies that flow this way: `ClientAction` in `interop/action/` (server returns subclasses inside `ServerResponse`), `FormEvent`/`InputEvent` in `interop/form/event/`, `Authentication` in `interop/connection/authentication/`, and any sealed-by-convention hierarchy where the server picks a subclass and the client must deserialize it.

**Protocol-semantics change.** Same bytes, different meaning — flipped default, previously-optional field now required, ignored value now interpreted, repurposed exception code. Both peers deserialize fine and then disagree on behavior.

### Don't bump

- Refactors that don't touch `api/`.
- Changes inside an RMI implementation that don't change the interface or any Serializable wire type.
- `transient` / `static` fields, caches, lazy holders. Plain `private` is **not** exempt — only `transient` is.
- Private methods, non-serialized inner classes, helpers outside the Remote API and serialized form.
- GWT-layer changes (`web-client/.../gwt/`) that don't touch `api/`.
- `.lsf`, build, test, docs.

**Quick fallback** when transitive reachability isn't obvious in 30 seconds: if the changed class is used by or contained in a known wire payload (`ServerResponse`, `ClientAction` subtree, `FormClientData`, `NavigatorInfo`, `ServerSettings`), assume reachable and bump unless you can prove otherwise. Call out the bump in the commit body so the reviewer can confirm the trigger.
