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
    // null until the owner's first push. The component is drawn from that push and never before it: every shape the
    // projection promises is there from the first draw, so a component reads it without guarding a missing snapshot -
    // and a snapshot made up here to draw from would have none of them. So an owner whose EMPTY state means something
    // (no form open, no message logged) pushes that state, correctly shaped, when it is built, and one whose first
    // state comes from elsewhere (the form's first changes, the navigator's first update) is drawn when it arrives
    private JavaScriptObject lastData;

    private final Placement placement;

    public ReactRoot(String componentName, JavaScriptObject controller, Placement placement) {
        this.componentName = componentName;
        this.placement = placement;
        this.store = createStore();
        this.context = createContext(store, controller, placement, placement.marksHosts(), this);
    }

    // where a placed view goes and where it is taken back from: <Lsf name/> renders the node, the owner moves its own
    // view into it and takes it back out. `row` names WHICH host of a per-row renderer, and is null wherever there is
    // only one. One interface with both halves rather than two of one method each, so that a call site cannot read as
    // "the placer does something" without saying which way it goes
    public interface Placement {
        void mount(String name, Element host, JavaScriptObject row);
        void unmount(String name, Element host, JavaScriptObject row);

        // whether a host is marked as holding one of the platform's views: the lsf-view class, which the layout fills
        // the host by - a flex column, with the min-size reset a moved view needs -, and data-lsf-sid, which a
        // stylesheet addresses the host by. <Lsf> renders the marks itself, as props of its own, since a class written
        // into a node React renders is lost the moment the component's own class for it changes. Each owner says it: a
        // form container and the forms window mark theirs, which hold a view the layout fills them with; the navigator
        // and the log windows leave theirs as the component rendered them
        boolean marksHosts();
    }

    // `element` is the node the root is mounted in, set at mount: what the component draws is inside it, and nothing a
    // view does may reach above it - everything there belongs to the platform or to another view
    private static native JavaScriptObject createContext(JavaScriptObject store, JavaScriptObject controller, Placement placement, boolean marks, ReactRoot root)/*-{
        return { store: store, controller: controller, element: null, view: {
            mount: function(name, host, row) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::mount(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            unmount: function(name, host, row) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::unmount(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            check: function(name, host, row) {
                root.@lsfusion.gwt.client.base.view.ReactRoot::checkPlaced(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            marks: marks } };
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

    // the root is created as soon as there is a place for it, but the component is drawn only once there is data too -
    // and says whether it drew now. `sync`: commit that first render inside this call instead of in a later task. Only
    // an owner whose window is MEASURED when it is shown needs it - a -3 main container is fixed to whatever can be
    // measured then, and an asynchronous commit leaves it measuring an empty host. The navigator, the forms window and
    // the log are never measured and stay asynchronous. Later updates always go through the store, asynchronously,
    // either way
    public boolean mount(Element element, boolean sync) {
        if (createRoot(element) && lastData != null) {
            render(sync);
            return true;
        }
        return false;
    }

    // a component that cannot be drawn says so in the element the window or the container would have filled, not only
    // in the console - the same rule a placement mistake follows. Without it a whole window goes blank with nothing on
    // the page saying why, and for the forms window that is the whole application: every open form sits in the park
    private void cannotDraw(Element element, String shown, String logged) {
        GwtClientUtils.showLsfViewError(element, shown);
        GwtClientUtils.logLsfViewError(logged);
    }

    // ONE push for the whole update. props.data reaches the component through the platform's own root, which reads this
    // same store, so it is a subscriber like any selector hook and this single notification queues all of them together
    // - one React pass. Rendering the root again here as well was a second pass React could not merge with the first,
    // a store notification and a root.render being scheduled on different lanes.
    // The FIRST push is the exception: nothing has been drawn yet, so nothing subscribes, and it is what draws the
    // component - here if the root is mounted already, at mount otherwise
    public void updateData(JavaScriptObject data) {
        if (data == lastData)
            return;
        boolean first = lastData == null;
        lastData = data;
        if (first)
            render(false);
        else
            notifyStore();
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
        this.@ReactRoot::context.element = element;
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
            _notify: function() { // each listener on its own: one that throws - a view's bucketOf, a selector - is its
                listeners.forEach(function(listener) { // own error, logged where it is thrown, and the next one is told
                    try {
                        listener();
                    } catch (e) {
                        $wnd.console.error(e);
                    }
                });
            }
        };
    }-*/;

    private native void notifyStore()/*-{
        this.@ReactRoot::store._notify();
    }-*/;

    // rendered ONCE per mount, when the root is there and so is the first snapshot: what is mounted is the platform's
    // own root, which subscribes to the store and hands the projection down as props.data, so every later change is one
    // notification and one React pass. The boundary sits inside that root and around the application's component - a
    // component that throws while drawing leaves the reason in the window instead of tearing the root down and leaving
    // it blank
    private native void render(boolean sync)/*-{
        var root = this.@ReactRoot::root;
        if (!root) return;
        var React = $wnd.React;
        var context = this.@ReactRoot::context;
        var element = React.createElement($wnd.lsfusion.__context.Provider, { value: context },
            React.createElement($wnd.lsfusion.__root, {
                name: this.@ReactRoot::componentName,
                component: this.@ReactRoot::component,
                controller: context.controller
            }));
        // commit the initial render before the window measures its content. It is legal here because a root is mounted
        // from a GWT attach handler, never from inside another root's render or effect (placing an lsf child is a DOM
        // move, not an attach) - React would otherwise warn and silently fall back to an asynchronous commit
        if (sync && $wnd.ReactDOM.flushSync)
            $wnd.ReactDOM.flushSync(function() { root.render(element); });
        else
            root.render(element);
    }-*/;





    public native void unmount()/*-{
        var root = this.@ReactRoot::root;
        if (root) {
            root.unmount();
            this.@ReactRoot::root = null;
        }
    }-*/;
}
