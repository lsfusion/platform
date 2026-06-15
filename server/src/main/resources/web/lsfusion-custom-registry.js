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

// CUSTOM REACT hooks + <List>, on the same window.lsfusion namespace. DEFINED here (this script loads before any
// compiled bundle), but INSTALLED lazily by each compiled bundle's preamble (esbuild banner -> __installReactHooks)
// right before the bundle body runs. That ordering is what lets a bundle alias a helper at module TOP —
// `const List = window.lsfusion.List;` — and still bind to the FINAL window.React: an app may override React at a
// less-negative before-system order (after this script but before the bundles), so we must NOT capture React eagerly
// here. Idempotent; the first caller wins.
// BEHAVIORAL TWIN of ReactContainerView.installHooks() (GWT JSNI), which installs the same logic at mount as the
// self-contained fallback (and for hand-written global components, which get no preamble). The two MUST be kept in
// sync; the canonical narrative for <List>/KeysList/SimpleList lives on the GWT side. Differences vs that copy: this
// runs eagerly so it has an `if (!React) return` guard; `window.` instead of GWT's `$wnd.`.
(function () {
    var ns = window.lsfusion || (window.lsfusion = {});
    if (ns.__installReactHooks) return; // idempotent (defensive: don't redefine)
    ns.__installReactHooks = function () {
        if (ns.__formContext) return;
        var React = window.React;
        if (!React) return; // eager path may run before React loads (defensive: it is a before-system resource, so normally present)
        var Ctx = React.createContext(null);
        Object.defineProperty(ns, '__formContext', { value: Ctx }); // internal (non-enumerable)
        ns.useFormData = function (selector) {
            var store = React.useContext(Ctx).store;
            var select = selector || function (s) { return s; };
            return React.useSyncExternalStore(store.subscribe, function () { return select(store.getSnapshot()); });
        };
        ns.useFormController = function () { return React.useContext(Ctx).controller; };
        var RowWrapper = React.memo(function (p) {
            var row = ns.useFormData(function (s) { var g = s && s[p.groupSID]; var bk = g && g.byKey; return bk ? bk[p.rowKey] : null; });
            if (row == null) return null; // row removed (about to unmount): don't hand a null row to the component
            var rowProps = {};
            var pass = p.pass;
            if (pass) for (var pk in pass) rowProps[pk] = pass[pk];
            rowProps.row = row; rowProps.rowKey = p.rowKey; rowProps.index = p.index;
            return React.createElement(p.component, rowProps);
        });
        var KeysList = function (props) {
            var comp = props.component || props.children;
            var data = props.data || {};
            var keys = data.keys || [];
            var groupSID = data.__groupSID;
            var pass = null, deps = [keys, comp, groupSID], pk = [];
            for (var k in props) if (k !== 'data' && k !== 'component' && k !== 'children' && k !== 'simple') pk.push(k);
            pk.sort();
            for (var pi = 0; pi < pk.length; pi++) { (pass || (pass = {}))[pk[pi]] = props[pk[pi]]; deps.push(pk[pi]); deps.push(props[pk[pi]]); }
            return React.useMemo(function () {
                return keys.map(function (rowKey, index) {
                    return React.createElement(RowWrapper, { key: rowKey, groupSID: groupSID, rowKey: rowKey, index: index, component: comp, pass: pass });
                });
            }, deps);
        };
        var simpleMemo = new WeakMap();
        var SimpleList = function (props) {
            var comp = props.component || props.children;
            var rows = (props.data && props.data.list) || [];
            var memoComp = simpleMemo.get(comp);
            if (!memoComp) { memoComp = React.memo(comp); simpleMemo.set(comp, memoComp); }
            var pass = null;
            for (var k in props) if (k !== 'data' && k !== 'component' && k !== 'children' && k !== 'simple') (pass || (pass = {}))[k] = props[k];
            return rows.map(function (row, index) {
                var rowProps = {};
                if (pass) for (var pk in pass) rowProps[pk] = pass[pk];
                rowProps.key = row.key; rowProps.row = row; rowProps.rowKey = row.key; rowProps.index = index;
                return React.createElement(memoComp, rowProps);
            });
        };
        ns.List = function (props) { // hook-free dispatcher: swaps component type, never a conditional hook
            var simple = props.simple == null ? !!ns.listSimple : !!props.simple;
            return React.createElement(simple ? SimpleList : KeysList, props);
        };
    };
})();
