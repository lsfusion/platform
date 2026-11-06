package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.FormsWindowController;
import lsfusion.gwt.client.form.controller.GFormControllerTest;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import net.customware.gwt.dispatch.shared.Result;

// the forms window a React component draws (WINDOW ... CUSTOM): what it is given of the open forms. No React draws
// anything here, HtmlUnit has none: the root is never mounted, and the snapshots are read from the store a component's
// hooks read
public class ReactFormsViewTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private FormsController formsController;
    private FormsWindowController formsWindow;
    private ReactFormsView view;

    @Override
    protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        GFormControllerTest.installPageHelpers(); // the forms controller builds its toolbar's buttons
        JsniTestSupport.installPageSetup(); // ... and the edit modes their images

        formsController = new FormsController(new WindowsController() {
            public Widget getWindowView(GAbstractWindow window) { return null; }
        }) { // nothing is sent: no form here asks the server anything
            public <T extends Result> long syncDispatch(NavigatorRequestAction<T> action, RequestCountingAsyncCallback<T> callback) { return -1; }
            public long asyncDispatch(ExecuteNavigatorAction action, RequestCountingAsyncCallback<ServerResponseResult> callback) { return -1; }
            public GNavigatorActionDispatcher getDispatcher() { return null; }
        };
        GAbstractWindow window = new GAbstractWindow();
        window.canonicalName = "System.forms";
        window.react = true;
        window.custom = "NoSuchComponent";
        formsWindow = new FormsWindowController(formsController, window, true, null);
        view = (ReactFormsView) formsView(formsWindow);
    }

    // what changed is drawn again, and only that: an entry of a form that did not change is the very object pushed
    // before - when another form opens, and when another form's caption changes -, so a tab memoized on it is not
    // redrawn; the order of the forms is the same array while no form opens or closes, and a change of nothing at all
    // is the snapshot already there
    public void testAnEntryOfAFormThatDidNotChangeKeepsItsReference() {
        FormDockable first = open("First"), second = open("Second");
        JavaScriptObject two = data(view);
        open("Third");
        JavaScriptObject three = data(view);
        assertSame(entry(two, first), entry(three, first));
        assertSame(entry(two, second), entry(three, second));
        assertNotSame(JsniTestSupport.field(two, "open"), JsniTestSupport.field(three, "open"));

        second.requestedCaption = "Second, renamed";
        view.formsChanged();
        JavaScriptObject renamed = data(view);
        assertSame(entry(three, first), entry(renamed, first));
        assertNotSame(entry(three, second), entry(renamed, second));
        assertEquals("Second, renamed", caption(entry(renamed, second)));
        assertEquals("Second", caption(entry(three, second))); // a snapshot pushed is never changed
        assertSame(JsniTestSupport.field(three, "open"), JsniTestSupport.field(renamed, "open"));

        view.formsChanged();
        assertSame(renamed, data(view));
    }

    // a form the window is waiting for - an async open's placeholder, so with the caption its open asked for -, put in
    // as FormsWindowController puts it, and the window told
    private FormDockable open(String caption) {
        FormDockable dockable = new FormDockable(formsController, null, "ReactFormsViewTest." + caption, true, null, null);
        dockable.requestedCaption = caption;
        formsWindow.getForms().add(dockable);
        view.formAdded(dockable, null);
        view.formsChanged();
        return dockable;
    }

    private static JavaScriptObject entry(JavaScriptObject data, FormDockable dockable) {
        return JsniTestSupport.field(JsniTestSupport.field(data, "byName"), dockable.formName);
    }

    private static native FormsView formsView(FormsWindowController formsWindow) /*-{
        return formsWindow.@lsfusion.gwt.client.form.controller.FormsWindowController::formsView;
    }-*/;
    private static native JavaScriptObject data(ReactFormsView view) /*-{
        return view.@lsfusion.gwt.client.form.view.ReactFormsView::root.@lsfusion.gwt.client.base.view.ReactRoot::store.getSnapshot();
    }-*/;
    private static native String caption(JavaScriptObject entry) /*-{ return entry.caption; }-*/;
}
