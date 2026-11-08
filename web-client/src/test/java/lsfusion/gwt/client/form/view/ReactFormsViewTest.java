package lsfusion.gwt.client.form.view;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import lsfusion.gwt.client.base.jsni.JsniTestSupport;
import lsfusion.gwt.client.controller.remote.action.RequestCountingAsyncCallback;
import lsfusion.gwt.client.controller.remote.action.form.ServerResponseResult;
import lsfusion.gwt.client.controller.remote.action.navigator.ExecuteNavigatorAction;
import lsfusion.gwt.client.controller.remote.action.navigator.NavigatorRequestAction;
import lsfusion.gwt.client.form.controller.EditMode;
import lsfusion.gwt.client.form.controller.FormsController;
import lsfusion.gwt.client.form.controller.FormsWindowController;
import lsfusion.gwt.client.form.controller.GFormControllerTest;
import lsfusion.gwt.client.navigator.controller.dispatch.GNavigatorActionDispatcher;
import lsfusion.gwt.client.navigator.window.GAbstractWindow;
import lsfusion.gwt.client.navigator.window.view.WindowsController;
import net.customware.gwt.dispatch.shared.Result;

// the forms window a React component draws (WINDOW ... CUSTOM): what it is given of the open forms, and a form placed in
// a host. No React draws anything here, HtmlUnit has none: the root is never mounted, the snapshots are read from the
// store a component's hooks read, and the component's side is played through the context a <Platform> works with
public class ReactFormsViewTest extends GWTTestCase {
    @Override public String getModuleName() { return "lsfusion.gwt.main"; }

    private FormsController formsController;
    private FormsWindowController formsWindow;
    private ReactFormsView view;

    @Override
    protected void gwtSetUp() {
        JsniTestSupport.installMapDelete();
        JsniTestSupport.installResizeObserver(); // the hosts are watched for their size
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

    // a form placed goes into its host and fills it, and taken away it goes back to wait in the park - not shown, as a
    // background tab is, and still the window's
    public void testAFormIsPlacedInItsHostAndParkedAgain() {
        FormDockable form = open("First");
        Element content = form.getContentWidget().getElement(), host = host();
        mount(view, form.formName, host);
        assertSame(host, content.getParentElement());
        assertTrue(content.hasClassName("fill-parent-flex"));

        unmount(view, form.formName, host);
        Element park = content.getParentElement();
        assertNotSame(host, park);
        assertEquals(Style.Display.NONE.getCssName(), park.getStyle().getDisplay());
        assertSame(view.getView(), form.getContentWidget().getParent());
    }

    // a second place of a form says the first keeps it, and once the component takes the first away the eldest place
    // still waiting takes the form, the mistake it showed gone - not one the component took away meanwhile, nor one no
    // longer in the page (removed with the first, in one render) -, and the next one in line once that one goes
    public void testAPlaceRefusedAFormTakesItOnceTheFirstGoes() {
        FormDockable form = open("First");
        Element content = form.getContentWidget().getElement();
        Element first = host(), withdrawn = host(), removed = host(), next = host(), last = host();
        for (Element host : new Element[] {first, withdrawn, removed, next, last})
            mount(view, form.formName, host);
        assertSame(first, content.getParentElement());
        assertTrue(next.getInnerText(), next.getInnerText().contains("already placed by another <Platform>"));

        unmount(view, form.formName, withdrawn);
        removed.removeFromParent();
        unmount(view, form.formName, first);
        assertSame(next, content.getParentElement());
        assertFalse(next.getInnerText(), next.getInnerText().contains("already placed"));
        assertTrue(last.getInnerText(), last.getInnerText().contains("already placed by another <Platform>"));

        unmount(view, form.formName, next);
        assertSame(last, content.getParentElement());
    }

    // a refused call reaches the component as an Error with the reason alone, so it can catch and show that reason
    public void testControllerRefusesSelectingAFormThatIsNotOpen() {
        assertEquals("'nope' is not an open form, so it cannot be made the current one",
                refusalOf(controller(view), "select", "nope"));
    }

    public void testControllerRefusesClosingAFormThatIsNotOpen() {
        assertEquals("'nope' is not an open form, so it cannot be closed",
                refusalOf(controller(view), "close", "nope"));
    }

    public void testControllerRefusesClosingAFormBlockedByItsChild() {
        FormDockable form = open("First"), child = open("Child");
        form.setBlockingForm(child);
        view.formsChanged();
        assertEquals("form '" + form.formName + "' is blocked by a form opened from it and cannot be closed"
                        + "; the entry's `blocked` says so before it is tried",
                refusalOf(controller(view), "close", form.formName));
        assertTrue(formsWindow.getForms().contains(form));
    }

    public void testControllerRefusesAnUnknownEditMode() {
        assertEquals("'nope' is not an edit mode; `editModes` in the projection has the ones there are",
                refusalOf(controller(view), "setEditMode", "nope"));
    }

    public void testControllerChoosesAKnownEditMode() {
        int previous = FormsController.getEditModeIndex();
        EditMode mode = previous == EditMode.DIALOG.getIndex() ? EditMode.DEFAULT : EditMode.DIALOG;
        try {
            JsniTestSupport.call(controller(view), "setEditMode", mode.name());
            assertEquals(mode.getIndex(), FormsController.getEditModeIndex());
        } finally {
            formsController.setEditMode(EditMode.getMode(previous));
        }
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

    private static Element host() {
        Element host = Document.get().createDivElement();
        RootPanel.getBodyElement().appendChild(host);
        return host;
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
    private static native JavaScriptObject controller(ReactFormsView view) /*-{
        return view.@lsfusion.gwt.client.form.view.ReactFormsView::root.@lsfusion.gwt.client.base.view.ReactRoot::context.controller;
    }-*/;
    private static native String refusalOf(JavaScriptObject controller, String method, String name) /*-{
        try {
            controller[method](name);
        } catch (error) {
            if (error instanceof Error)
                return error.message;
            throw error;
        }
        return null;
    }-*/;
    private static native void mount(ReactFormsView view, String name, Element host) /*-{
        view.@lsfusion.gwt.client.form.view.ReactFormsView::root.@lsfusion.gwt.client.base.view.ReactRoot::context.view.mount(name, host);
    }-*/;
    private static native void unmount(ReactFormsView view, String name, Element host) /*-{
        view.@lsfusion.gwt.client.form.view.ReactFormsView::root.@lsfusion.gwt.client.base.view.ReactRoot::context.view.unmount(name, host);
    }-*/;
    private static native String caption(JavaScriptObject entry) /*-{ return entry.caption; }-*/;
}
