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
// compiled bundle), but INSTALLED lazily — by each compiled bundle's preamble (esbuild banner -> __installReactHooks)
// right before the bundle body runs, and by ReactContainerView.createRoot at mount for a hand-written global that gets
// no preamble. That ordering is what lets a bundle alias a helper at module TOP — `const List = window.lsfusion.List;`
// — and still bind to the FINAL window.React: an app may override React at a less-negative before-system order (after
// this script but before the bundles / the form), so we must NOT capture React eagerly here. Idempotent; the first
// caller wins. This is the ONLY copy of the install logic; the GWT client calls it, it does not duplicate it.
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
        // the DELEGATION primitives. <LsfComponent sid/> is a placeholder for a DESIGN child marked `delegate = TRUE`:
        // the platform MOVES that child's real GWT view into the host node on mount and back to its park node on
        // cleanup. The host renders NO React children, ever: the moment React owns a child of that node it can wipe the
        // foreign GWT DOM. Cleanup is the exact inverse of mount, so StrictMode's mount->cleanup->mount cannot stack
        // duplicates. The GWT view stays logically attached throughout, so no onUnload/onLoad fires.
        // useLsfComponent(sid) -> a ref callback mounting the delegated child into the element the component ALREADY
        // renders, so no placeholder node exists at all. The platform marks that element (class + data-lsf-sid/kind).
        ns.useLsfComponent = function (sid) {
            var view = React.useContext(Ctx).view;
            var held = React.useRef(null); // the ref callback is handed null on detach, so the host is remembered here
            return React.useCallback(function (host) {
                if (held.current) { view.unmount(sid, held.current); held.current = null; }
                if (host) { view.mount(sid, host); held.current = host; }
            }, [view, sid]);
        };
        ns.LsfComponent = function (props) {
            return React.createElement('div', { ref: ns.useLsfComponent(props.sid), className: props.className, style: props.style });
        };
        // the delegated children's descriptors live in the projected data (data.components = { sid: {caption, image} },
        // in DESIGN order) — a plain data field, so it is read with useFormData like any other; no dedicated
        // hook is needed. A dynamic caption marks the scope dirty, so build() hands a new components map and this re-renders.
        var EMPTY_COMPONENTS = Object.freeze({}); // STABLE ref when a scope has no delegated children (else useSyncExternalStore loops on a fresh {})
        // <LsfComponents/>: place every delegated child, in DESIGN order. This is the generic default that makes the
        // container content-extensible: a third module's `EXTEND FORM` + `delegate = TRUE` child appears in position
        // without touching the react component. Each is drawn with its caption above it — the caption a delegated
        // component no longer draws in GWT is drawn here instead (as a tabbed container draws captions in the tab strip).
        // Pass props.components (or read props.data.components) for a different layout; the caption is in each descriptor.
        ns.LsfComponents = function (props) {
            var data = ns.useFormData(function (s) { return (s && s.components) || EMPTY_COMPONENTS; }); // ALWAYS call the hook, then let props override
            var components = props.components || data; // the sid -> descriptor map (DESIGN insertion order)
            return Object.keys(components).map(function (sid) {
                var c = components[sid];
                var caption = (c.caption != null || c.image != null)
                    ? React.createElement('div', { className: 'lsf-slot-caption' },
                        c.image != null ? React.createElement('span', { className: 'lsf-slot-image', dangerouslySetInnerHTML: { __html: c.image } }) : null,
                        c.caption)
                    : null;
                return React.createElement('div', { key: sid, className: 'lsf-slot' },
                    caption,
                    React.createElement(ns.LsfComponent, { sid: sid }));
            });
        };
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
        // <BucketScope>/useBucket/<Buckets>: the pivot/matrix analogue of <List> (bucketKey -> rowKey[] index with
        // per-cell subscription); the canonical narrative lives on the GWT side (ReactContainerView.installHooks).
        var EMPTY = Object.freeze([]);
        var BucketCtx = React.createContext(null);
        var makeBucketStore = function (formStore, groupSID, bucketOf) {
            // ref-diff index: O(rows) ref-compares per form change, re-bucketing only the changed rows (see the JSNI twin)
            var listeners = new Set(), buckets = Object.create(null), rowCache = Object.create(null), lastNode, lastKeys = EMPTY, formUnsub = null, pending = false;
            var norm = function (bk) { // bucketOf result -> deduped string[] | null (null = the row lands nowhere)
                if (bk == null) return null;
                if (!Array.isArray(bk)) return ['' + bk];
                var r = [];
                for (var i = 0; i < bk.length; i++) if (bk[i] != null) { var c = '' + bk[i]; if (r.indexOf(c) < 0) r.push(c); }
                return r.length ? r : null;
            };
            var same = function (a, b) { // element-wise INCLUDING order: cell arrays follow the group order
                if (a === b) return true;
                if (!a || !b || a.length !== b.length) return false;
                for (var i = 0; i < a.length; i++) if (a[i] !== b[i]) return false;
                return true;
            };
            var ensure = function () { // sync the index to the current snapshot
                var s = formStore.getSnapshot();
                var g = s ? s[groupSID] : null;
                if (g === lastNode) return;
                var keys = (g && g.keys) || EMPTY, byKey = (g && g.byKey) || null;
                var all = keys !== lastKeys, dirty = all ? null : Object.create(null), any = all;
                var mark = function (b) { if (b && !all) { any = true; for (var i = 0; i < b.length; i++) dirty[b[i]] = true; } };
                var next = Object.create(null), rk, i;
                for (i = 0; i < keys.length; i++) {
                    rk = keys[i];
                    var row = byKey ? byKey[rk] : null;
                    var prev = rowCache[rk];
                    if (prev && prev.ref === row) { next[rk] = prev; continue; } // unchanged ref -> same bucket, skip
                    var nb = row == null ? null : norm(bucketOf(row, rk));
                    if (!prev || !same(prev.b, nb)) { mark(prev && prev.b); mark(nb); }
                    next[rk] = { ref: row, b: nb };
                }
                for (rk in rowCache) if (!(rk in next)) mark(rowCache[rk].b); // removed rows leave their old cells
                // commit only below the last bucketOf call: a throw above leaves the whole index at the previous
                // snapshot, so the next ensure RETRIES instead of early-returning over a half-advanced state.
                // pending: a render-time ensure (getBucket) may consume the change BEFORE the form listener runs; the flag survives to it
                lastNode = g; lastKeys = keys; rowCache = next; pending = true;
                if (any) { // rebuild ONLY the dirty cells' arrays, membership in group (keys) order
                    var acc = Object.create(null);
                    for (i = 0; i < keys.length; i++) {
                        var e = next[keys[i]], b = e && e.b;
                        if (b) for (var j = 0; j < b.length; j++) { var c = b[j]; if (all || dirty[c]) (acc[c] || (acc[c] = [])).push(keys[i]); }
                    }
                    if (all) {
                        for (var c2 in acc) if (same(buckets[c2], acc[c2])) acc[c2] = buckets[c2]; // keep unchanged refs
                        buckets = acc;
                    } else
                        for (var c3 in dirty) {
                            var arr = acc[c3];
                            if (!arr) delete buckets[c3]; // emptied cell -> back to the stable EMPTY
                            else if (!same(buckets[c3], arr)) buckets[c3] = arr;
                        }
                }
            };
            return {
                subscribe: function (l) {
                    listeners.add(l);
                    if (listeners.size === 1) formUnsub = formStore.subscribe(function () { ensure(); if (pending) { pending = false; listeners.forEach(function (x) { x(); }); } }); // unchanged cells then skip by Object.is on their array
                    return function () { listeners['delete'](l); if (!listeners.size && formUnsub) { formUnsub(); formUnsub = null; } };
                },
                getBucket: function (k) { ensure(); return buckets[k] || EMPTY; } // ensure: a cell renders before its effect subscribes
            };
        };
        ns.BucketScope = function (props) { // props: group (SID), bucketOf(row, rowKey) -> bucketKey | bucketKey[] | null, bucketDeps
            var formStore = React.useContext(Ctx).store;
            // bucketOf is CAPTURED at store creation (see the JSNI twin for the full narrative)
            var bucketOf = props.bucketOf;
            var store = React.useMemo(function () {
                return makeBucketStore(formStore, props.group, bucketOf);
            }, [formStore, props.group].concat(props.bucketDeps || EMPTY));
            return React.createElement(BucketCtx.Provider, { value: store }, props.children);
        };
        ns.useBucket = function (bucketKey) { // one call per cell component, for its FIXED key (hook rules)
            var store = React.useContext(BucketCtx);
            if (!store) throw new Error("lsfusion.useBucket: no enclosing BucketScope");
            var key = '' + bucketKey;
            return React.useSyncExternalStore(store.subscribe, function () { return store.getBucket(key); });
        };
        var CellWrapper = React.memo(function (p) {
            var rowKeys = ns.useBucket(p.cellKey);
            var cellProps = {};
            var pass = p.pass;
            if (pass) for (var pk in pass) cellProps[pk] = pass[pk];
            cellProps.cellKey = p.cellKey; cellProps.rowKeys = rowKeys; cellProps.index = p.index;
            return React.createElement(p.component, cellProps);
        });
        var BucketCells = function (props) {
            var comp = props.component || props.children;
            var cells = props.cells || EMPTY;
            var pass = null, deps = [cells, comp], pk = [];
            for (var k in props) if (k !== 'cells' && k !== 'component' && k !== 'children' && k !== 'group' && k !== 'bucketOf' && k !== 'bucketDeps') pk.push(k);
            pk.sort();
            for (var pi = 0; pi < pk.length; pi++) { (pass || (pass = {}))[pk[pi]] = props[pk[pi]]; deps.push(pk[pi]); deps.push(props[pk[pi]]); }
            return React.useMemo(function () {
                return cells.map(function (cellKey, index) {
                    return React.createElement(CellWrapper, { key: '' + cellKey, cellKey: '' + cellKey, index: index, component: comp, pass: pass });
                });
            }, deps);
        };
        ns.Buckets = function (props) {
            return React.createElement(ns.BucketScope, { group: props.group, bucketOf: props.bucketOf, bucketDeps: props.bucketDeps },
                React.createElement(BucketCells, props));
        };
    };
})();
