package lsfusion.gwt.client.navigator.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.view.ReactRoot;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.controller.GINavigatorController;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.LinkedHashSet;

// WINDOW ... CUSTOM 'Name': a React component draws this window's elements instead of the standard toolbar. It gets the
// same bucket the standard view renders - the window's elements, which depend on what is selected elsewhere - and the
// navigator controller, whose activate() does what clicking an element does.
public class ReactNavigatorView extends ParkedNavigatorView {

    private final ReactRoot root;
    private JavaScriptObject lastData; // the projection last built, so an unchanged entry can keep its reference

    public ReactNavigatorView(GNavigatorWindow window, GINavigatorController navigatorController) {
        super(window, navigatorController);

        GwtClientUtils.addClassName(panel, "panel-react");

        // the crossing BACK to the platform: the component renders the host node, we fill it with the standard button
        root = new ReactRoot(window.custom, navigatorController.getController(), new ReactRoot.Placement() {
            @Override
            public void mount(String name, Element host, JavaScriptObject row) {
                buttons.mountButton(name, host);
            }

            @Override
            public void unmount(String name, Element host, JavaScriptObject row) {
                buttons.unmountButton(name, host);
            }
        });

        panel.addAttachHandler(event -> {
            if (event.isAttached())
                root.mount(panel.getElement());
            else
                root.unmount();
        });
    }

    @Override
    public void refresh(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected) {
        buttons.refresh(elements, selected); // so that <Lsf name/> has a standard button to place
        root.updateData(buildData(elements, selected));
    }

    // the projection is built from the BUCKET, not from the element tree: a child that crossed into another window, or
    // one gated out because its parent is not selected, is not this window's to draw and must not leak into it
    private JavaScriptObject buildData(LinkedHashSet<GNavigatorElement> elements, GNavigatorElement selected) {
        JavaScriptObject data = createData();
        for (GNavigatorElement element : elements) {
            String image = element.image != null ? element.image.createImageHTML() : null;
            JavaScriptObject entry = createEntry(data, element.canonicalName, element.caption, image, element.lsf,
                    element.elementClass, element.hide, element.isFolder(), element.equals(selected));
            for (GNavigatorElement child : element.children)
                if (elements.contains(child))
                    addChild(entry, child.canonicalName);

            if (!elements.contains(element.parent)) // the same root elements the standard view draws, children hang off them
                addRoot(data, element.canonicalName);
        }
        return lastData = reuse(lastData, data);
    }

    private native JavaScriptObject createData()/*-{
        return { root: [], byName: {} };
    }-*/;

    // structural sharing, the same guarantee the form projection gives: an entry that did not change keeps its
    // reference, so a component memoized on it does not re-render, and a refresh that changed nothing at all returns
    // the very same snapshot, which ReactRoot then skips entirely
    private native JavaScriptObject reuse(JavaScriptObject prev, JavaScriptObject next)/*-{
        if (!prev) return next;

        var sameArray = function (a, b) {
            if (a.length !== b.length) return false;
            for (var i = 0; i < a.length; i++)
                if (a[i] !== b[i]) return false;
            return true;
        };
        // over the entry's OWN keys, so a field added to createEntry cannot be forgotten here - and forgetting one
        // fails silently, by handing back the previous entry for ever
        var sameEntry = function (a, b) {
            var keys = Object.keys(a);
            if (keys.length !== Object.keys(b).length) return false;
            for (var i = 0; i < keys.length; i++) {
                var key = keys[i];
                if (key === 'children' ? !sameArray(a.children, b.children) : a[key] !== b[key]) return false;
            }
            return true;
        };

        var names = Object.keys(next.byName);
        var same = names.length === Object.keys(prev.byName).length;
        for (var i = 0; i < names.length; i++) {
            var name = names[i], before = prev.byName[name];
            if (before && sameEntry(before, next.byName[name]))
                next.byName[name] = before;
            else
                same = false;
        }

        if (!sameArray(prev.root, next.root))
            return next;

        next.root = prev.root; // the order did not change either, so the array itself is worth keeping
        return same ? prev : next;
    }-*/;

    // `name` is on the entry itself, so an entry passed around alone still knows what it is called - `root` and
    // `children` are arrays of names, and without it every consumer had to carry the key beside the entry.
    // An LSF element is DRAWN by the platform, so what it draws is not on the entry at all: it carries only what the
    // component may still want around the button it places, the same reduction an lsf form child gets
    private native JavaScriptObject createEntry(JavaScriptObject data, String canonicalName, String caption, String image,
                                                boolean lsf, String elementClass, boolean hidden, boolean folder, boolean selected)/*-{
        var entry = lsf
                ? { name: canonicalName, caption: caption, image: image, lsf: true, children: [] }
                : { name: canonicalName, caption: caption, image: image, elementClass: elementClass,
                    hidden: hidden, folder: folder, selected: selected, children: [] };
        data.byName[canonicalName] = entry;
        return entry;
    }-*/;

    private native void addChild(JavaScriptObject entry, String canonicalName)/*-{
        entry.children.push(canonicalName);
    }-*/;

    private native void addRoot(JavaScriptObject data, String canonicalName)/*-{
        data.root.push(canonicalName);
    }-*/;

}
