package lsfusion.gwt.client.base.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;

// the store a ReactRoot hands its component's hooks, with no React drawing anything: the root is never mounted, so the
// first push, which would draw the component, draws nothing, and every later one only tells the store's subscribers
public class ReactRootTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    @Override protected void gwtSetUp() { JsniTestSupport.installMapDelete(); }

    private static class NoPlacement implements ReactRoot.Placement {
        public void mount(String name, Element host, JavaScriptObject row) { fail("nothing is placed"); }
        public void unmount(String name, Element host, JavaScriptObject row) { fail("nothing is placed"); }
        public boolean marksHosts() { return false; }
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
}
