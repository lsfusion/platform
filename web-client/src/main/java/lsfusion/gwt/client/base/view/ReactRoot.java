package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.GwtClientUtils;

// the React root drawing an application component, with the store its descendants subscribe to - or, where a React
// view of a form places the component, what draws it in that view's tree instead (settle). `host` is NOT this:
// a host is the node <Platform name/> renders and the platform moves a view into, which is why this is a root. Everything here is about
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
    // where the component is drawn, between mount and unmount: a React root of its own, or the embedder of the host
    // whose view draws it in its tree (settle)
    private JavaScriptObject root;
    private JavaScriptObject embedder;
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
        this.context = createContext(store, controller, placement, placement.getHost(), this);
    }

    // where a placed view goes and where it is taken back from: <Platform name/> renders the node, the owner moves its
    // own view into it and takes it back out. `row` names WHICH host of a per-row renderer, and is null wherever there
    // is only one. One interface with both halves rather than two of one method each, so that a call site cannot read
    // as "the placer does something" without saying which way it goes
    public interface Placement {
        void mount(String name, Element host, JavaScriptObject row);
        void unmount(String name, Element host, JavaScriptObject row);

        // what its hosts are dressed in, besides the ref and `hidden` the hook gives each (usePlatform): the kind of
        // host it places into - a form container, the forms window and the window layout put a view into theirs, which
        // the layout fills them with by their marks (MarkedHost); the navigator and the log windows leave theirs as
        // the component rendered them (PlainHost)
        Host getHost();

        // the entry a placed name keys in the owner's `data` - what hides its host while it says `hidden`
        // (usePlatform): the field of that name, unless the owner keys what it places otherwise - a form container keys
        // a platform-drawn panel property's entry by its integration name on its group's node
        default JavaScriptObject entryOf(JavaScriptObject data, String name) {
            return GwtClientUtils.getOwnField(data, name);
        }

        // whether the component shows a container it draws itself (useShown): only a form container's view draws any -
        // the server reads what is inside one only while it is shown -, any other owner has none to show or hide
        default void show(String name, boolean shown) {
            GwtClientUtils.logCustomViewError("useShown('" + name + "') does nothing here: only the view of a form's"
                    + " container draws containers of its own to show or hide");
        }

        // whether a React component the platform draws inside what a host holds is drawn in this root's tree, as a
        // part of the view that places it, rather than in a root of its own (settle). A form's view: what it does not
        // place it does not show, and the server does not read - there is nothing of it to keep while it has no
        // place. An open form or a window stays what it is while no place shows it, with what its components hold,
        // so the forms window, the window layout and a navigator window embed nothing
        default boolean embeds() {
            return false;
        }

        // the component's tree is there, and it is gone - told by React as it commits either, whichever tree the
        // component is drawn in: one a view embeds goes as its host does, by no call of the owner's
        default void drawn() {
        }
        default void gone() {
        }
    }

    // ... and the placement of a root whose component is given nothing to place - a property's cell, the CUSTOM view
    // of a group's rows: a <Platform> in it says so in its host
    public static Placement noPlacement(String owner) {
        return new Placement() {
            private final Host host = new PlainHost();

            @Override
            public void mount(String name, Element element, JavaScriptObject row) {
                GwtClientUtils.showCustomViewError(element, owner + " cannot place '" + name + "'");
            }

            @Override
            public void unmount(String name, Element element, JavaScriptObject row) {
                GwtClientUtils.clearCustomViewError(element);
            }

            @Override
            public Host getHost() {
                return host;
            }
        };
    }

    // A HOST's props besides its ref and `hidden`, which the hook adds (usePlatform): the class the component gives it,
    // and what the kind of host adds to it. Props of the host's own, rendered by <Platform> / usePlatform, rather than
    // anything written into the node behind React's back: a class written into a node React renders is lost the moment
    // the component's own class for it changes
    public abstract static class Host {
        public abstract JavaScriptObject props(String name, String className);
    }
    // ... one a view of the platform's is put into: marked so - the lsf-platform class, which the layout fills the host
    // by (a flex column, with the min-size reset a moved view needs), merged with the component's own, and
    // data-lsf-sid, which a stylesheet addresses the host by
    public static final class MarkedHost extends Host {
        public native JavaScriptObject props(String name, String className) /*-{
            return { className: className ? 'lsf-platform ' + className : 'lsf-platform', 'data-lsf-sid': name };
        }-*/;
    }
    // ... one left as the component rendered it: the class it gives it alone, none where it gives none
    public static final class PlainHost extends Host {
        public native JavaScriptObject props(String name, String className) /*-{
            return className == null ? {} : { className: className };
        }-*/;
    }

    // `element` is the node the root is mounted in, set at mount: what the component draws is inside it, and nothing a
    // view does may reach above it - everything there belongs to the platform or to another view
    private static native JavaScriptObject createContext(JavaScriptObject store, JavaScriptObject controller, Placement placement, Host host, ReactRoot root)/*-{
        return { store: store, controller: controller, element: null, view: {
            embeds: placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::embeds()(),
            drawn: function() {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::drawn()();
            },
            gone: function() {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::gone()();
            },
            mount: function(name, host, row) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::mount(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            unmount: function(name, host, row) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::unmount(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            check: function(name, host, row) {
                root.@lsfusion.gwt.client.base.view.ReactRoot::checkPlaced(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            },
            entryOf: function(data, name) {
                return placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::entryOf(Lcom/google/gwt/core/client/JavaScriptObject;Ljava/lang/String;)(data, name);
            },
            show: function(name, shown) {
                placement.@lsfusion.gwt.client.base.view.ReactRoot.Placement::show(Ljava/lang/String;Z)(name, !!shown);
            },
            host: function(name, className) {
                return host.@lsfusion.gwt.client.base.view.ReactRoot.Host::props(Ljava/lang/String;Ljava/lang/String;)(name, className === undefined ? null : className);
            } } };
    }-*/;

    // a host is judged only once the commit that rendered it is over, and the registry says when that is. At the moment
    // the ref arrives, a legitimate portal's container is not in the page yet - refusing there refuses those portals,
    // which was written, measured and reverted - and by the time the commit and its effects are done, a host that is
    // still outside the document is one nothing will ever show. Then the placement is undone exactly as React's own
    // cleanup would undo it, through the owner: the view goes back to where it waits instead of living on in a node
    // that has left the page, and the name is free for a place that IS in it. One check for every surface, since every
    // one of them draws through this root - the form container and its per-row editors, the navigator window, the
    // forms window, the log and the window layout
    private void checkPlaced(String name, Element host, JavaScriptObject row) {
        if (GwtClientUtils.isInDocument(host))
            return;

        GwtClientUtils.logCustomViewError("'" + name + "' was placed in a <Platform> that is not in the page, so nothing put"
                + " there would be seen; the place is given up and the view goes back to waiting");
        placement.unmount(name, host, row);
    }

    // WHERE the component is drawn is where its node is, asked as it is mounted and each time the platform moves
    // the view the node is in: inside a host that embeds what it holds - a <Platform> of a form's view - in the tree of that
    // view, as a portal of the host into this node (lsfusion-custom-registry.js, makeEmbedder), and taken out of it as
    // the node leaves the host; in a form's park, where what no host holds waits unseen, nowhere; anywhere else in a
    // React root of its own. A root it keeps wherever its node goes next: React clears the node of a root whenever it
    // renders that root with nothing in it - an unmounted one too, if anything was told to it while it was taken
    // down -, so the node of a root is never handed to another tree.
    // The place is taken as soon as there is one, but the component is drawn only once there is data too. `sync`:
    // commit that first render of a root inside this call instead of in a later task. Only an owner whose window is
    // MEASURED when it is shown needs it - a -3 main container is fixed to whatever can be measured then, and an
    // asynchronous commit leaves it measuring an empty host; what a host embeds is drawn as the commit that placed it
    // ends, within the same call of the view around it. The navigator, the forms window, the log and the window
    // layout are never measured and stay asynchronous. Later updates always go through the store, asynchronously,
    // either way
    public void settle(Element element, boolean sync) {
        JavaScriptObject place = getEmbedder(element);
        if (root != null || place != null && place == embedder)
            return;

        unmount();
        if (!prepare(element))
            return;
        if (place == null)
            root = createRoot(element);
        else
            embedder = place;
        if (lastData != null)
            render(sync);
    }

    // the embedder the nearest node above that says one says, null where none does - and where what says is the
    // form, which says none
    private static native JavaScriptObject getEmbedder(Element element)/*-{
        for (var node = element.parentNode; node; node = node.parentNode)
            if (node.lsfEmbedder !== undefined)
                return node.lsfEmbedder;
        return null;
    }-*/;

    // a form's node and its park in it (GFormLayout): what waits in the park for a place is not shown, so a component
    // is not drawn there. The form itself says no embedder: a component is drawn in the tree of a view of its own
    // form only, and whatever holds the form - a container or a cell of another form that a view of that form
    // places - what no view of its own places is drawn in a root, when it is first mounted and when it is made later
    public static native void setPark(Element form, Element park)/*-{
        form.lsfEmbedder = null;
        park.lsfEmbedder = { add: function() {}, remove: function() {} };
    }-*/;

    // ... asked again, for a component of a form's, as the platform moves one of the form's views from the park into
    // a place, or back (GFormLayout.viewMoved) - with no `sync`: a host moves the view in while React is committing,
    // and what is measured as it is shown is drawn as it is attached
    public void settle() {
        Element element = getElement();
        if (element != null)
            settle(element, false);
    }
    private native Element getElement()/*-{
        return this.@ReactRoot::context.element;
    }-*/;

    // drawn in the panel while it is attached: mounted as it is attached, unmounted as it is detached - for an owner
    // that is never measured, so the first render stays asynchronous
    public void mountWhileAttached(Widget panel) {
        panel.addAttachHandler(event -> {
            if (event.isAttached())
                settle(panel.getElement(), false);
            else
                unmount();
        });
    }

    // a component that cannot be drawn says so in the element the window or the container would have filled, not only
    // in the console - the same rule a placement mistake follows. Without it a whole window goes blank with nothing on
    // the page saying why, and for the forms window that is the whole application: every open form sits in the park
    private void cannotDraw(Element element, String shown, String logged) {
        GwtClientUtils.showCustomViewError(element, shown);
        GwtClientUtils.logCustomViewError(logged);
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

    // structural sharing for an owner that builds its projection anew each time - the navigator window, the forms
    // window - and keeps the snapshot it built last. Whatever did not change keeps its reference, so a component
    // memoized on an entry that did not change does not re-render, and a push that changed nothing at all is the very
    // same snapshot, which is then skipped. The form views' projection (GReactFormData) shares row by row itself and is
    // no tree (a row is in `list` and in `byKey`), so it never goes through this pass.
    // over the OWN keys, at every level, so a field an owner adds to its entries cannot be forgotten here - and
    // forgetting one fails silently, by handing back the previous value for ever. `next` is a new build nobody has
    // seen yet, so what did not change is put into it in place.
    // Only an array and a plain object are what their keys say, and only they are looked into. Anything else is a value
    // of its own, and says which itself (valueOf): the last one where it is of the same class and answers the same - a
    // date, which a build makes anew each time and which carries its moment in no key at all - and, answering nothing
    // but itself, only where it is that very object: an object of the client's own (a row's key,
    // GGroupObjectValue.setRowObjects), whose fields are not a snapshot's to compare, still less to write.
    // A plain object is told by what it is and not by its window's Object: the client's code runs in a window of its
    // own, and a snapshot carries the plain objects both windows made (a JSON the client parsed, an object of the page's)
    public static native JavaScriptObject reuse(JavaScriptObject prev, JavaScriptObject next)/*-{
        // a plain object's prototype is some window's Object.prototype, which has none above it, or none at all - as
        // the prototype of no class is, a class's being an object itself
        var isPlain = function (value) {
            if (Array.isArray(value))
                return true;
            var prototype = Object.getPrototypeOf(value);
            return prototype === null || Object.getPrototypeOf(prototype) === null;
        };
        var reuse = function (prev, next) {
            if (prev === next || prev === null || next === null || typeof prev !== 'object' || typeof next !== 'object')
                return next;
            var plain = isPlain(prev);
            if (plain !== isPlain(next) || Array.isArray(prev) !== Array.isArray(next))
                return next;
            if (!plain)
                return Object.getPrototypeOf(prev) === Object.getPrototypeOf(next) && prev.valueOf() === next.valueOf()
                        ? prev : next;

            // the same keys, and an array's length too - a hole has no key
            var keys = Object.keys(next), same = keys.length === Object.keys(prev).length
                    && (!Array.isArray(next) || prev.length === next.length);
            for (var i = 0; i < keys.length; i++) {
                var key = keys[i], own = Object.prototype.hasOwnProperty.call(prev, key);
                var value = next[key] = reuse(own ? prev[key] : undefined, next[key]);
                if (!own || value !== prev[key])
                    same = false;
            }
            return same ? prev : next;
        };
        return reuse(prev, next);
    }-*/;

    // whether the CUSTOM of a property or of an object group names a React component rather than a classic render
    // function: one registered as a component - lsfusion.custom.register(name, component, 'reactView'). The name
    // itself does not say it there, as it does for the `custom` of a container or a window (the server's
    // Custom.isReactComponent): a classic function may be named as components are
    public static native boolean isReactView(String name) /*-{
        var registry = $wnd.lsfusion && $wnd.lsfusion.custom;
        return name != null && !!(registry && registry.isReactView && registry.isReactView(name));
    }-*/;

    private JavaScriptObject findComponent() {
        return GwtClientUtils.getGlobalField(componentName, "reactView", true);
    }

    private static native JavaScriptObject createRoot(Element element)/*-{
        return $wnd.ReactDOM.createRoot(element);
    }-*/;

    private native boolean prepare(Element element)/*-{
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
        // marks an element a React component draws in
        element.classList.add('panel-react');
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

    // rendered ONCE per mount, when the place is there and so is the first snapshot: what is mounted is the platform's
    // own root, which subscribes to the store and hands the projection down as props.data, so every later change is one
    // notification and one React pass. The boundary sits inside that root and around the application's component - a
    // component that throws while drawing leaves the reason in the window instead of tearing the root down and leaving
    // it blank. The same element whichever tree it is drawn in: embedded, the component still reads its own store and
    // controller, and its error stays its own
    private native void render(boolean sync)/*-{
        var root = this.@ReactRoot::root, embedder = this.@ReactRoot::embedder;
        if (!root && !embedder) return;
        var React = $wnd.React;
        var context = this.@ReactRoot::context;
        var element = React.createElement($wnd.lsfusion.__context.Provider, { value: context },
            React.createElement($wnd.lsfusion.__root, {
                name: this.@ReactRoot::componentName,
                component: this.@ReactRoot::component,
                controller: context.controller
            }));
        // commit the initial render before the window measures its content. It is legal here because a root is mounted
        // from a GWT attach handler, never from inside another root's render or effect (placing a platform-drawn child
        // is a DOM move, not an attach) - React would otherwise warn and silently fall back to an asynchronous commit
        if (embedder)
            embedder.add(context.element, element);
        else if (sync && $wnd.ReactDOM.flushSync)
            $wnd.ReactDOM.flushSync(function() { root.render(element); });
        else
            root.render(element);
    }-*/;

    public native void unmount()/*-{
        var root = this.@ReactRoot::root, embedder = this.@ReactRoot::embedder;
        if (root) {
            root.unmount();
            this.@ReactRoot::root = null;
        }
        if (embedder) {
            embedder.remove(this.@ReactRoot::context.element);
            this.@ReactRoot::embedder = null;
        }
    }-*/;
}
