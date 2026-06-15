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
