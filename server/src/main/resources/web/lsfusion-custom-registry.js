// lsFusion custom-JS registry — platform-vendored, loaded BEFORE compiled bundles and before GWT (negative onWebClientInit order).
// Compiled web/.compiled/*.js bundles call register(name, impl); the GWT client resolves names registry-first with a
// window[name] fallback (so legacy hand-written globals keep working). One namespace shared with the CUSTOM REACT hooks.
// Public surface: lsfusion.custom.register / lsfusion.custom.get. The entry map is closure-local; collision
// diagnostics are exposed non-enumerably (lsfusion.custom.diagnostics) for debugging, not as API.
(function () {
    var ns = window.lsfusion || (window.lsfusion = {});
    if (ns.custom && ns.custom.register) return; // idempotent (defensive: don't clobber if already installed)
    var entries = Object.create(null);
    var diagnostics = [];
    ns.custom = {
        // register a compiled export under its public name. kind is optional (the .lsf call site supplies the
        // expected kind at resolve time); duplicate names are a hard error (auto-load order across bundles is unstable).
        register: function (name, impl, kind) {
            if (name in entries) {
                if (entries[name].impl === impl) return; // the same impl re-registered (e.g. a bundle loaded twice) — harmless
                throw new Error("lsfusion.custom: duplicate registry name '" + name + "'");
            }
            if (window[name] !== undefined && window[name] !== impl) {
                // legacy global with the same name exists and differs — lsFusion resolvers (registry-first) will see
                // the compiled impl while direct window[name] readers keep the legacy one; must be LOUD, not silent.
                var msg = "lsfusion.custom: collision — '" + name + "' exists both as a window global and a compiled registration (registry wins for lsFusion, window stays for direct readers)";
                diagnostics.push(msg);
                console.warn(msg);
            }
            entries[name] = { impl: impl, kind: kind };
            if (window[name] === undefined) window[name] = impl; // back-compat alias (don't overwrite an existing global)
        },
        // resolve registry-first; expectedKind is a soft, call-site-supplied check (registration kind is optional).
        get: function (name, expectedKind) {
            var e = entries[name];
            if (e) {
                if (expectedKind && e.kind && e.kind !== expectedKind)
                    throw new Error("lsfusion.custom: '" + name + "' is kind " + e.kind + ", used as " + expectedKind);
                return e.impl;
            }
            return undefined; // caller falls back to window[name]
        }
    };
    Object.defineProperty(ns.custom, 'diagnostics', { value: diagnostics }); // internal (non-enumerable)
})();
