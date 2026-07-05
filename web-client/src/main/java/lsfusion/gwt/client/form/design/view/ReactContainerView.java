package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ResizableComplexPanel;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GContainer;
import lsfusion.gwt.client.form.design.view.flex.LayoutContainerView;

// CUSTOM REACT 'fn': hosts a React component (window[fn], fn = container's custom) that OWNS this container's subtree.
// The component receives props { data, controller }: `data` is the @lsfusion/core-shaped projected form state
// (re-rendered on each form change), `controller` mutates the form. That is the primary, props-down contract — the
// normal optimization is React.memo or React Compiler over props.data (structural sharing keeps unchanged refs stable).
// For OPT-IN fine-grained re-render without prop-threading, descendants use the window.lsfusion hooks
// (useFormData(selector) over the data / useFormController) backed by a React context the platform installs around
// the component (react-redux's Provider + useSelector/useDispatch shape) — e.g. useFormData(s => s.i.list[k]).
// The hooks are zero-overhead when no component subscribes: the snapshot IS the (structurally shared) data object.
public class ReactContainerView extends LayoutContainerView {

    private final ResizableComplexPanel panel;
    private final String componentName; // = container.custom
    private final JavaScriptObject store; // selector store behind the context (subscribe/getSnapshot), per view, survives re-mounts
    private final JavaScriptObject ctxValue; // stable context value { store, controller }: the Provider value must not change identity, or every context consumer re-renders
    private JavaScriptObject root;       // the ReactDOM root, created lazily per attach
    private JavaScriptObject lastData = JavaScriptObject.createObject(); // last projected @lsfusion/core data (== the hook snapshot); starts empty, so no fallbacks anywhere

    public ReactContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        componentName = container.getCustom();
        store = createStore(); // DOM-independent, created once; updateData can feed it even before first mount
        ctxValue = createCtxValue(store, formController.controller);
        panel = new ResizableComplexPanel();
        GwtClientUtils.addClassName(panel, "panel-react");
        panel.addAttachHandler(event -> {
            if (event.isAttached())
                mount(panel.getElement());
            else
                unmount();
        });
    }

    @Override
    protected void addImpl(int index) {
        // React owns the subtree; GWT children are not laid out here
    }

    @Override
    protected void removeImpl(int index) {
    }

    @Override
    public Widget getView() {
        return panel;
    }

    public GContainer getContainer() {
        return container;
    }

    // pushed from GFormController.applyRemoteChanges after each form change
    public void updateData(JavaScriptObject data) {
        if (data == lastData)
            return;
        lastData = data;
        notifyStore(); // fine-grained subscribers (useFormData selectors)
        render();      // props.data
    }

    private void mount(Element element) {
        if (!createRoot(element))
            return;
        render();
    }

    private JavaScriptObject resolveComponent() {
        return GwtClientUtils.getGlobalField(componentName, "reactView", true);
    }

    private native JavaScriptObject createCtxValue(JavaScriptObject store, JavaScriptObject controller)/*-{
        return { store: store, controller: controller };
    }-*/;

    private native boolean createRoot(Element element)/*-{
        if (!$wnd.React || !$wnd.ReactDOM) {
            $wnd.console.error("lsFusion CUSTOM REACT: window.React / window.ReactDOM are not loaded");
            return false;
        }
        if (!this.@lsfusion.gwt.client.form.design.view.ReactContainerView::resolveComponent()()) {
            $wnd.console.error("lsFusion CUSTOM REACT: component '" + this.@lsfusion.gwt.client.form.design.view.ReactContainerView::componentName + "' not found in registry or on window");
            return false;
        }
        @lsfusion.gwt.client.form.design.view.ReactContainerView::installHooks()();
        this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root = $wnd.ReactDOM.createRoot(element);
        return true;
    }-*/;

    // install the form context + hooks once (window.lsfusion): the canonical Provider + useSelector-style API.
    // public surface = the use* hooks + List; the context itself is internal (non-enumerable __formContext).
    // Runs from createRoot, where window.React presence is already checked.
    // BEHAVIORAL TWIN of window.lsfusion.__installReactHooks in lsfusion-custom-registry.js (the eager path, run by each
    // compiled bundle's preamble before its body so a module-top `const List = window.lsfusion.List` resolves). This
    // self-contained copy is the mount-time installer and keeps the GWT client working without that script (and for
    // hand-written global components, which get no preamble). Keep the two copies in sync.
    private static native void installHooks()/*-{
        var ns = $wnd.lsfusion || ($wnd.lsfusion = {});
        if (ns.__formContext)
            return;
        var React = $wnd.React;
        var Ctx = React.createContext(null);
        Object.defineProperty(ns, '__formContext', { value: Ctx }); // internal (non-enumerable)
        // subscribe to a selected slice of the projected data; re-renders only when selector(data) changes
        // (Object.is) — e.g. useFormData(s => s.i.list[k]). No selector = the whole data.
        ns.useFormData = function(selector) {
            var store = React.useContext(Ctx).store;
            var select = selector || function(s) { return s; };
            return React.useSyncExternalStore(store.subscribe, function() { return select(store.getSnapshot()); });
        };
        ns.useFormController = function() { return React.useContext(Ctx).controller; };
        // <List>: per-row render economy with NO app-level memo/hooks (the user component just reads props.row).
        // There is a progression of ways to render a group's rows, from least to most capable:
        //   data.<g>.list.map(...)  -> a simpler <List simple/>  -> the main <List/> (default).
        // ns.List is a HOOK-FREE dispatcher that renders one of two internal list components (so the choice swaps a
        // component TYPE — clean remount — and never makes a hook call conditional). Pick the simpler one via the
        // per-element prop `simple` or, globally, window.lsfusion.listSimple (prop overrides global; default = main).
        //
        // KeysList (the main <List>, default): maps the projection's referentially-STABLE keys array (data.keys; new
        // ref only on add/remove/reorder) inside useMemo, and renders a module-level memoized RowWrapper per key. Each
        // wrapper SUBSCRIBES to its own row via useFormData(s => s[groupSID].byKey[rowKey]); a value/current change
        // re-renders only the changed row and the outer map is skipped (keys ref unchanged) -> O(1) per value update.
        // SimpleList (<List simple/>): the simpler path — maps the churny data.list and memoizes the row component
        // (React.memo, cached per component in a WeakMap); economy relies on the projector reusing unchanged row refs.
        // Both must be used as a component (<List .../> / createElement(List, ...)); component ?? children; other props
        // pass through with reserved props (row/rowKey/index) applied LAST.
        var RowWrapper = React.memo(function(p) {
            var row = ns.useFormData(function(s) { var g = s && s[p.groupSID]; var bk = g && g.byKey; return bk ? bk[p.rowKey] : null; });
            if (row == null) return null; // row removed (about to unmount): don't hand a null row to the component
            var rowProps = {};
            var pass = p.pass;
            if (pass) for (var pk in pass) rowProps[pk] = pass[pk];
            // reserved props LAST so a pass-through prop can't clobber the platform row
            rowProps.row = row; rowProps.rowKey = p.rowKey; rowProps.index = p.index;
            return React.createElement(p.component, rowProps);
        });
        var KeysList = function(props) {
            var comp = props.component || props.children;
            var data = props.data || {};
            var keys = data.keys || [];
            var groupSID = data.__groupSID;
            var pass = null, deps = [keys, comp, groupSID], pk = [];
            for (var k in props) if (k !== 'data' && k !== 'component' && k !== 'children' && k !== 'simple') pk.push(k);
            pk.sort(); // deterministic, NAME-aware deps (push name + value) so {foo:1}->{bar:1} invalidates; length varies only if the passthrough prop SET changes between renders (rare)
            for (var pi = 0; pi < pk.length; pi++) { (pass || (pass = {}))[pk[pi]] = props[pk[pi]]; deps.push(pk[pi]); deps.push(props[pk[pi]]); }
            return React.useMemo(function() {
                return keys.map(function(rowKey, index) {
                    return React.createElement(RowWrapper, { key: rowKey, groupSID: groupSID, rowKey: rowKey, index: index, component: comp, pass: pass });
                });
            }, deps);
        };
        var simpleMemo = new $wnd.WeakMap();
        var SimpleList = function(props) {
            var comp = props.component || props.children;
            var rows = (props.data && props.data.list) || [];
            var memoComp = simpleMemo.get(comp);
            if (!memoComp) { memoComp = React.memo(comp); simpleMemo.set(comp, memoComp); }
            var pass = null;
            for (var k in props) if (k !== 'data' && k !== 'component' && k !== 'children' && k !== 'simple') (pass || (pass = {}))[k] = props[k];
            return rows.map(function(row, index) {
                var rowProps = {};
                if (pass) for (var pk in pass) rowProps[pk] = pass[pk];
                rowProps.key = row.key; rowProps.row = row; rowProps.rowKey = row.key; rowProps.index = index;
                return React.createElement(memoComp, rowProps);
            });
        };
        ns.List = function(props) { // hook-free dispatcher: swaps component type, never a conditional hook
            var simple = props.simple == null ? !!ns.listSimple : !!props.simple;
            return React.createElement(simple ? SimpleList : KeysList, props);
        };
        // <BucketScope>/useBucket: the pivot/matrix analogue of <List> — re-buckets ONE group's rows into derived
        // cells (bucketKey -> rowKey[]) with per-CELL render economy. The app owns the layout (it supplies the cell
        // keys, so empty cells render too) and the cell markup; the platform owns keying, subscription and economy:
        // a move re-renders only the old + new cell, an in-place edit only the row (its useFormData), an empty cell
        // returns the one frozen EMPTY. Membership only, NOT per-cell aggregates (those are GPivot's job).
        var EMPTY = Object.freeze([]);
        var BucketCtx = React.createContext(null);
        var makeBucketStore = function(formStore, groupSID, bucketOf) {
            // ref-diff index (the <List>-parity engine): rowCache maps rowKey -> { ref: row, b: bucketKey[]|null }.
            // An unchanged row REF can't change its bucket (rows are structurally shared), so a form change costs
            // O(rows) ref-compares, re-bucketing only the changed rows (when some cell DID change, one more
            // rescan of the rows rebuilds the dirty cells' arrays); unrelated-group changes cost ONE compare.
            // All dictionaries are null-prototype: row/bucket keys come from app data ('__proto__' must be plain).
            var listeners = new $wnd.Set(), buckets = Object.create(null), rowCache = Object.create(null), lastNode, lastKeys = EMPTY, formUnsub = null, pending = false;
            var norm = function(bk) { // bucketOf result -> deduped string[] | null (null = the row lands nowhere)
                if (bk == null) return null;
                if (!Array.isArray(bk)) return ['' + bk];
                var r = [];
                for (var i = 0; i < bk.length; i++) if (bk[i] != null) { var c = '' + bk[i]; if (r.indexOf(c) < 0) r.push(c); }
                return r.length ? r : null;
            };
            var same = function(a, b) { // element-wise INCLUDING order: cell arrays follow the group order
                if (a === b) return true;
                if (!a || !b || a.length !== b.length) return false;
                for (var i = 0; i < a.length; i++) if (a[i] !== b[i]) return false;
                return true;
            };
            var ensure = function() { // sync the index to the current snapshot
                var s = formStore.getSnapshot();
                var g = s ? s[groupSID] : null;
                if (g === lastNode) return;
                var keys = (g && g.keys) || EMPTY, byKey = (g && g.byKey) || null;
                // a reorder keeps every row ref (only the keys array ref betrays it) but changes the order INSIDE
                // cells -> rebuild all cells (`same` still keeps the refs of the untouched ones)
                var all = keys !== lastKeys, dirty = all ? null : Object.create(null), any = all;
                var mark = function(b) { if (b && !all) { any = true; for (var i = 0; i < b.length; i++) dirty[b[i]] = true; } };
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
                subscribe: function(l) {
                    listeners.add(l);
                    if (listeners.size === 1) formUnsub = formStore.subscribe(function() { ensure(); if (pending) { pending = false; listeners.forEach(function(x) { x(); }); } }); // unchanged cells then skip by Object.is on their array
                    return function() { listeners['delete'](l); if (!listeners.size && formUnsub) { formUnsub(); formUnsub = null; } };
                },
                getBucket: function(k) { ensure(); return buckets[k] || EMPTY; } // ensure: a cell renders before its effect subscribes
            };
        };
        ns.BucketScope = function(props) { // props: group (SID), bucketOf(row, rowKey) -> bucketKey | bucketKey[] | null, bucketDeps
            var formStore = React.useContext(Ctx).store;
            // bucketOf is CAPTURED at store creation: bucketDeps must list the outside values it closes over (the
            // hook-deps contract; omitting one = the usual stale closure) and keep a stable length; the store swaps
            // when they change. No render-time ref mutation, so an interrupted render can't leak an uncommitted
            // bucketOf into the live store.
            var bucketOf = props.bucketOf;
            var store = React.useMemo(function() {
                return makeBucketStore(formStore, props.group, bucketOf);
            }, [formStore, props.group].concat(props.bucketDeps || EMPTY));
            return React.createElement(BucketCtx.Provider, { value: store }, props.children);
        };
        ns.useBucket = function(bucketKey) { // one call per cell component, for its FIXED key (hook rules)
            var store = React.useContext(BucketCtx);
            if (!store) throw new Error("lsfusion.useBucket: no enclosing BucketScope");
            var key = '' + bucketKey;
            return React.useSyncExternalStore(store.subscribe, function() { return store.getBucket(key); });
        };
        // <Buckets group cells bucketOf [bucketDeps] component/>: the flat sugar — a BucketScope whose body maps
        // `cells` to memoized wrappers, each subscribing to its own cell; mirrors KeysList's name-aware pass deps.
        // The cell component receives { cellKey, rowKeys, index, ...pass } (reserved props applied LAST).
        var CellWrapper = React.memo(function(p) {
            var rowKeys = ns.useBucket(p.cellKey);
            var cellProps = {};
            var pass = p.pass;
            if (pass) for (var pk in pass) cellProps[pk] = pass[pk];
            cellProps.cellKey = p.cellKey; cellProps.rowKeys = rowKeys; cellProps.index = p.index;
            return React.createElement(p.component, cellProps);
        });
        var BucketCells = function(props) {
            var comp = props.component || props.children;
            var cells = props.cells || EMPTY;
            var pass = null, deps = [cells, comp], pk = [];
            for (var k in props) if (k !== 'cells' && k !== 'component' && k !== 'children' && k !== 'group' && k !== 'bucketOf' && k !== 'bucketDeps') pk.push(k);
            pk.sort(); // deterministic, NAME-aware deps (same scheme as KeysList)
            for (var pi = 0; pi < pk.length; pi++) { (pass || (pass = {}))[pk[pi]] = props[pk[pi]]; deps.push(pk[pi]); deps.push(props[pk[pi]]); }
            return React.useMemo(function() {
                return cells.map(function(cellKey, index) {
                    return React.createElement(CellWrapper, { key: '' + cellKey, cellKey: '' + cellKey, index: index, component: comp, pass: pass });
                });
            }, deps);
        };
        ns.Buckets = function(props) {
            return React.createElement(ns.BucketScope, { group: props.group, bucketOf: props.bucketOf, bucketDeps: props.bucketDeps },
                React.createElement(BucketCells, props));
        };
    }-*/;

    // the hook snapshot IS the projected data object itself: lastData only changes ref when the data changed, and
    // structural sharing keeps unchanged subtrees reference-equal — exactly what useSyncExternalStore selectors need.
    // (selector hooks fit this immutable-snapshot model; a mutable-proxy useSnapshot would be the wrong fit.)
    private native JavaScriptObject createStore()/*-{
        var reactView = this;
        var listeners = new $wnd.Set();
        return {
            subscribe: function(listener) {
                listeners.add(listener);
                return function() {
                    listeners['delete'](listener);
                };
            },
            getSnapshot: function() {
                return reactView.@lsfusion.gwt.client.form.design.view.ReactContainerView::lastData;
            },
            _notify: function() {
                listeners.forEach(function(listener) {
                    listener();
                });
            }
        };
    }-*/;

    private native void notifyStore()/*-{
        this.@lsfusion.gwt.client.form.design.view.ReactContainerView::store._notify();
    }-*/;

    private native void render()/*-{
        var root = this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root;
        if (!root) return;
        var React = $wnd.React;
        var component = this.@lsfusion.gwt.client.form.design.view.ReactContainerView::resolveComponent()();
        var ctxValue = this.@lsfusion.gwt.client.form.design.view.ReactContainerView::ctxValue;
        var props = {
            data: this.@lsfusion.gwt.client.form.design.view.ReactContainerView::lastData, // projected @lsfusion/core form state (primary contract; re-rendered each data change)
            controller: ctxValue.controller // changeProperty/changeProperties/changeObject + exec/eval/evalAction/change
        };
        root.render(React.createElement($wnd.lsfusion.__formContext.Provider, { value: ctxValue }, React.createElement(component, props)));
    }-*/;

    private native void unmount()/*-{
        var root = this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root;
        if (root) {
            root.unmount();
            this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root = null;
        }
    }-*/;
}
