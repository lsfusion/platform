package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import lsfusion.gwt.client.base.GwtClientUtils;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;

// the store a ReactRoot hands its component's hooks, with no React drawing anything: the root is never mounted, so the
// first push, which would draw the component, draws nothing, and every later one only tells the store's subscribers.
// And what a root dresses a host in, by the kind of host its owner places into (ReactRoot.Host), and that a host it
// hides hides what is in it
public class ReactRootTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() { JsniTestSupport.installMapDelete(); }

    private static class NoPlacement implements ReactRoot.Placement {
        public void mount(String name, Element host, JavaScriptObject row) { fail("nothing is placed"); }
        public void unmount(String name, Element host, JavaScriptObject row) { fail("nothing is placed"); }
        public ReactRoot.Host getHost() { return new ReactRoot.PlainHost(); }
    }

    // #46: a listener that throws - a view's bucketOf, a selector - is its own error, logged where it is thrown, and
    // the listeners after it are told all the same, each reading the new snapshot. The first push tells no one, being
    // what draws the component; a push of the snapshot already there tells no one either; and one unsubscribed is not
    // told again
    public void testEveryListenerIsToldEvenAfterOneThrows() {
        ReactRoot root = new ReactRoot("NoSuchComponent", JavaScriptObject.createObject(), new NoPlacement());
        JavaScriptObject told = subscribeThreeTheMiddleThrowing(root);
        root.updateData(snapshot(1));
        assertEquals("", joined(told));

        JavaScriptObject errors = spyConsoleErrors();
        try {
            JavaScriptObject second = snapshot(2);
            root.updateData(second);
            assertEquals("first 2,third 2", joined(told));
            assertEquals(1, countMatching(errors, "a selector broke"));

            root.updateData(second);
            assertEquals("first 2,third 2", joined(told));

            unsubscribeFirst(told);
            root.updateData(snapshot(3));
            assertEquals("first 2,third 2,third 3", joined(told));
            assertEquals(2, countMatching(errors, "a selector broke"));
        } finally {
            restoreConsole(errors);
        }
    }

    private static native JavaScriptObject snapshot(int n) /*-{
        return { n: n };
    }-*/;

    // each listener told writes down its name and the snapshot it reads then; the list keeps the first one's
    // unsubscribe
    private static native JavaScriptObject subscribeThreeTheMiddleThrowing(ReactRoot root) /*-{
        var told = [], store = root.@lsfusion.gwt.client.base.view.ReactRoot::store;
        told.unsubscribeFirst = store.subscribe(function () { told.push('first ' + store.getSnapshot().n); });
        store.subscribe(function () { throw new Error('a selector broke'); });
        store.subscribe(function () { told.push('third ' + store.getSnapshot().n); });
        return told;
    }-*/;
    private static native void unsubscribeFirst(JavaScriptObject told) /*-{ told.unsubscribeFirst(); }-*/;
    private static native String joined(JavaScriptObject told) /*-{ return told.join(','); }-*/;

    private static native JavaScriptObject spyConsoleErrors() /*-{
        var log = [], original = $wnd.console.error;
        $wnd.console.error = function () { log.push(Array.prototype.join.call(arguments, ' ')); };
        log.restore = function () { $wnd.console.error = original; };
        return log;
    }-*/;
    private static native void restoreConsole(JavaScriptObject log) /*-{ log.restore(); }-*/;
    private static native int countMatching(JavaScriptObject log, String part) /*-{
        return log.filter(function (message) { return message.indexOf(part) >= 0; }).length;
    }-*/;

    // a host a view of the platform's is put into is marked so: the class the layout fills it by, merged with the
    // component's own - asked at every render, so a class that changes keeps the mark -, and the name it holds
    public void testAHostAViewGoesIntoIsMarked() {
        ReactRoot.Host host = new ReactRoot.MarkedHost();
        JavaScriptObject panel = host.props("BOX(o)", "panel"), wide = host.props("BOX(o)", "panel panel-wide");
        assertEquals("lsf-platform panel", text(panel, "className"));
        assertEquals("lsf-platform panel panel-wide", text(wide, "className"));
        assertEquals("BOX(o)", text(wide, "data-lsf-sid"));
        assertEquals("lsf-platform", text(host.props("x", null), "className")); // no class of its own: still marked
    }

    // ... and one a component lays out itself is left as it rendered it: its own class, and no name
    public void testAHostTheComponentLaysOutIsLeftAsItIs() {
        ReactRoot.Host host = new ReactRoot.PlainHost();
        JavaScriptObject menu = host.props("Sale.sales", "menu-main");
        assertEquals("menu-main", text(menu, "className"));
        assertFalse(has(menu, "data-lsf-sid"));
        assertFalse(has(host.props("Sale.sales", null), "className")); // no class given: none, not a null one
    }

    // ... and a view in a host the component hides - <Platform> puts `hidden` on it while the entry of what it holds
    // says so - is not shown, as one the platform hides is not: the platform does not hide a view a React view places,
    // so what it takes only from a view that is shown - a key of a filter group's widget - goes with the host
    public void testAViewInAHostTheComponentHidesIsNotShown() {
        FlowPanel host = new FlowPanel(); // the host a <Platform> renders, the view of the platform's put into it
        Label view = new Label("view");
        host.add(view);
        RootPanel.get().add(host);
        try {
            assertTrue(GwtClientUtils.isShowing(view));
            host.getElement().setAttribute("hidden", "");
            assertFalse(GwtClientUtils.isShowing(view));
            host.getElement().removeAttribute("hidden");
            assertTrue(GwtClientUtils.isShowing(view));
        } finally {
            RootPanel.get().remove(host);
        }
    }

    private static native String text(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    private static native boolean has(JavaScriptObject object, String name) /*-{ return name in object; }-*/;
}
