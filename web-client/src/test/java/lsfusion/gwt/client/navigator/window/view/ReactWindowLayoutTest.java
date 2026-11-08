package lsfusion.gwt.client.navigator.window.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.GNavigatorWindow;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

// NAVIGATOR CUSTOM: the windows laid out by a React component - the entries it is given, and a window placed in a host
// and parked again. No React draws anything here, HtmlUnit has none: the root is never mounted, and the component's
// side is played through the context a <Platform> works with - the store it reads and the view it places with
public class ReactWindowLayoutTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    // a window's view as its owner builds it, counting what the layout tells it
    private static class View extends SimplePanel implements RequiresResize {
        int resized;
        public void onResize() { resized++; }
    }

    private GNavigatorWindow root, toolbar;
    private GAbstractWindow log, forms;
    private final Map<GAbstractWindow, View> views = new HashMap<>();
    private WindowsController controller;
    private ReactWindowLayout element;

    @Override
    protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        JsniTestSupport.installResizeObserver();

        root = window(new GNavigatorWindow(), "System.root", false, true);
        toolbar = window(new GNavigatorWindow(), "System.toolbar", true, true);
        log = window(new GAbstractWindow(), "System.log", false, false); // HIDE WINDOW System.log
        forms = window(new GAbstractWindow(), "System.forms", false, true);

        controller = new WindowsController() {
            @Override
            public Widget getWindowView(GAbstractWindow window) {
                return views.get(window);
            }
        };
        controller.initializeWindows(Arrays.asList(root, toolbar, log), forms, "AppLayout");
        element = (ReactWindowLayout) controller.rootElement;
    }

    @Override
    protected void gwtTearDown() {
        RootLayoutPanel.get().clear();
    }

    private <W extends GAbstractWindow> W window(W window, String canonicalName, boolean vertical, boolean visible) {
        window.canonicalName = canonicalName;
        window.caption = canonicalName.substring(canonicalName.indexOf('.') + 1);
        window.vertical = vertical;
        window.visible = visible;
        views.put(window, new View());
        return window;
    }

    // every window of the navigator is named in declared order and has an entry in byName - System.forms too, which
    // the standard layout puts in its centre on its own -, and HIDE WINDOW hides one from the start
    public void testEveryWindowIsAnEntryUnderItsName() {
        JavaScriptObject data = data(element);
        assertEquals("byName,windows", keys(data));
        JsArrayString windows = JsniTestSupport.field(data, "windows").cast();
        assertEquals("System.root,System.toolbar,System.log,System.forms", windows.join(","));
        JavaScriptObject entries = JsniTestSupport.field(data, "byName");
        assertEquals("System.forms,System.log,System.root,System.toolbar", keys(entries));

        JavaScriptObject entry = JsniTestSupport.field(entries, "System.toolbar");
        assertEquals("System.toolbar", string(entry, "name"));
        assertEquals("toolbar", string(entry, "caption"));
        assertFalse(bool(entry, "hidden"));
        assertTrue(bool(entry, "vertical"));

        assertTrue(bool(JsniTestSupport.field(entries, "System.log"), "hidden"));
        assertFalse(bool(JsniTestSupport.field(entries, "System.root"), "vertical"));
    }

    // a navigator window with nothing to draw is hidden as data, in a snapshot where only its entry is new; an update
    // that changes nothing leaves the very same snapshot
    public void testAWindowTheNavigatorHidesIsHidden() {
        JavaScriptObject before = data(element);
        Map<GAbstractWindow, Boolean> visibility = new HashMap<>();
        visibility.put(root, true);
        visibility.put(toolbar, false);
        controller.updateVisibility(visibility);

        JavaScriptObject after = data(element);
        assertNotSame(before, after);
        assertSame(JsniTestSupport.field(before, "windows"), JsniTestSupport.field(after, "windows"));
        JavaScriptObject beforeEntries = JsniTestSupport.field(before, "byName");
        JavaScriptObject afterEntries = JsniTestSupport.field(after, "byName");
        assertNotSame(beforeEntries, afterEntries);
        assertNotSame(JsniTestSupport.field(beforeEntries, "System.toolbar"), JsniTestSupport.field(afterEntries, "System.toolbar"));
        assertFalse(bool(JsniTestSupport.field(beforeEntries, "System.toolbar"), "hidden"));
        assertTrue(bool(JsniTestSupport.field(afterEntries, "System.toolbar"), "hidden"));
        assertSame(JsniTestSupport.field(beforeEntries, "System.root"), JsniTestSupport.field(afterEntries, "System.root"));
        assertSame(JsniTestSupport.field(beforeEntries, "System.log"), JsniTestSupport.field(afterEntries, "System.log"));
        assertSame(JsniTestSupport.field(beforeEntries, "System.forms"), JsniTestSupport.field(afterEntries, "System.forms"));

        controller.updateVisibility(visibility);
        assertSame(after, data(element));
    }

    // <Platform> hides a window's place through the entry the root's view returns, so that entry must come from byName
    public void testAHiddenWindowIsFoundInByName() {
        JavaScriptObject data = data(element);
        JavaScriptObject entries = JsniTestSupport.field(data, "byName");
        assertNotNull(entries);
        assertSame(JsniTestSupport.field(entries, "System.log"), entryOf(element, data, "System.log"));
        assertTrue(bool(entryOf(element, data, "System.log"), "hidden"));
        assertFalse(bool(entryOf(element, data, "System.toolbar"), "hidden"));

        Map<GAbstractWindow, Boolean> visibility = new HashMap<>();
        visibility.put(toolbar, false);
        controller.updateVisibility(visibility);
        assertTrue(bool(entryOf(element, data(element), "System.toolbar"), "hidden"));

        visibility.put(toolbar, true);
        controller.updateVisibility(visibility);
        assertFalse(bool(entryOf(element, data(element), "System.toolbar"), "hidden"));
    }

    // a window placed goes into its host and fills it, and taken away it goes back to wait in the park - in the
    // document and not shown, as a background tab is
    public void testAWindowIsPlacedInItsHostAndParkedAgain() {
        Element host = host();
        Element view = views.get(toolbar).getElement();
        mount(element, "System.toolbar", host);
        assertSame(host, view.getParentElement());
        assertTrue(view.hasClassName("fill-parent-flex"));

        unmount(element, "System.toolbar", host);
        Element park = view.getParentElement();
        assertNotSame(host, park);
        assertEquals(Style.Display.NONE.getCssName(), park.getStyle().getDisplay());
        assertSame(element.getView(), views.get(toolbar).getParent()); // still the panel's, so still attached and alive
        assertTrue(views.get(toolbar).isAttached());
    }

    // a name that is no window, and a second host for one, say so in the host
    public void testAMistakeIsSaidInTheHost() {
        Element wrong = host();
        mount(element, "System.nope", wrong);
        assertTrue(wrong.getInnerText(), wrong.getInnerText().contains("'System.nope' is not a window"));

        Element first = host(), second = host();
        mount(element, "System.forms", first);
        mount(element, "System.forms", second);
        assertSame(first, views.get(forms).getElement().getParentElement());
        assertTrue(second.getInnerText(), second.getInnerText().contains("already placed by another <Platform>"));
    }

    // the component resizes the place a window fills: the window's view is told, as a split's child is when its border
    // is dragged, so a grid, a pivot's chart, a map measure themselves again - deferred, as PlacedViews' other owners do
    public void testAWindowIsToldItsSizeChanged() {
        Element host = host();
        mount(element, "System.forms", host);
        View view = views.get(forms);

        delayTestFinish(5000);
        // counted from after the layout the setup scheduled (RootLayoutPanel lays out finally), which resizes the view too
        Scheduler.get().scheduleDeferred(() -> {
            int resized = view.resized;
            JsniTestSupport.resize(host);
            Scheduler.get().scheduleDeferred(() -> {
                assertEquals(resized + 1, view.resized);
                finishTest();
            });
        });
    }

    private static Element host() {
        Element host = Document.get().createDivElement();
        RootPanel.getBodyElement().appendChild(host);
        return host;
    }

    private static native JavaScriptObject data(ReactWindowLayout element) /*-{
        return element.@lsfusion.gwt.client.navigator.window.view.ReactWindowLayout::root.@lsfusion.gwt.client.base.view.ReactRoot::store.getSnapshot();
    }-*/;
    private static native void mount(ReactWindowLayout element, String name, Element host) /*-{
        element.@lsfusion.gwt.client.navigator.window.view.ReactWindowLayout::root.@lsfusion.gwt.client.base.view.ReactRoot::context.view.mount(name, host);
    }-*/;
    private static native void unmount(ReactWindowLayout element, String name, Element host) /*-{
        element.@lsfusion.gwt.client.navigator.window.view.ReactWindowLayout::root.@lsfusion.gwt.client.base.view.ReactRoot::context.view.unmount(name, host);
    }-*/;
    private static native JavaScriptObject entryOf(ReactWindowLayout element, JavaScriptObject data, String name) /*-{
        return element.@lsfusion.gwt.client.navigator.window.view.ReactWindowLayout::root.@lsfusion.gwt.client.base.view.ReactRoot::context.view.entryOf(data, name);
    }-*/;
    private static native String keys(JavaScriptObject data) /*-{ return Object.keys(data).sort().join(','); }-*/;
    private static native String string(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
    private static native boolean bool(JavaScriptObject object, String name) /*-{ return object[name]; }-*/;
}
