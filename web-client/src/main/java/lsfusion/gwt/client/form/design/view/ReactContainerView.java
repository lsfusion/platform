package lsfusion.gwt.client.form.design.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.form.controller.GFormController;
import lsfusion.gwt.client.form.design.GComponent;
import lsfusion.gwt.client.form.design.GContainer;

import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

// CUSTOM REACT 'fn': hosts a React component (window[fn], fn = container's custom) that OWNS this container's subtree,
// except for the children marked `delegate = TRUE` in DESIGN — those keep their real GWT view, are excluded from the
// projection, and the component places them with <LsfComponent sid/> (see props.data.components / <LsfComponents/>).
// The component receives props { data, controller }: `data` is the @lsfusion/core-shaped projected form state
// (re-rendered on each form change) and carries the delegated children's descriptors in data.components; `controller`
// mutates the form. That is the primary, props-down contract — the
// normal optimization is React.memo or React Compiler over props.data (structural sharing keeps unchanged refs stable).
// For OPT-IN fine-grained re-render without prop-threading, descendants use the window.lsfusion hooks
// (useFormData(selector) over the data / useFormController) backed by a React context the platform installs around
// the component (react-redux's Provider + useSelector/useDispatch shape) — e.g. useFormData(s => s.i.list[k]).
// The hooks are zero-overhead when no component subscribes: the snapshot IS the (structurally shared) data object.
public class ReactContainerView extends ParkedContainerView {

    private final String componentName; // = container.custom
    private final JavaScriptObject store; // selector store behind the context (subscribe/getSnapshot), per view, survives re-mounts
    private final JavaScriptObject ctxValue; // stable context value { store, controller }: the Provider value must not change identity, or every context consumer re-renders
    private JavaScriptObject root;       // the ReactDOM root, created lazily per attach
    private JavaScriptObject lastData = JavaScriptObject.createObject(); // last projected @lsfusion/core data (== the hook snapshot); starts empty, so no fallbacks anywhere

    // sID -> the host claimed for it (the first placeholder wins). The child's view is mounted there once it exists, so
    // the host outlives a SHOWIF drop/rebuild of the view: whether the view exists now is indexOfDelegated(sid) >= 0
    private final Map<String, Element> hosts = new HashMap<>();
    private final Set<String> reactHidden = new HashSet<>(); // sIDs the server has been told the component is not showing
    private boolean unmountingRoot; // true while the React root is torn down (form close), so no per-child hide is sent

    public ReactContainerView(GFormController formController, GContainer container) {
        super(container, formController);
        componentName = container.getCustom();
        store = createStore(); // DOM-independent, created once; updateData can feed it even before first mount
        ctxValue = createCtxValue(store, formController.controller);
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
        // React owns the subtree, so only a DELEGATED child gets a GWT view here
        super.addImpl(index);

        // the view is built by its controller, and a property hidden with `remove` is dropped and built again as it
        // comes back, long after React rendered the host. A host never re-runs its ref for that, and the inherited
        // removeImpl leaves the host in `hosts`, so the one that has been waiting is mounted here
        Element host = hosts.get(children.get(index).sID);
        if (host != null)
            attachView(index, host);
    }

    // called back by the host's ref, and the cleanup is its exact inverse (F1). React runs every ref detach of a commit
    // before any attach, so a mount can never race an unmount of the same child: a second live host for one sid is
    // always a duplicate placeholder, never a legitimate move.
    private void mountComponent(String sid, Element host) {
        stampHost(host, sid); // marks the host even when the mount reports a problem below
        if (!isDelegatedChild(sid)) { // a typo, or a non-delegated child: no view will ever exist for this host
            reportUnknown(sid, host);
            return;
        }
        Element current = hosts.get(sid);
        if (current != null && current != host) { // the FIRST placeholder keeps the child, so it cannot be left empty
            reportDuplicate(sid, host);
            return;
        }
        hosts.put(sid, host);
        setReactHidden(sid, false); // React shows this child now, so the server may read its data again
        int index = indexOfDelegated(sid);
        if (index >= 0) // the view exists; otherwise the host waits, and addImpl mounts it once the view is built
            attachView(index, host);
    }

    // tell the server whether the React component is showing this delegated child, so a hidden child's data is not read
    // (like collapse / tab activation). Only a real change is sent; a delegated child is shown by default on the server
    private void setReactHidden(String sid, boolean hidden) {
        if (hidden ? reactHidden.add(sid) : reactHidden.remove(sid))
            formController.setUserHidden(findDeclared(sid), hidden);
    }

    private void attachView(int index, Element host) {
        ComponentViewWidget childView = getChildView(index);
        childView.appendTo(host);
        // a delegated child is one self-contained view (a property is forced non-inline in PropertyPanelRenderer, so it is
        // a single widget, not inline value/comment siblings) — getSingleWidget is that view. Stretch it to fill the host
        // with the platform fill-parent-flex(-cont) classes — the same fill a native SizedFlexPanel child gets; the
        // min-size reset those classes omit is added in layout.css.
        GwtClientUtils.setupFlexParent(childView.getSingleWidget().widget.getElement());
        resizeChildren(); // the child moved out of the display:none park into a laid-out slot
    }

    // the host may be an element the component renders for its own layout (useLsfComponent), so the platform marks it
    // rather than expecting the marks
    private void stampHost(Element host, String sid) {
        GwtClientUtils.addClassName(host, "lsf-component");
        host.setAttribute("data-lsf-sid", sid);
    }

    private void clearHost(Element host) {
        GwtClientUtils.removeClassName(host, "lsf-component");
        host.removeAttribute("data-lsf-sid");
    }

    private void unmountComponent(String sid, Element host) {
        if (hosts.get(sid) != host) { // an error / duplicate host, or a remount already moved the child to another host:
            clearDiagnostic(host);    // either way there is nothing of ours to release here
            return;
        }
        hosts.remove(sid);
        if (!unmountingRoot) // a real hide by React, not the form closing (which resets the server's hidden set anyway)
            setReactHidden(sid, true);
        clearHost(host); // the host outlives the mount: it may be the component's own element, or React may reuse it
        int index = indexOfDelegated(sid);
        if (index >= 0) // the view still exists; park it. If it was already dropped (SHOWIF), there is nothing to park
            parkChild(index);
    }

    private int indexOfDelegated(String sid) {
        for (int i = 0, size = children.size(); i < size; i++) {
            GComponent child = children.get(i);
            if (child.isDelegated() && child.sID.equals(sid))
                return i;
        }
        return -1;
    }

    private boolean isDelegatedChild(String sid) {
        GComponent declared = findDeclared(sid);
        return declared != null && declared.isDelegated();
    }

    // a sid that is not a delegated child will never get a view — say WHY. The diagnostic is VISIBLE, not console-only:
    // a typo'd or non-delegated sid must not silently render nothing. Writing into the host is safe: an <LsfComponent>
    // host never renders React children (F2), so React cannot overwrite it.
    private void reportUnknown(String sid, Element host) {
        if (findDeclared(sid) == null) {
            showDiagnostic(host, "'" + sid + "' is not a child of '" + container.sID + "'");
            logError("component '" + sid + "' is not a child of container '" + container.sID + "'");
        } else { // declared, but without delegate = TRUE, so React owns it
            showDiagnostic(host, "'" + sid + "' has no delegate = TRUE");
            logError("child '" + sid + "' of container '" + container.sID + "' has no `delegate = TRUE`: React owns it, so there is no GWT view to place");
        }
    }

    private void reportDuplicate(String sid, Element host) {
        showDiagnostic(host, "'" + sid + "' is already placed by another <LsfComponent>");
        logError("component '" + sid + "' is placed by more than one <LsfComponent>; the first placeholder keeps it");
    }

    private GComponent findDeclared(String sid) {
        for (GComponent child : container.children)
            if (sid.equals(child.sID))
                return child;
        return null;
    }

    private void showDiagnostic(Element host, String message) {
        host.setInnerText("lsFusion: " + message);
        GwtClientUtils.addClassName(host, "lsf-component-error");
    }

    private void clearDiagnostic(Element host) {
        host.setInnerText("");
        GwtClientUtils.removeClassName(host, "lsf-component-error");
        clearHost(host);
    }

    private static native void logError(String message)/*-{ $wnd.console.error("lsFusion CUSTOM REACT: " + message); }-*/;

    public GContainer getContainer() {
        return container;
    }

    // pushed from GFormController.applyRemoteChanges after each form change. A delegated child's caption / captionClass /
    // image now lives inside `data` (data.components, built by GReactFormData): a descriptor change marks the scope dirty,
    // so build() returns a new top ref and the data change alone re-renders React — no separate components channel.
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

    // stable context value: `view` is the per-CONTAINER delegation API (mount/unmount). It cannot live on `controller`,
    // which is form-level and shared by every react container, so it could not resolve which container a sid belongs to.
    private native JavaScriptObject createCtxValue(JavaScriptObject store, JavaScriptObject controller)/*-{
        var reactView = this;
        return {
            store: store,
            controller: controller,
            view: {
                mount: function(sid, host) {
                    reactView.@lsfusion.gwt.client.form.design.view.ReactContainerView::mountComponent(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;)(sid, host);
                },
                unmount: function(sid, host) {
                    reactView.@lsfusion.gwt.client.form.design.view.ReactContainerView::unmountComponent(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;)(sid, host);
                }
            }
        };
    }-*/;

    private native boolean createRoot(Element element)/*-{
        if (!$wnd.React || !$wnd.ReactDOM) {
            $wnd.console.error("lsFusion CUSTOM REACT: window.React / window.ReactDOM are not loaded");
            return false;
        }
        if (!$wnd.lsfusion || !$wnd.lsfusion.__installReactHooks) {
            $wnd.console.error("lsFusion CUSTOM REACT: lsfusion-custom-registry.js is not loaded");
            return false;
        }
        if (!this.@lsfusion.gwt.client.form.design.view.ReactContainerView::resolveComponent()()) {
            $wnd.console.error("lsFusion CUSTOM REACT: component '" + this.@lsfusion.gwt.client.form.design.view.ReactContainerView::componentName + "' not found in registry or on window");
            return false;
        }
        // install the form context + hooks (window.lsfusion) — the Provider + useSelector-style API and the delegation
        // primitives. Idempotent (first caller wins), and done at MOUNT, not at registry load, so it binds the FINAL
        // window.React: an app may override React with a before-system resource after the registry but before mount.
        // A compiled bundle's preamble already ran this before its own body, but a hand-written global gets no preamble
        $wnd.lsfusion.__installReactHooks();
        this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root = $wnd.ReactDOM.createRoot(element);
        return true;
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
            data: this.@lsfusion.gwt.client.form.design.view.ReactContainerView::lastData, // projected @lsfusion/core form state (primary contract; re-rendered each data change); includes data.components (delegated children's descriptors)
            controller: ctxValue.controller // changeProperty/changeProperties/changeObject + exec/eval/evalAction/change
        };
        root.render(React.createElement($wnd.lsfusion.__formContext.Provider, { value: ctxValue }, React.createElement(component, props)));
    }-*/;

    private void unmount() {
        unmountingRoot = true;
        unmountRoot(); // runs the hosts' ref cleanups, which park every mounted child and drop it from `hosts`
        unmountingRoot = false;
        hosts.clear(); // any host left waiting (its view dropped by SHOWIF) belongs to the old tree; drop it too
        reactHidden.clear(); // the server's FormInstance is reset on the next open, so its hidden set starts empty too
    }

    private native void unmountRoot()/*-{
        var root = this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root;
        if (root) {
            root.unmount();
            this.@lsfusion.gwt.client.form.design.view.ReactContainerView::root = null;
        }
    }-*/;
}
