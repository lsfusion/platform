package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;

// the React root drawing an application component, with the store its descendants subscribe to. `host` is NOT this:
// a host is the node <Lsf name/> renders and the platform moves a view into, which is why this is a root. Everything here is about
// React and nothing about what is being drawn, so every window and container that a component draws shares it, and they
// differ only in the controller and the placement they hand it and the data they push.
public class ReactRoot {

    private final String componentName;
    // the store is DOM-independent and created once, so it survives re-mounts and can be fed before the first one
    private final JavaScriptObject store;
    // built once, here rather than in each owner: React re-renders every consumer when the Provider's value changes
    // identity, so it must not be rebuilt - and the shape is the contract the window.lsfusion hooks read, so an owner
    // that wrote its own could drift from them without anything saying so
    private final JavaScriptObject context;
    private JavaScriptObject component; // resolved once, so React sees the same component type on every render
    private JavaScriptObject root;
    // starts empty, so nothing downstream ever has to guard a missing snapshot
    private JavaScriptObject lastData = JavaScriptObject.createObject();

    private final Placement placement;

    public ReactRoot(String componentName, JavaScriptObject controller, Placement placement) {
        this.componentName = componentName;
        this.placement = placement;
        this.store = createStore();
        this.context = createContext(store, controller, placement, this);
    }

    // where a placed view goes and where it is taken back from: <Lsf name/> renders the node, the owner moves its own
    // view into it and takes it back out. `row` names WHICH host of a per-row renderer, and is null wherever there is
    // only one. One interface with both halves rather than two of one method each, so that a call site cannot read as
    // "the placer does something" without saying which way it goes
    public interface Placement {
        void mount(String name, Element host, JavaScriptObject row);
        void unmount(String name, Element host, JavaScriptObject row);
    }

    private static native JavaScriptObject createContext(JavaScriptObject store, JavaScriptObject controller, Placement placement, ReactRoot root)/*-{
        return { store: store, controller: controller, view: {
            mount: function(name, host, row) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::mount(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            unmount: function(name, host, row) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::unmount(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            check: function(name, host, row) {
                root.@lsfusion.gwt.client.base.view.ReactRoot::checkPlaced(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            } } };
    }-*/;

    // a host is judged only once the commit that rendered it is over, and the registry says when that is. At the moment
    // the ref arrives, a legitimate portal's container is not in the page yet - refusing there refuses those portals,
    // which was written, measured and reverted - and by the time the commit and its effects are done, a host that is
    // still outside the document is one nothing will ever show. Then the placement is undone exactly as React's own
    // cleanup would undo it, through the owner: the view goes back to where it waits instead of living on in a node
    // that has left the page, and the name is free for a place that IS in it. One check for every surface, since every
    // one of them draws through this root - the form container and its per-row editors, the navigator window, the
    // forms window and the log
    private void checkPlaced(String name, Element host, JavaScriptObject row) {
        if (GwtClientUtils.isInDocument(host))
            return;

        GwtClientUtils.logLsfViewError("'" + name + "' was placed in an <Lsf> that is not in the page, so nothing put"
                + " there would be seen; the place is given up and the view goes back to waiting");
        placement.unmount(name, host, row);
    }

    public void mount(Element element) {
        if (createRoot(element))
            render();
    }

    // a component that cannot be drawn says so in the element the window or the container would have filled, not only
    // in the console - the same rule a placement mistake follows. Without it a whole window goes blank with nothing on
    // the page saying why, and for the forms window that is the whole application: every open form sits in the park
    private void cannotDraw(Element element, String shown, String logged) {
        GwtClientUtils.showLsfViewError(element, shown);
        GwtClientUtils.logLsfViewError(logged);
    }

    public void updateData(JavaScriptObject data) {
        if (data == lastData)
            return;
        lastData = data;
        notifyStore(); // fine-grained subscribers (useData selectors)
        render();      // props.data
    }

    private JavaScriptObject findComponent() {
        return GwtClientUtils.getGlobalField(componentName, "reactView", true);
    }

    private native boolean createRoot(Element element)/*-{
        var name = this.@ReactRoot::componentName;
        if (!$wnd.React || !$wnd.ReactDOM) {
            this.@ReactRoot::cannotDraw(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;Ljava/lang/String;)(element,
                "React is not loaded", "'" + name + "' cannot be drawn: window.React / window.ReactDOM are not loaded");
            return false;
        }
        if (!$wnd.lsfusion || !$wnd.lsfusion.__installReactHooks) {
            this.@ReactRoot::cannotDraw(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;Ljava/lang/String;)(element,
                "the lsFusion registry is not loaded", "'" + name + "' cannot be drawn: lsfusion-custom-registry.js is not loaded");
            return false;
        }
        var component = this.@ReactRoot::findComponent()();
        // a name is looked up in the registry first and on window after it, so that a hand-written global keeps
        // working - and window carries names of its own. Anything that is not a component React can draw would be
        // handed to React and fail there instead, sometimes past the boundary and into a dialog naming nothing
        if (!component || (typeof component !== 'function' && !component.$$typeof)) {
            this.@ReactRoot::cannotDraw(Lcom/google/gwt/dom/client/Element;Ljava/lang/String;Ljava/lang/String;)(element,
                "'" + name + "' is not a registered component",
                "'" + name + "' is not a component: it is not in the registry, and what window carries under that name is "
                    + (component ? "not something React can draw" : "nothing"));
            return false;
        }
        this.@ReactRoot::component = component;
        // install the context + hooks (window.lsfusion) — the Provider + useSelector-style API and the delegation
        // primitives. Idempotent (first caller wins), and done at MOUNT, not at registry load, so it binds the FINAL
        // window.React: an app may override React with a before-system resource after the registry but before mount.
        // A compiled bundle's preamble already ran this before its own body, but a hand-written global gets no preamble
        $wnd.lsfusion.__installReactHooks();
        this.@ReactRoot::root = $wnd.ReactDOM.createRoot(element);
        return true;
    }-*/;

    // the hook snapshot IS the projected data object itself: lastData only changes ref when the data changed, and
    // structural sharing keeps unchanged subtrees reference-equal — exactly what useSyncExternalStore selectors need.
    // (selector hooks fit this immutable-snapshot model; a mutable-proxy useSnapshot would be the wrong fit.)
    private native JavaScriptObject createStore()/*-{
        var host = this;
        var listeners = new $wnd.Set();
        return {
            subscribe: function(listener) {
                listeners.add(listener);
                return function() {
                    listeners['delete'](listener);
                };
            },
            getSnapshot: function() {
                return host.@ReactRoot::lastData;
            },
            _notify: function() {
                listeners.forEach(function(listener) {
                    listener();
                });
            }
        };
    }-*/;

    private native void notifyStore()/*-{
        this.@ReactRoot::store._notify();
    }-*/;

    private native void render()/*-{
        var root = this.@ReactRoot::root;
        if (!root) return;
        var React = $wnd.React;
        var component = this.@ReactRoot::component;
        var context = this.@ReactRoot::context;
        var props = {
            data: this.@ReactRoot::lastData, // the projection (primary contract, re-rendered each data change); every projected thing is an entry in it, keyed as it is keyed
            controller: context.controller
        };
        // the boundary sits INSIDE the Provider and around the application's component, so a component that throws
        // while drawing leaves the reason in the window instead of tearing the root down and leaving it blank
        root.render(React.createElement($wnd.lsfusion.__context.Provider, { value: context },
            React.createElement($wnd.lsfusion.__boundary, { name: this.@ReactRoot::componentName, data: props.data },
                React.createElement(component, props))));
    }-*/;

    public native void unmount()/*-{
        var root = this.@ReactRoot::root;
        if (root) {
            root.unmount();
            this.@ReactRoot::root = null;
        }
    }-*/;
}
