package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;

// a React root drawing an application component, with the store its descendants subscribe to. Everything here is about
// React and nothing about what is being drawn, so a form container and a navigator window share it and differ only in
// the controller and placers they hand it and the data they push.
public class ReactHost {

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

    public ReactHost(String componentName, JavaScriptObject controller, Placer mount, Placer unmount) {
        this.componentName = componentName;
        this.store = createStore();
        this.context = createContext(store, controller, mount, unmount);
    }

    // where a placed view goes and where it is taken back from: <Lsf name/> renders the node, the owner moves its own
    // view into it. `row` names WHICH host of a per-row renderer, and is null wherever there is only one
    public interface Placer {
        void place(String name, Element host, JavaScriptObject row);
    }

    private static native JavaScriptObject createContext(JavaScriptObject store, JavaScriptObject controller, Placer mount, Placer unmount)/*-{
        var place = function(placer) {
            return function(name, host, row) {
                placer.@lsfusion.gwt.client.base.view.ReactHost.Placer::place(Ljava/lang/String;Lcom/google/gwt/dom/client/Element;Lcom/google/gwt/core/client/JavaScriptObject;)(name, host, row || null);
            };
        };
        return { store: store, controller: controller, view: { mount: place(mount), unmount: place(unmount) } };
    }-*/;

    public void mount(Element element) {
        if (createRoot(element))
            render();
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
        if (!$wnd.React || !$wnd.ReactDOM) {
            @lsfusion.gwt.client.base.GwtClientUtils::logLsfViewError(Ljava/lang/String;)("window.React / window.ReactDOM are not loaded");
            return false;
        }
        if (!$wnd.lsfusion || !$wnd.lsfusion.__installReactHooks) {
            @lsfusion.gwt.client.base.GwtClientUtils::logLsfViewError(Ljava/lang/String;)("lsfusion-custom-registry.js is not loaded");
            return false;
        }
        this.@ReactHost::component = this.@ReactHost::findComponent()();
        if (!this.@ReactHost::component) {
            @lsfusion.gwt.client.base.GwtClientUtils::logLsfViewError(Ljava/lang/String;)("component '" + this.@ReactHost::componentName + "' is not registered and is not on window");
            return false;
        }
        // install the context + hooks (window.lsfusion) — the Provider + useSelector-style API and the delegation
        // primitives. Idempotent (first caller wins), and done at MOUNT, not at registry load, so it binds the FINAL
        // window.React: an app may override React with a before-system resource after the registry but before mount.
        // A compiled bundle's preamble already ran this before its own body, but a hand-written global gets no preamble
        $wnd.lsfusion.__installReactHooks();
        this.@ReactHost::root = $wnd.ReactDOM.createRoot(element);
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
                return host.@ReactHost::lastData;
            },
            _notify: function() {
                listeners.forEach(function(listener) {
                    listener();
                });
            }
        };
    }-*/;

    private native void notifyStore()/*-{
        this.@ReactHost::store._notify();
    }-*/;

    private native void render()/*-{
        var root = this.@ReactHost::root;
        if (!root) return;
        var React = $wnd.React;
        var component = this.@ReactHost::component;
        var context = this.@ReactHost::context;
        var props = {
            data: this.@ReactHost::lastData, // the projection (primary contract, re-rendered each data change); every projected thing is an entry in it, keyed as it is keyed
            controller: context.controller
        };
        root.render(React.createElement($wnd.lsfusion.__context.Provider, { value: context }, React.createElement(component, props)));
    }-*/;

    public native void unmount()/*-{
        var root = this.@ReactHost::root;
        if (root) {
            root.unmount();
            this.@ReactHost::root = null;
        }
    }-*/;
}
