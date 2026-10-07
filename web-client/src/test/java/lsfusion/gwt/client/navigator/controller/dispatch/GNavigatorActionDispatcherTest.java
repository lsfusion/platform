package lsfusion.gwt.client.navigator.controller.dispatch;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.junit.client.GWTTestCase;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.navigator.GNavigatorAction;
import lsfusion.gwt.client.navigator.GNavigatorElement;
import lsfusion.gwt.client.navigator.GNavigatorFolder;
import lsfusion.gwt.client.navigator.controller.GNavigatorController;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// activate(canonicalName) of the navigator's controller does what clicking the element does, so it refuses what the
// navigator does not show: an element hidden by its SHOWIF, or one in a folder hidden by its own
public class GNavigatorActionDispatcherTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private final List<GNavigatorElement> activated = new ArrayList<>();
    private GNavigatorController navigator;
    private GNavigatorElement folder, users;

    @Override protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        navigator = new GNavigatorController(null) {
            @Override public void activate(GNavigatorElement element, NativeEvent event) { activated.add(element); }
            @Override public void updateVisibility(Map<GAbstractWindow, Boolean> visibleWindows) { }
        };
        GNavigatorElement root = element(new GNavigatorFolder(), "System.root", null);
        folder = element(new GNavigatorFolder(), "Test.admin", root);
        users = element(new GNavigatorAction(), "Test.users", folder);
        navigator.setRoot(root);
        new GNavigatorActionDispatcher(null, null, navigator); // gives the navigator its controller
    }
    private static GNavigatorElement element(GNavigatorElement element, String canonicalName, GNavigatorElement parent) {
        element.canonicalName = canonicalName;
        element.children = new ArrayList<>();
        element.parent = parent;
        if (parent != null)
            parent.children.add(element);
        return element;
    }
    // the message controller.activate throws, null when it throws nothing
    private static native String activate(JavaScriptObject controller, String canonicalName) /*-{
        try {
            controller.activate(canonicalName);
            return null;
        } catch (e) {
            return e.message;
        }
    }-*/;

    public void testAnElementShownIsActivated() {
        assertNull(activate(navigator.getController(), "Test.users"));
        assertEquals(1, activated.size());
        assertSame(users, activated.get(0));
    }
    public void testAnElementHiddenIsRefused() {
        users.hide = true;
        assertEquals("Navigator element 'Test.users' is hidden", activate(navigator.getController(), "Test.users"));
        assertTrue(activated.isEmpty());
    }
    // the standard views draw no child of a hidden folder (GAbstractNavigatorView.forEachDrawn)
    public void testAnElementOfAHiddenFolderIsRefused() {
        folder.hide = true;
        assertEquals("Navigator element 'Test.users' is hidden, as its folder 'Test.admin' is",
                activate(navigator.getController(), "Test.users"));
        assertTrue(activated.isEmpty());
    }
}
